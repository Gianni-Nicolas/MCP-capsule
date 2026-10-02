package com.capsula.mcp.server.metadata.service;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Normaliza identificadores (schema/tabla/columna) segun como el motor
 * los almacena internamente. Necesario para consultar {@link DatabaseMetaData}
 * de forma exact-match en motores como H2/Oracle (upper) y Postgres (lower).
 *
 * Package-private: detalle interno del feature metadata.
 */
final class IdentifierNormalizer {

	private final boolean storesUpperCase;
	private final boolean storesLowerCase;

	private IdentifierNormalizer(boolean storesUpperCase, boolean storesLowerCase) {
		this.storesUpperCase = storesUpperCase;
		this.storesLowerCase = storesLowerCase;
	}

	static IdentifierNormalizer from(DatabaseMetaData metaData) throws SQLException {
		return new IdentifierNormalizer(
				metaData.storesUpperCaseIdentifiers(),
				metaData.storesLowerCaseIdentifiers()
		);
	}

	/**
	 * Devuelve el identificador tal como el motor lo almacena.
	 * Si el valor viene null o vacio, retorna null (usado como wildcard por JDBC).
	 */
	String normalize(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			return null;
		}
		if (storesUpperCase) {
			return identifier.toUpperCase();
		}
		if (storesLowerCase) {
			return identifier.toLowerCase();
		}
		return identifier;
	}

	/**
	 * Para patrones (LIKE) con wildcards. Si es null o vacio devuelve "%".
	 */
	String normalizePattern(String pattern) {
		if (pattern == null || pattern.isBlank()) {
			return "%";
		}
		return normalize(pattern);
	}

}

