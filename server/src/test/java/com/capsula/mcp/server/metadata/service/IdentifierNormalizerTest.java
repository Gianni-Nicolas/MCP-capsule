package com.capsula.mcp.server.metadata.service;

import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Mismo package que IdentifierNormalizer para poder testear una clase package-private.
class IdentifierNormalizerTest {

	@Test
	void shouldUppercaseWhenEngineStoresUpper() throws SQLException {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		when(md.storesUpperCaseIdentifiers()).thenReturn(true);
		when(md.storesLowerCaseIdentifiers()).thenReturn(false);

		IdentifierNormalizer n = IdentifierNormalizer.from(md);
		assertEquals("EMPLOYEE", n.normalize("employee"));
		assertEquals("%", n.normalizePattern(null));
	}

	@Test
	void shouldLowercaseWhenEngineStoresLower() throws SQLException {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		when(md.storesUpperCaseIdentifiers()).thenReturn(false);
		when(md.storesLowerCaseIdentifiers()).thenReturn(true);

		IdentifierNormalizer n = IdentifierNormalizer.from(md);
		assertEquals("employee", n.normalize("EMPLOYEE"));
	}

	@Test
	void shouldKeepMixedCaseOtherwise() throws SQLException {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		when(md.storesUpperCaseIdentifiers()).thenReturn(false);
		when(md.storesLowerCaseIdentifiers()).thenReturn(false);

		IdentifierNormalizer n = IdentifierNormalizer.from(md);
		assertEquals("Employee", n.normalize("Employee"));
	}

	@Test
	void shouldReturnNullForEmptyInput() throws SQLException {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		when(md.storesUpperCaseIdentifiers()).thenReturn(true);
		IdentifierNormalizer n = IdentifierNormalizer.from(md);
		assertNull(n.normalize(""));
		assertNull(n.normalize(null));
	}

	@Test
	void shouldNormalizeNonBlankPattern() throws SQLException {
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		when(md.storesUpperCaseIdentifiers()).thenReturn(true);
		when(md.storesLowerCaseIdentifiers()).thenReturn(false);

		IdentifierNormalizer n = IdentifierNormalizer.from(md);
		assertEquals("EMP%", n.normalizePattern("emp%"));
	}

}