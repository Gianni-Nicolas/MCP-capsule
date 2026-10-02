package com.capsula.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del endpoint de generacion de SQL.
 *
 * @param sql query SQL generada por el LLM.
 */
@Schema(description = "Respuesta del endpoint de generacion de SQL.")
public record QueryResponse(

		@Schema(
				description = "Query SQL generada por el LLM.",
				example = "SELECT c.first_name, c.last_name FROM customer c JOIN bank_account a ON a.customer_id = c.id")
		String sql
) {
}