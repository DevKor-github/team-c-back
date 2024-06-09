package devkor.com.teamcback.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_TOKEN_PREFIX = "Bearer";

    @Bean
    public OpenAPI openAPI(){
        Info apiInfo = new Info()
            .title("고대로 API 명세")
            .description("고대로 백엔드 api 명세서입니다.")
            .version("1.0.0");


        String securityJwtName = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);
        Components components = new Components()
            .addSecuritySchemes(securityJwtName, new SecurityScheme()
                .name(securityJwtName)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .scheme(BEARER_TOKEN_PREFIX)
                .bearerFormat(securityJwtName)
                .name("AccessToken"));


        return new OpenAPI()
            .info(apiInfo)
            .addSecurityItem(securityRequirement)
            .components(components);
    }

}
