package com.capsula.mcp.client.tool;

import com.capsula.mcp.client.dto.ToolInvocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.List;
import java.util.Map;

/**
 * Decorator de {@link ToolCallback} que delega en la tool MCP real y registra
 * cada invocacion (nombre, argumentos, resultado) en el
 * {@link ToolInvocationRecorder} del request actual.
 *
 * <p>Los argumentos y el resultado se parsean a JSON real (estructuras Map/List) para
 * que la response salga prolija (JSON anidado) en vez de strings escapados. Se usan
 * Map/List (Object) en lugar de JsonNode para que serialicen correctamente con
 * cualquier ObjectMapper de la capa web. El resultado MCP viene como un array de
 * bloques {@code [{"text":"..."}]} donde el {@code text} es a su vez JSON; se desanida
 * para exponerlo ya parseado.
 */
public class RecordingToolCallback implements ToolCallback {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final ToolCallback delegate;
	private final ToolInvocationRecorder recorder;

	public RecordingToolCallback(ToolCallback delegate, ToolInvocationRecorder recorder) {
		this.delegate = delegate;
		this.recorder = recorder;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		String result = delegate.call(toolInput);
		record(toolInput, result);
		return result;
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		String result = delegate.call(toolInput, toolContext);
		record(toolInput, result);
		return result;
	}

	private void record(String arguments, String result) {
		recorder.record(new ToolInvocation(
				delegate.getToolDefinition().name(),
				parseJson(arguments),
				unwrapMcpResult(result)
		));
	}

	/** Parsea un string a estructura JSON (Map/List/valor); si no es JSON valido, devuelve el string tal cual. */
	private Object parseJson(String raw) {
		if (raw == null) {
			return null;
		}
		try {
			return MAPPER.readValue(raw, Object.class);
		}
		catch (Exception ex) {
			return raw;
		}
	}

	/**
	 * Desanida el resultado MCP: normalmente es un array {@code [{"text":"<json>"}]}.
	 * Reemplaza cada campo "text" (string con JSON) por el JSON ya parseado, para que
	 * la response quede totalmente estructurada.
	 */
	@SuppressWarnings("unchecked")
	private Object unwrapMcpResult(String raw) {
		Object root = parseJson(raw);
		if (root instanceof List<?> list) {
			for (Object element : list) {
				if (element instanceof Map<?, ?> map && map.get("text") instanceof String text) {
					((Map<String, Object>) map).put("text", parseJson(text));
				}
			}
		}
		return root;
	}
}