package com.capsula.mcp.client.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observabilidad tecnica del proceso de IA (Micrometer Observation).
 *
 * <p>Convive con el decorator {@link com.capsula.mcp.client.tool.RecordingToolCallback}:
 * <ul>
 *   <li>El decorator arma la traza que se DEVUELVE al usuario en la response (metadataUsed).</li>
 *   <li>Este handler emite LOGS tecnicos legibles de las observaciones RELEVANTES de
 *       Spring AI: las llamadas al modelo (chat) y a cada tool MCP, con su duracion.</li>
 * </ul>
 *
 * <p>El handler se declara como {@code @Bean}: Spring Boot lo auto-registra en el
 * {@link io.micrometer.observation.ObservationRegistry} que usa Spring AI (igual que los
 * handlers propios del framework). El filtrado se hace por TIPO de contexto, no por
 * nombre, porque durante {@code supportsContext} el nombre de la observacion aun es null.
 */
@Configuration
public class ObservabilityConfig {

	@Bean
	ObservationHandler<Observation.Context> springAiLoggingObservationHandler() {
		return new SpringAiLoggingObservationHandler();
	}

	/**
	 * Handler que loguea, de forma legible, solo las observaciones de IA que interesan:
	 * <ul>
	 *   <li>Llamadas al modelo de chat ({@code ChatModelObservationContext}): modelo + duracion.</li>
	 *   <li>Ejecucion de tools MCP ({@code ToolCallingObservationContext}): nombre de la tool + duracion.</li>
	 * </ul>
	 * Ignora capas intermedias (advisors, chat client) para evitar ruido.
	 */
	static class SpringAiLoggingObservationHandler implements ObservationHandler<Observation.Context> {

		private static final Logger log = LoggerFactory.getLogger("com.capsula.mcp.client.observability");
		private static final String START_NANOS = "capsula.startNanos";

		// Filtramos por el TIPO del contexto (durante supportsContext el nombre de la
		// observacion aun es null). Solo nos interesan estos dos.
		private static final String CHAT_MODEL_CTX = "ChatModelObservationContext";
		private static final String TOOL_CTX = "ToolCallingObservationContext";

		@Override
		public boolean supportsContext(Observation.Context context) {
			String simpleName = context.getClass().getSimpleName();
			return CHAT_MODEL_CTX.equals(simpleName) || TOOL_CTX.equals(simpleName);
		}

		@Override
		public void onStart(Observation.Context context) {
			context.put(START_NANOS, System.nanoTime());
		}

		@Override
		public void onStop(Observation.Context context) {
			String simpleName = context.getClass().getSimpleName();
			long ms = elapsedMillis(context);
			if (CHAT_MODEL_CTX.equals(simpleName)) {
				log.info("[IA] LLM   modelo={} · {} ms", tag(context, "gen_ai.request.model"), ms);
			}
			else if (TOOL_CTX.equals(simpleName)) {
				log.info("[IA] TOOL  {} · {} ms", tag(context, "spring.ai.tool.definition.name"), ms);
			}
		}

		@Override
		public void onError(Observation.Context context) {
			String simpleName = context.getClass().getSimpleName();
			Throwable error = context.getError();
			String what = CHAT_MODEL_CTX.equals(simpleName)
					? "LLM modelo=" + tag(context, "gen_ai.request.model")
					: "TOOL " + tag(context, "spring.ai.tool.definition.name");
			log.warn("[IA] ✖ ERROR {} · {} ms: {}", what, elapsedMillis(context),
					error != null ? error.toString() : "unknown");
		}

		/** Valor de un tag de baja cardinalidad; "-" si no esta presente. */
		private String tag(Observation.Context context, String key) {
			for (KeyValue kv : context.getLowCardinalityKeyValues()) {
				if (kv.getKey().equals(key)) {
					return kv.getValue();
				}
			}
			return "-";
		}

		private long elapsedMillis(Observation.Context context) {
			Object start = context.get(START_NANOS);
			if (start instanceof Long startNanos) {
				return (System.nanoTime() - startNanos) / 1_000_000;
			}
			return -1;
		}
	}
}