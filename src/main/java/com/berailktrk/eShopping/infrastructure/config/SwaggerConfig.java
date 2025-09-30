package com.berailktrk.eShopping.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger/OpenAPI konfigürasyonu
 * JWT Bearer token authentication için
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token'ınızı buraya girin (Bearer prefix'siz)")
                        )
                )
                .info(new Info()
                        .title("eShopping API")
                        .description("E-Ticaret platformu - Stok takibi ve sipariş yönetim sistemi API dokümantasyonu")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("eShopping Team")
                                .email("support@eshopping.com")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                        )
                );
    }
}
