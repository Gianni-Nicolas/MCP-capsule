package com.capsula.mcp.server.metadata.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests unitarios de la excepcion de acceso a metadata.
 */
class MetadataAccessExceptionTest {

	@Test
	void shouldRetainMessageSqlStateAndCause() {
		Throwable cause = new RuntimeException("root");

		MetadataAccessException ex =
				new MetadataAccessException("fallo", "42S02", cause);

		assertEquals("fallo", ex.getMessage());
		assertEquals("42S02", ex.getSqlState());
		assertSame(cause, ex.getCause());
	}

	@Test
	void shouldAllowNullSqlStateViaShortConstructor() {
		Throwable cause = new RuntimeException("root");

		MetadataAccessException ex =
				new MetadataAccessException("fallo", cause);

		assertEquals("fallo", ex.getMessage());
		assertNull(ex.getSqlState(), "El constructor corto deja sqlState en null");
		assertSame(cause, ex.getCause());
	}

}

