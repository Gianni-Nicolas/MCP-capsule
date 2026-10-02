package com.capsula.mcp.client;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(properties = {
        // Excluye la autoconfig del cliente MCP para que no intente conectarse por SSE
        // al servidor MCP real (no disponible en los tests) ni registre su bean
        // 'mcpToolCallbacks'. Nosotros proveemos uno vacio mas abajo.
        "spring.autoconfigure.exclude="
                + "org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration,"
                + "org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration"
})
class ClientApplicationTests {

    /**
     * Provider de tools MCP vacio, para que el {@code ChatClientConfig} pueda construir
     * el ChatClient sin depender del servidor MCP real.
     */
    @TestConfiguration
    static class NoMcpTestConfig {

        @Bean
        ToolCallbackProvider mcpToolCallbacks() {
            return () -> new ToolCallback[0];
        }
    }

    @Test
    void contextLoads() {
    }

}