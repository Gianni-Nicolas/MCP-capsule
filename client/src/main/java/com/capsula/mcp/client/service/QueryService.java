package com.capsula.mcp.client.service;
import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Orquesta la generacion de SQL a partir de lenguaje natural:
 * invoca al {@link ChatClient} (que usa las tools MCP de metadata)
 * y limpia la salida.
 */
@Service
public class QueryService {

	private static final Logger log = LoggerFactory.getLogger(QueryService.class);

	private final ChatClient chatClient;
	private final SqlResponseCleaner sqlCleaner;

	public QueryService(ChatClient sqlChatClient, SqlResponseCleaner sqlCleaner) {
		this.chatClient = sqlChatClient;
		this.sqlCleaner = sqlCleaner;
	}

	public QueryResponse generate(QueryRequest request) {
		log.info("Generando SQL para pedido: '{}'", request.request());

		String raw = chatClient.prompt()
				.user(request.request())
				.call()
				.content();

		String sql = sqlCleaner.clean(raw);
		return new QueryResponse(sql);
	}
}