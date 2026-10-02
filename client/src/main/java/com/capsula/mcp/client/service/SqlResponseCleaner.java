package com.capsula.mcp.client.service;

import org.springframework.stereotype.Component;

/**
 * Normaliza la salida cruda del LLM para dejar una sentencia SQL "limpia":
 * quita fences markdown (```sql ... ```), backticks y espacios sobrantes.
 *
 * Se extrae como componente propio porque la limpieza la comparten varios flujos
 * (generar SQL y, a futuro, ejecutar la SQL generada), y para poder testearla aislada.
 */
@Component
public class SqlResponseCleaner {

	public String clean(String raw) {
		if (raw == null) {
			return "";
		}
		String sql = raw.strip();
		if (sql.startsWith("```")) {
			// remueve la primera linea de apertura (```sql o ```)
			int firstNewline = sql.indexOf('\n');
			if (firstNewline >= 0) {
				sql = sql.substring(firstNewline + 1);
			}
			// remueve el cierre ```
			int closing = sql.lastIndexOf("```");
			if (closing >= 0) {
				sql = sql.substring(0, closing);
			}
		}
		sql = sql.strip();

		// Colapsa saltos de linea y espacios multiples en una sola linea, para que
		// al copiar y pegar la query (p. ej. en la consola H2) se ejecute directamente
		// sin romper por los '\n'.
		sql = sql.replaceAll("\\s+", " ").strip();

		return sql;
	}
}