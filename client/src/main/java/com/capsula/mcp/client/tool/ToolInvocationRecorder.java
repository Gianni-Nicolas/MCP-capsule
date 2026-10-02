package com.capsula.mcp.client.tool;

import com.capsula.mcp.client.dto.ToolInvocation;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Acumula, por request HTTP, las invocaciones a tools MCP que hace el LLM.
 * Es request-scoped: cada pedido /api/query tiene su propia traza aislada.
 * Se inyecta como proxy en beans singleton (ChatClient / RecordingToolCallback).
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ToolInvocationRecorder {

	private final List<ToolInvocation> invocations = new ArrayList<>();

	public void record(ToolInvocation invocation) {
		invocations.add(invocation);
	}

	/** Copia inmutable de la traza acumulada en el request actual. */
	public List<ToolInvocation> snapshot() {
		return List.copyOf(invocations);
	}

	public void clear() {
		invocations.clear();
	}
}

