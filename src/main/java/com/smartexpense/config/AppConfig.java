package com.smartexpense.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Expense Categorizer API")
                        .description("""
                            An AI-powered expense management API that auto-categorizes expenses using:
                            1. **Keyword Matching** — fast, rule-based matching (Swiggy→Food, Uber→Transport)
                            2. **Gemini LLM Fallback** — intelligent categorization for unknown descriptions
                            3. **Anomaly Detection** — flags expenses > 3× the 3-month category average
                            4. **Async Excel Upload** — Kafka-powered bulk ingestion of expense sheets
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Smart Expense Team")
                                .email("support@smartexpense.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token (obtained from /api/auth/login)")));
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
