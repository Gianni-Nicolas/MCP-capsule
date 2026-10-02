package com.capsula.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de generacion de SQL en lenguaje natural.
 *
 * @param request texto en lenguaje natural con lo que se quiere consultar.
 */
@Schema(description = "Pedido de generacion de SQL en lenguaje natural.")
public record QueryRequest(

		@Schema(
				description = "Texto en lenguaje natural con lo que se quiere consultar.",
				example = "clientes con al menos una tarjeta",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "El campo 'request' es obligatorio.")
		String request
) {
}