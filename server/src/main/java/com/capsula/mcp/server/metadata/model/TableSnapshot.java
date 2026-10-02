package com.capsula.mcp.server.metadata.model;

import java.util.List;

/**
 * Vista agregada de una tabla: sirve para que un cliente LLM obtenga
 * columnas + PK + FKs + indices en una sola llamada MCP y reduzca round-trips.
 */
public record TableSnapshot(
		TableInfo table,
		List<ColumnInfo> columns,
		PrimaryKeyInfo primaryKey,
		List<ForeignKeyInfo> foreignKeys,
		List<IndexInfo> indexes
) {
}