package com.healthcare.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI configuration. The /v3/api-docs and /swagger-ui.html
 * endpoints are only meaningful in local / dev profiles; they are exposed
 * for now and will be locked down in the cloud phase.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        final String bearerSchemeName = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("v1")
                        .description("Healthcare Platform — Auth / Identity Service")
                        .license(new License().name("Internal").url("https://example.internal")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components().addSecuritySchemes(bearerSchemeName,
                        new SecurityScheme()
                                .name(bearerSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
