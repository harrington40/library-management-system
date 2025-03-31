
package com.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Library Management API")
                .description("API for managing books, members and loans")
                .version("1.0"));
    }
}
