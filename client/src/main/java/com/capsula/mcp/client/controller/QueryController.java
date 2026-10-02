package com.capsula.mcp.client.controller;

import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import com.capsula.mcp.client.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de chat NL->SQL.
 *
 * POST /api/query
 * Body: { "request": "traeme los empleados del departamento de ventas", "includeMetadata": false }
 * Respuesta: { "sql": "SELECT ..." } (+ "metadataUsed" si includeMetadata=true)
 */
@RestController
@RequestMapping("/api/query")
@Tag(name = "Query NL→SQL", description = "Genera SQL a partir de lenguaje natural usando tools MCP de metadata.")
public class QueryController {

	private final QueryService queryService;

	public QueryController(QueryService queryService) {
		this.queryService = queryService;
	}

	@PostMapping
	@Operation(
			summary = "Genera una sentencia SQL a partir de lenguaje natural",
			description = """
					Recibe un pedido en lenguaje natural y devuelve UNA sentencia SQL valida.
					El LLM inspecciona el esquema real via tools MCP antes de construir la query.
					Si 'includeMetadata' es true, la respuesta incluye la traza de tools MCP usadas.
					""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SQL generada correctamente",
					content = @Content(schema = @Schema(implementation = QueryResponse.class))),
			@ApiResponse(responseCode = "400", description = "Request invalido (campo 'request' vacio)",
					content = @Content)
	})
	public QueryResponse query(@Valid @RequestBody QueryRequest request) {
		return queryService.generate(request);
	}
}