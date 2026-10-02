package com.capsula.mcp.server.metadata.model;

import java.util.List;

/**
 * Indice de una tabla. Las columnas vienen ordenadas por ORDINAL_POSITION.
 */
public record IndexInfo(
		String schema,
		String table,
		String indexName,
		boolean unique,
		String type,
		List<String> columns
) {
}