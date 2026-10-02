package com.capsula.mcp.client.config;

import com.capsula.mcp.client.tool.RecordingToolCallback;
import com.capsula.mcp.client.tool.ToolInvocationRecorder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configura el {@link ChatClient} usado para traducir lenguaje natural a SQL.
 * Registra las tools MCP del server (metadata JDBC) envueltas en
 * {@link RecordingToolCallback} para poder trazar cuales uso el LLM.
 */
@Configuration
public class ChatClientConfig {

	private static final String SYSTEM_PROMPT = """
			Sos un asistente experto en SQL. Tu unica tarea es traducir un pedido en
			lenguaje natural a UNA sentencia SQL valida para la base de datos configurada.

			REGLAS OBLIGATORIAS:
			1. Antes de escribir cualquier SQL, DEBES inspeccionar el esquema real usando las
			   tools de metadata disponibles (db_list_schemas, db_list_tables,
			   db_get_table_columns, db_get_foreign_keys, db_get_indexes, db_get_constraints).
			   Nunca asumas nombres de tablas o columnas.
			2. Para entender una tabla completa combina las tools atomicas: db_get_table_columns
			   (columnas), db_get_constraints (PK/UNIQUE), db_get_foreign_keys (FKs) y
			   db_get_indexes (indices), antes de armar joins o filtros.
			3. Respeta EXACTAMENTE los nombres de schemas, tablas y columnas tal como los
			   devuelven las tools (incluido el case).
			4. Construi joins usando las foreign keys reales reportadas por las tools.
			5. No inventes tablas, columnas ni relaciones que no existan en la metadata.
			6. Si el pedido es ambiguo, elegi la interpretacion mas razonable segun el esquema.

			FORMATO DE SALIDA:
			- Responde UNICAMENTE con la sentencia SQL final, sin explicaciones, sin comentarios
			  y sin formato markdown (no uses ``` ni backticks).
			""";

	@Bean
	ChatClient sqlChatClient(ChatClient.Builder builder,
							 ToolCallbackProvider mcpToolCallbacks,
							 ToolInvocationRecorder recorder) {
		List<ToolCallback> recordingCallbacks = Arrays.stream(mcpToolCallbacks.getToolCallbacks())
				.map(callback -> (ToolCallback) new RecordingToolCallback(callback, recorder))
				.toList();

		return builder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(recordingCallbacks)
				.build();
	}
}
