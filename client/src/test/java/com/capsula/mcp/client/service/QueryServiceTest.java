package com.capsula.mcp.client.service;

import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import com.capsula.mcp.client.dto.ToolInvocation;
import com.capsula.mcp.client.tool.ToolInvocationRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QueryService")
class QueryServiceTest {

	private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

	@Mock
	private ToolInvocationRecorder recorder;

	private QueryService service;

	private void stubChatClient(String raw) {
		when(chatClient.prompt().user(anyString()).call().content()).thenReturn(raw);
	}

	@Test
	@DisplayName("given includeMetadata true when generate is called then it returns cleaned SQL, clears the recorder and attaches metadata")
	void generatesWithMetadata() {
		service = new QueryService(chatClient, recorder, new SqlResponseCleaner());
		stubChatClient("```sql\nSELECT 1\nFROM t\n```");
		List<ToolInvocation> trace = List.of(new ToolInvocation("db_list_tables", null, null));
		when(recorder.snapshot()).thenReturn(trace);

		QueryResponse response = service.generate(new QueryRequest("dame algo", true));

		assertThat(response.sql()).isEqualTo("SELECT 1 FROM t");
		assertThat(response.metadataUsed()).isEqualTo(trace);
		verify(recorder).clear();
	}

	@Test
	@DisplayName("given includeMetadata false when generate is called then metadataUsed is null")
	void generatesWithoutMetadata() {
		service = new QueryService(chatClient, recorder, new SqlResponseCleaner());
		stubChatClient("SELECT 2");
		when(recorder.snapshot()).thenReturn(List.of());

		QueryResponse response = service.generate(new QueryRequest("otra cosa", false));

		assertThat(response.sql()).isEqualTo("SELECT 2");
		assertThat(response.metadataUsed()).isNull();
	}

	@Test
	@DisplayName("given a request when generate is called then the recorder is cleared before invoking the chat client")
	void clearsRecorderBeforeCallingChatClient() {
		service = new QueryService(chatClient, recorder, new SqlResponseCleaner());
		stubChatClient("SELECT 3");
		when(recorder.snapshot()).thenReturn(List.of());

		service.generate(new QueryRequest("pedido", false));

		// recorder.clear() debe ocurrir antes de recorder.snapshot()
		var order = inOrder(recorder);
		order.verify(recorder).clear();
		order.verify(recorder).snapshot();
	}
}