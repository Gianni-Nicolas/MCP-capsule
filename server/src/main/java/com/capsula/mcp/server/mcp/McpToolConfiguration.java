package com.capsula.mcp.server.mcp;

import com.capsula.mcp.server.metadata.tool.DatabaseMetadataTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agregador de tools MCP: junta las tools de cada feature y las registra
 * como un unico {@link ToolCallbackProvider} para el servidor MCP.
 * Cuando aparezcan nuevos features, sumar sus tools aca.
 */
@Configuration
public class McpToolConfiguration {

    @Bean
    ToolCallbackProvider toolCallbackProvider(DatabaseMetadataTools databaseMetadataTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(databaseMetadataTools)
                .build();
    }

}
