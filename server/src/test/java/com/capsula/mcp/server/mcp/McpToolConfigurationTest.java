package com.capsula.mcp.server.mcp;

import com.capsula.mcp.server.metadata.service.MetadataService;
import com.capsula.mcp.server.metadata.tool.DatabaseMetadataTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifica que el agregador MCP registre todas las tools de metadata como
 * callbacks, exponiendo los nombres esperados al servidor.
 */
class McpToolConfigurationTest {

	@Test
	void registersAllDatabaseMetadataTools() {
		DatabaseMetadataTools tools = new DatabaseMetadataTools(mock(MetadataService.class));

		ToolCallbackProvider provider = new McpToolConfiguration().toolCallbackProvider(tools);
		ToolCallback[] callbacks = provider.getToolCallbacks();

		Set<String> names = Arrays.stream(callbacks)
				.map(cb -> cb.getToolDefinition().name())
				.collect(Collectors.toSet());

		assertEquals(6, callbacks.length, "Se esperan 6 tools registradas");
		assertTrue(names.containsAll(Set.of(
				"db_list_schemas",
				"db_list_tables",
				"db_get_table_columns",
				"db_get_foreign_keys",
				"db_get_indexes",
				"db_get_constraints"
		)), "Deben registrarse todas las tools de metadata");
	}
}

