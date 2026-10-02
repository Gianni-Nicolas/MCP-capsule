package com.capsula.mcp.client.tool;

import com.capsula.mcp.client.dto.ToolInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RecordingToolCallback")
class RecordingToolCallbackTest {

	@Mock
	private ToolCallback delegate;

	@Mock
	private ToolInvocationRecorder recorder;

	@Mock
	private ToolDefinition toolDefinition;

	@Mock
	private ToolMetadata toolMetadata;

	private RecordingToolCallback callback;

	@BeforeEach
	void setUp() {
		when(toolDefinition.name()).thenReturn("db_get_table_snapshot");
		when(delegate.getToolDefinition()).thenReturn(toolDefinition);
		callback = new RecordingToolCallback(delegate, recorder);
	}

	@Test
	@DisplayName("given a callback when getToolDefinition is called then it delegates to the real callback")
	void delegatesToolDefinition() {
		assertThat(callback.getToolDefinition()).isSameAs(toolDefinition);
	}

	@Test
	@DisplayName("given a callback when getToolMetadata is called then it delegates to the real callback")
	void delegatesToolMetadata() {
		when(delegate.getToolMetadata()).thenReturn(toolMetadata);
		assertThat(callback.getToolMetadata()).isSameAs(toolMetadata);
	}

	private ToolInvocation capturedInvocation() {
		ArgumentCaptor<ToolInvocation> captor = ArgumentCaptor.forClass(ToolInvocation.class);
		verify(recorder).record(captor.capture());
		return captor.getValue();
	}

	@Test
	@DisplayName("given an MCP wrapped result when call(input) is invoked then it delegates, returns the result and records the invocation")
	void callWithoutContextRecords() {
		when(delegate.call("{\"table\":\"CUSTOMER\"}"))
				.thenReturn("[{\"text\":\"{\\\"rows\\\":5}\"}]");

		String result = callback.call("{\"table\":\"CUSTOMER\"}");

		assertThat(result).isEqualTo("[{\"text\":\"{\\\"rows\\\":5}\"}]");
		ToolInvocation inv = capturedInvocation();
		assertThat(inv.tool()).isEqualTo("db_get_table_snapshot");
		assertThat(inv.arguments()).isEqualTo(Map.of("table", "CUSTOMER"));
		// El resultado MCP viene como [{"text":"<json>"}] y se desanida el "text" ya parseado.
		assertThat(inv.result()).isInstanceOf(List.class);
		List<?> resultList = (List<?>) inv.result();
		assertThat(resultList).hasSize(1);
		Map<?, ?> block = (Map<?, ?>) resultList.get(0);
		assertThat(block.get("text")).isEqualTo(Map.of("rows", 5));
	}

	@Test
	@DisplayName("given a tool context when call(input, context) is invoked then it delegates with the context, returns the result and records")
	void callWithContextRecords() {
		ToolContext ctx = new ToolContext(Map.of());
		when(delegate.call(eq("{\"a\":1}"), any(ToolContext.class)))
				.thenReturn("plain-text-result");

		String result = callback.call("{\"a\":1}", ctx);

		assertThat(result).isEqualTo("plain-text-result");
		ToolInvocation inv = capturedInvocation();
		assertThat(inv.arguments()).isEqualTo(Map.of("a", 1));
		// Resultado que NO es JSON valido -> se registra el string tal cual.
		assertThat(inv.result()).isEqualTo("plain-text-result");
	}

	@Test
	@DisplayName("given non-JSON arguments when call is invoked then they are recorded as the raw string")
	void invalidJsonArgumentsKeptRaw() {
		when(delegate.call("not-json")).thenReturn("null");

		callback.call("not-json");

		ToolInvocation inv = capturedInvocation();
		assertThat(inv.arguments()).isEqualTo("not-json");
	}

	@Test
	@DisplayName("given a null result when call is invoked then it is recorded as null")
	void nullResultKeptNull() {
		when(delegate.call("{}")).thenReturn(null);

		callback.call("{}");

		ToolInvocation inv = capturedInvocation();
		assertThat(inv.arguments()).isEqualTo(Map.of());
		assertThat(inv.result()).isNull();
	}

	@Test
	@DisplayName("given a list block without a 'text' field when unwrapping the result then it is left unchanged")
	void listBlockWithoutTextUnchanged() {
		when(delegate.call("{}")).thenReturn("[{\"other\":\"value\"}]");

		callback.call("{}");

		ToolInvocation inv = capturedInvocation();
		assertThat(inv.result()).isEqualTo(List.of(Map.of("other", "value")));
	}

	@Test
	@DisplayName("given a non-list JSON result when call is invoked then it is recorded parsed as-is")
	void nonListJsonResultParsed() {
		when(delegate.call("{}")).thenReturn("{\"count\":3}");

		callback.call("{}");

		ToolInvocation inv = capturedInvocation();
		assertThat(inv.result()).isEqualTo(Map.of("count", 3));
	}
}