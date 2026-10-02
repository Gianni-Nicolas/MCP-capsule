package com.capsula.mcp.server.metadata.model;

import java.util.List;

/**
 * Restriccion (PK, UNIQUE, FK o CHECK) sobre una tabla.
 * type es uno de: PRIMARY_KEY, UNIQUE, FOREIGN_KEY, CHECK.
 * definition solo aplica a CHECK y depende del soporte del motor.
 */
public record ConstraintInfo(
		String schema,
		String table,
		String name,
		String type,
		List<String> columns,
		String definition
) {
}

