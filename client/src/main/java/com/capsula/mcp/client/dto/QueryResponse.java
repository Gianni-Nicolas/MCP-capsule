package com.capsula.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta del endpoint de generacion de SQL.
 *
 * @param sql          query SQL generada por el LLM.
 * @param metadataUsed traza de tools MCP usadas (solo si includeMetadata=true; si no, null y se omite del JSON).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta del endpoint de generacion de SQL.")
public record QueryResponse(

		@Schema(
				description = "Query SQL generada por el LLM.",
				example = "SELECT * FROM empleados e JOIN departamentos d ON e.dept_id = d.id WHERE d.nombre = 'ventas'")
		String sql,

		@Schema(
				description = "Traza de tools MCP usadas (solo si includeMetadata=true; si no, se omite del JSON).")
		List<ToolInvocation> metadataUsed
) {
}