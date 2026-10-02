package com.capsula.mcp.server.metadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Propiedades para controlar el comportamiento del servicio de metadata.
 * Ejemplo en application.properties:
 *   mcp.metadata.max-results=200
 *   mcp.metadata.excluded-schemas=INFORMATION_SCHEMA,SYS
 */
@ConfigurationProperties(prefix = "mcp.metadata")
public class MetadataProperties {

	/** Tamano maximo por respuesta para evitar explotar el contexto del LLM. */
	private int maxResults = 200;

	/** Schemas del sistema que se ocultan por default. Comparados en upper-case. */
	private Set<String> excludedSchemas = defaultExcludedSchemas();

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public Set<String> getExcludedSchemas() {
		return excludedSchemas;
	}

	public void setExcludedSchemas(Set<String> excludedSchemas) {
		this.excludedSchemas = excludedSchemas;
	}

	private static Set<String> defaultExcludedSchemas() {
		Set<String> set = new LinkedHashSet<>();
		set.add("INFORMATION_SCHEMA");
		set.add("SYS");
		set.add("SYSTEM");
		set.add("SYSAUX");
		set.add("PG_CATALOG");
		set.add("PG_TOAST");
		set.add("MYSQL");
		set.add("PERFORMANCE_SCHEMA");
		return set;
	}

}

