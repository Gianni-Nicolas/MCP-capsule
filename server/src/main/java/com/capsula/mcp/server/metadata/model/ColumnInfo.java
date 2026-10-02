package com.capsula.mcp.server.metadata.model;

public record ColumnInfo(
		String schema,
		String table,
		String name,
		int jdbcType,
		String typeName,
		boolean nullable,
		String defaultValue,
		int size,
		int scale,
		int ordinal
) {
}

