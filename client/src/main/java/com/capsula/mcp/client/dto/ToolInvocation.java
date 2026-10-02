package com.capsula.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Registro de una invocacion a una tool MCP durante la generacion de la query.
 *
 * @param tool      nombre de la tool MCP invocada (ej: db_get_table_snapshot).
 * @param arguments argumentos con los que se invoco, como JSON real (Map/List, no string escapado).
 * @param result    resultado devuelto por la tool, como JSON real y desanidado
 *                  (se extrae el contenido "text" del envoltorio MCP y se parsea).
 */
@Schema(description = "Registro de una invocacion a una tool MCP durante la generacion de la query.")
public record ToolInvocation(

		@Schema(description = "Nombre de la tool MCP invocada.", example = "db_get_table_snapshot")
		String tool,

		@Schema(description = "Argumentos con los que se invoco (JSON real, no string escapado).")
		Object arguments,

		@Schema(description = "Resultado devuelto por la tool (JSON real y desanidado del envoltorio MCP).")
		Object result
) {
}