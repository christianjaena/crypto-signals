package com.christianjaena.crypto_signals.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cryptoSignalsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Signal Generator API")
                        .description("A comprehensive cryptocurrency trading signal generator using technical analysis across multiple timeframes. " +
                                "This service implements a 6-step signal generation process based on EMA, RSI, and StochRSI indicators.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Christian Jaena")
                                .email("christian@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://api.cryptosignals.com")
                                .description("Production server")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Crypto Signal Generator")
                                .description("API endpoints for generating cryptocurrency trading signals")
                ));
    }
}
