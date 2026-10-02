package com.capsula.mcp.server.metadata.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios de las propiedades de configuracion del servicio de metadata.
 */
class MetadataPropertiesTest {

	@Test
	void shouldHaveSensibleDefaults() {
		MetadataProperties props = new MetadataProperties();

		assertEquals(200, props.getMaxResults(), "El default de maxResults deberia ser 200");
		assertTrue(props.getExcludedSchemas().contains("INFORMATION_SCHEMA"),
				"Por default deberia excluir INFORMATION_SCHEMA");
		assertTrue(props.getExcludedSchemas().contains("PG_CATALOG"),
				"Por default deberia excluir PG_CATALOG");
	}

	@Test
	void shouldAllowOverridingMaxResults() {
		MetadataProperties props = new MetadataProperties();

		props.setMaxResults(50);

		assertEquals(50, props.getMaxResults());
	}

	@Test
	void shouldAllowOverridingExcludedSchemas() {
		MetadataProperties props = new MetadataProperties();

		props.setExcludedSchemas(Set.of("CUSTOM_SYS"));

		assertEquals(Set.of("CUSTOM_SYS"), props.getExcludedSchemas());
	}

}