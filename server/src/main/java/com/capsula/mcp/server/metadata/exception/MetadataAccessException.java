package com.capsula.mcp.server.metadata.exception;

/**
 * Excepcion no chequeada para errores al acceder a la metadata JDBC.
 * Encapsula SQLState y motor para que sea util al LLM y al operador.
 */
public class MetadataAccessException extends RuntimeException {

	private final String sqlState;

	public MetadataAccessException(String message, String sqlState, Throwable cause) {
		super(message, cause);
		this.sqlState = sqlState;
	}

	public MetadataAccessException(String message, Throwable cause) {
		this(message, null, cause);
	}

	public String getSqlState() {
		return sqlState;
	}

}

