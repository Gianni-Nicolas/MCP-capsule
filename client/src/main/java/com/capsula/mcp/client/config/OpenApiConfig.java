package com.capsula.mcp.client.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata global de la API para Swagger UI / OpenAPI.
 *
 * <p>Swagger UI disponible en {@code /swagger-ui.html} y el JSON OpenAPI en
 * {@code /v3/api-docs} (puerto 8081).
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI capsulaOpenAPI() {
		return new OpenAPI().info(new Info()
				.title("Capsula MCP · API NL→SQL")
				.description("Traduce lenguaje natural a SQL usando un LLM con tools MCP de metadata JDBC.")
				.version("0.1.0"));
	}
}