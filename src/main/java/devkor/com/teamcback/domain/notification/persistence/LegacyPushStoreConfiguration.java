package devkor.com.teamcback.domain.notification.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "push.storage", havingValue = "mysql", matchIfMissing = true)
public class LegacyPushStoreConfiguration {
    @Bean
    static BeanFactoryPostProcessor pushTransactionAlias() {
        return factory -> {
            factory.registerAlias("transactionManager", "pushTransactionManager");
            factory.registerAlias("entityManagerFactory", "pushEntityManagerFactory");
        };
    }
}
