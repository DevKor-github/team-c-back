package devkor.com.teamcback.domain.notification.persistence;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.transaction.PlatformTransactionManager;

/** Enabled only at the coordinated cutover. No runtime schema creation on Neon. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "push.storage", havingValue = "neon")
public class NeonPushStoreConfiguration {
    @Bean
    org.springframework.beans.factory.InitializingBean validatePushAudience(Environment env) {
        return () -> {
            if (!List.of("INTERNAL_TEST", "LIVE").contains(env.getProperty("push.audience", "INTERNAL_TEST")))
                throw new IllegalStateException("push.audience must be INTERNAL_TEST or LIVE");
        };
    }

    static final String ROOT = "devkor.com.teamcback";

    @Bean @Primary @ConfigurationProperties("spring.datasource")
    DataSourceProperties serviceDataSourceProperties() { return new DataSourceProperties(); }

    @Bean(name = "dataSource") @Primary @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource serviceDataSource(@Qualifier("serviceDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean @ConfigurationProperties("push.datasource")
    DataSourceProperties pushDataSourceProperties() { return new DataSourceProperties(); }

    @Bean @ConfigurationProperties("push.datasource.hikari")
    HikariDataSource pushDataSource(@Qualifier("pushDataSourceProperties") DataSourceProperties properties) {
        if (properties.getUrl() == null || properties.getUrl().isBlank())
            throw new IllegalStateException("PUSH_DATABASE_URL is required for Neon storage");
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    static PersistenceManagedTypes managedTypes(ResourceLoader loader, boolean push) {
        List<String> names = new PersistenceManagedTypesScanner(loader).scan(ROOT).getManagedClassNames().stream()
                .filter(name -> {
                    try { return Class.forName(name).isAnnotationPresent(PushStore.class) == push; }
                    catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
                }).toList();
        return PersistenceManagedTypes.of(names, List.of());
    }

    @Bean(name = "entityManagerFactory") @Primary
    LocalContainerEntityManagerFactoryBean serviceEntityManagerFactory(EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource source, ResourceLoader loader, JpaProperties jpa, Environment env) {
        Map<String,Object> props = new HashMap<>(jpa.getProperties());
        // Cutover must never silently modify the existing service schema.
        props.put("hibernate.hbm2ddl.auto", env.getProperty("push.service-ddl-auto", "validate"));
        return builder.dataSource(source).managedTypes(managedTypes(loader, false))
                .properties(props).persistenceUnit("service").build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean pushEntityManagerFactory(EntityManagerFactoryBuilder builder,
            @Qualifier("pushDataSource") DataSource source, ResourceLoader loader, Environment env) {
        Map<String,Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", env.getProperty("push.ddl-auto", "validate"));
        props.put("hibernate.dialect", env.getProperty("push.dialect", "org.hibernate.dialect.PostgreSQLDialect"));
        props.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        return builder.dataSource(source).managedTypes(managedTypes(loader, true))
                .properties(props).persistenceUnit("push").build();
    }

    @Bean(name = "transactionManager") @Primary
    PlatformTransactionManager serviceTransactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
    @Bean
    PlatformTransactionManager pushTransactionManager(@Qualifier("pushEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackages = ROOT, excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = PushStore.class))
    static class ServiceRepositories {}

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackages = ROOT, includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = PushStore.class),
            entityManagerFactoryRef = "pushEntityManagerFactory", transactionManagerRef = "pushTransactionManager")
    static class PushRepositories {}
}
