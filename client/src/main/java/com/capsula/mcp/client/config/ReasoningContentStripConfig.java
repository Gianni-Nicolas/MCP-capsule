package com.capsula.mcp.client.config;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Varios providers "compatibles con OpenAI" que sirven modelos de reasoning
 * (p.ej. Groq con openai/gpt-oss-*, qwen3) agregan un campo propio
 * {@code reasoning_content} en el mensaje del assistant. Ese campo es de SOLO SALIDA:
 * el provider lo devuelve en la response pero lo RECHAZA si se lo reenvian en la request
 * (400: property 'reasoning_content' is unsupported).
 *
 * <p>En un flujo de tool-calling multi-turno, Spring AI deserializa la respuesta del
 * assistant incluyendo {@code reasoning_content} y la reenvia tal cual en el siguiente
 * turno, disparando el 400.
 *
 * <p>Spring AI 2.0.1 NO usa el RestClient de Spring para OpenAI: usa el SDK oficial
 * {@code com.openai.client.OpenAIClient} sobre OkHttp. Por eso el punto de extension
 * correcto es {@link OpenAiHttpClientBuilderCustomizer}, que permite registrar un
 * {@link Interceptor} de OkHttp. Este interceptor reescribe el body JSON saliente hacia
 * {@code /chat/completions} removiendo {@code reasoning_content} (y {@code reasoning})
 * de cada mensaje antes de que la request salga, resolviendo la incompatibilidad sin
 * cambiar de provider ni de modelo.
 *
 * <p>Esta siempre activo: es defensivo (solo actua sobre {@code /chat/completions} y solo
 * reescribe el body cuando realmente hay {@code reasoning_content}/{@code reasoning} que
 * remover). Para modelos sin razonamiento es un no-op (reenvia el body original tal cual),
 * por lo que no afecta a OpenRouter ni a otros providers que no emiten esos campos.
 */
@Configuration
public class ReasoningContentStripConfig {

	@Bean
	OpenAiHttpClientBuilderCustomizer stripReasoningContentCustomizer() {
		// ObjectMapper propio: no dependemos de un bean del contexto (puede no existir)
		// y la (de)serializacion de este JSON no requiere configuracion especial.
		return builder -> builder.interceptor(new ReasoningStripInterceptor(new ObjectMapper()));
	}

	/**
	 * Interceptor OkHttp que elimina {@code reasoning_content}/{@code reasoning} de los
	 * mensajes del body JSON en las requests a {@code /chat/completions}.
	 */
	static final class ReasoningStripInterceptor implements Interceptor {

		private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

		private final ObjectMapper mapper;

		ReasoningStripInterceptor(ObjectMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public Response intercept(Chain chain) throws IOException {
			Request request = chain.request();
			RequestBody body = request.body();

			if (body == null || !request.url().encodedPath().endsWith("/chat/completions")) {
				return chain.proceed(request);
			}

			byte[] original = readBody(body);
			byte[] sanitized = stripReasoning(original);

			if (sanitized == null) {
				// No hubo cambios: se reenvia el body original tal cual.
				Request rebuilt = request.newBuilder()
						.method(request.method(), RequestBody.create(original, contentType(body)))
						.build();
				return chain.proceed(rebuilt);
			}

			Request rebuilt = request.newBuilder()
					.method(request.method(), RequestBody.create(sanitized, JSON))
					.build();
			return chain.proceed(rebuilt);
		}

		private static byte[] readBody(RequestBody body) throws IOException {
			Buffer buffer = new Buffer();
			body.writeTo(buffer);
			return buffer.readByteArray();
		}

		private static MediaType contentType(RequestBody body) {
			MediaType ct = body.contentType();
			return ct != null ? ct : JSON;
		}

		/**
		 * Devuelve el body sanitizado, o {@code null} si no hubo nada que remover
		 * o si el body no era JSON parseable (fail-open).
		 */
		private byte[] stripReasoning(byte[] original) {
			try {
				JsonNode root = mapper.readTree(original);
				JsonNode messages = root.get("messages");
				if (messages == null || !messages.isArray()) {
					return null;
				}
				boolean modified = false;
				for (JsonNode msg : messages) {
					if (msg instanceof ObjectNode obj
							&& (obj.has("reasoning_content") || obj.has("reasoning"))) {
						obj.remove("reasoning_content");
						obj.remove("reasoning");
						modified = true;
					}
				}
				return modified ? mapper.writeValueAsBytes(root) : null;
			}
			catch (IOException ex) {
				return null;
			}
		}
	}
}