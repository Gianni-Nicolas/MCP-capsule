package com.capsula.mcp.server.metadata.model;

/**
 * Foreign key (saliente o entrante) entre dos tablas.
 * Las reglas se exponen como strings legibles: CASCADE, RESTRICT, SET_NULL, NO_ACTION, SET_DEFAULT.
 */
public record ForeignKeyInfo(
		String fkName,
		String pkName,
		String fkSchema,
		String fkTable,
		String fkColumn,
		String pkSchema,
		String pkTable,
		String pkColumn,
		int keySeq,
		String updateRule,
		String deleteRule
) {
}

