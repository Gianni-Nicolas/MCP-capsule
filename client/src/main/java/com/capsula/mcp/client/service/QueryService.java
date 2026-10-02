package com.capsula.mcp.client.service;

import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import com.capsula.mcp.client.dto.ToolInvocation;
import com.capsula.mcp.client.tool.ToolInvocationRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta la generacion de SQL a partir de lenguaje natural:
 * invoca al {@link ChatClient} (que usa las tools MCP de metadata),
 * limpia la salida y adjunta la traza de tools si se solicito.
 */
@Service
public class QueryService {

	private static final Logger log = LoggerFactory.getLogger(QueryService.class);

	private final ChatClient chatClient;
	private final ToolInvocationRecorder recorder;
	private final SqlResponseCleaner sqlCleaner;

	public QueryService(ChatClient sqlChatClient, ToolInvocationRecorder recorder, SqlResponseCleaner sqlCleaner) {
		this.chatClient = sqlChatClient;
		this.recorder = recorder;
		this.sqlCleaner = sqlCleaner;
	}

	public QueryResponse generate(QueryRequest request) {
		recorder.clear();
		log.info("Generando SQL para pedido: '{}' (includeMetadata={})",
				request.request(), request.includeMetadata());

		String raw = chatClient.prompt()
				.user(request.request())
				.call()
				.content();

		String sql = sqlCleaner.clean(raw);
		log.info("SQL generada con {} invocaciones a tools MCP", recorder.snapshot().size());

		List<ToolInvocation> metadataUsed = request.includeMetadata() ? recorder.snapshot() : null;
		return new QueryResponse(sql, metadataUsed);
	}
}
