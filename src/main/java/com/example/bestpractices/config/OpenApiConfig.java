package com.example.bestpractices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Best practices demonstrated:
 * - Provide descriptive API metadata (title, version, contact) so consumers know who to call
 * - Swagger UI is available at /swagger-ui.html; OpenAPI JSON at /v3/api-docs
 * - @Operation and @Tag on controllers are picked up automatically by springdoc
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Best Practices API")
                        .description("Reference implementation of Spring Boot best practices")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("ChrisvDalen")
                                .url("https://github.com/ChrisvDalen/spring-boot-best-practices"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
