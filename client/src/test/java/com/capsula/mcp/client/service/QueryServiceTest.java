package com.capsula.mcp.client.service;

import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QueryService")
class QueryServiceTest {

	private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

	private final QueryService service = new QueryService(chatClient, new SqlResponseCleaner());

	private void stubChatClient(String raw) {
		when(chatClient.prompt().user(anyString()).call().content()).thenReturn(raw);
	}

	@Test
	@DisplayName("given a request when generate is called then it returns the cleaned SQL")
	void generatesCleanedSql() {
		stubChatClient("```sql\nSELECT 1\nFROM t\n```");

		QueryResponse response = service.generate(new QueryRequest("dame algo"));

		assertThat(response.sql()).isEqualTo("SELECT 1 FROM t");
	}

	@Test
	@DisplayName("given a plain SQL response when generate is called then it returns it as-is")
	void generatesPlainSql() {
		stubChatClient("SELECT 2");

		QueryResponse response = service.generate(new QueryRequest("otra cosa"));

		assertThat(response.sql()).isEqualTo("SELECT 2");
	}
}