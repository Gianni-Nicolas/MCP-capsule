package com.capsula.mcp.server.metadata.tool;

import com.capsula.mcp.server.metadata.model.ColumnInfo;
import com.capsula.mcp.server.metadata.model.ConstraintInfo;
import com.capsula.mcp.server.metadata.model.ForeignKeyInfo;
import com.capsula.mcp.server.metadata.model.IndexInfo;
import com.capsula.mcp.server.metadata.model.SchemaInfo;
import com.capsula.mcp.server.metadata.model.TableInfo;
import com.capsula.mcp.server.metadata.model.TableSnapshot;
import com.capsula.mcp.server.metadata.service.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tools MCP que exponen metadata JDBC agnostica de motor.
 * Cada tool logea invocacion y tamano de resultado para trazabilidad.
 */
@Component
public class DatabaseMetadataTools {

	private static final Logger log = LoggerFactory.getLogger(DatabaseMetadataTools.class);

	private final MetadataService metadataService;

	public DatabaseMetadataTools(MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Tool(name = "db_list_schemas",
			description = "Lista los esquemas accesibles en la base configurada. Por default oculta schemas del sistema.")
	public List<SchemaInfo> listSchemas(
			@ToolParam(required = false,
					description = "Si es true incluye schemas del sistema (INFORMATION_SCHEMA, SYS, PG_CATALOG, etc.). Default false.")
			Boolean includeSystem
	) {
		boolean includeSys = Boolean.TRUE.equals(includeSystem);
		log.info("MCP db_list_schemas includeSystem={}", includeSys);
		List<SchemaInfo> result = metadataService.listSchemas(includeSys);
		log.debug("db_list_schemas returned {} schemas", result.size());
		return result;
	}

	@Tool(name = "db_list_tables",
			description = "Lista tablas y vistas de un schema. tablePattern acepta comodines SQL (% y _). Ambos parametros son opcionales.")
	public List<TableInfo> listTables(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(required = false,
					description = "Patron LIKE de tabla (ej: CUSTOMER%). Default: % (todas).")
			String tablePattern,
			@ToolParam(required = false,
					description = "Maximo de resultados. Default configurado por servidor (200).")
			Integer maxResults
	) {
		int limit = maxResults == null ? 0 : maxResults;
		log.info("MCP db_list_tables schema={} pattern={} maxResults={}", schema, tablePattern, limit);
		List<TableInfo> result = metadataService.listTables(schema, tablePattern, limit);
		log.debug("db_list_tables returned {} tables", result.size());
		return result;
	}

	@Tool(name = "db_get_table_columns",
			description = "Devuelve metadata detallada de columnas de una tabla (tipo, nullability, default, size, scale, orden).")
	public List<ColumnInfo> getTableColumns(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(description = "Nombre exacto de la tabla (sin comillas). Obligatorio.")
			String table
	) {
		log.info("MCP db_get_table_columns schema={} table={}", schema, table);
		List<ColumnInfo> result = metadataService.listColumns(schema, table);
		log.debug("db_get_table_columns returned {} columns for {}", result.size(), table);
		return result;
	}

	@Tool(name = "db_get_foreign_keys",
			description = "Devuelve las foreign keys de una tabla. Por default lista las SALIENTES (referencias de esta tabla hacia otras). "
					+ "Si direction='exported' lista las ENTRANTES (otras tablas que referencian a esta).")
	public List<ForeignKeyInfo> getForeignKeys(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(description = "Nombre exacto de la tabla. Obligatorio.")
			String table,
			@ToolParam(required = false,
					description = "Direccion: 'imported' (default, FKs de esta tabla) o 'exported' (FKs hacia esta tabla).")
			String direction
	) {
		boolean exported = "exported".equalsIgnoreCase(direction);
		log.info("MCP db_get_foreign_keys schema={} table={} direction={}",
				schema, table, exported ? "exported" : "imported");
		List<ForeignKeyInfo> result = exported
				? metadataService.getExportedForeignKeys(schema, table)
				: metadataService.getForeignKeys(schema, table);
		log.debug("db_get_foreign_keys returned {} FKs for {}", result.size(), table);
		return result;
	}

	@Tool(name = "db_get_indexes",
			description = "Devuelve los indices de una tabla (nombre, unicidad, tipo, columnas ordenadas). "
					+ "Los indices de estadisticas se omiten.")
	public List<IndexInfo> getIndexes(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(description = "Nombre exacto de la tabla. Obligatorio.")
			String table
	) {
		log.info("MCP db_get_indexes schema={} table={}", schema, table);
		List<IndexInfo> result = metadataService.getIndexes(schema, table);
		log.debug("db_get_indexes returned {} indexes for {}", result.size(), table);
		return result;
	}

	@Tool(name = "db_get_constraints",
			description = "Devuelve las restricciones PK, UNIQUE y FOREIGN_KEY de una tabla. "
					+ "Las CHECK constraints se omiten por falta de soporte estandar en JDBC.")
	public List<ConstraintInfo> getConstraints(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(description = "Nombre exacto de la tabla. Obligatorio.")
			String table
	) {
		log.info("MCP db_get_constraints schema={} table={}", schema, table);
		List<ConstraintInfo> result = metadataService.getConstraints(schema, table);
		log.debug("db_get_constraints returned {} constraints for {}", result.size(), table);
		return result;
	}

	/* 	Resuelve la tarea con menos iteraciones del loop de tool-calling, pero esta
		DESACTIVADA a proposito: queremos observar al LLM orquestar varias tools
		atomicas en el loop en vez de resolver todo en un unico call.
		Fue utilizada inicialmente en las primeras pruebas entre el cliente y servidor. */
	/*@Tool(name = "db_get_table_snapshot",
			description = "Vista agregada de una tabla en una sola llamada: metadata + columnas + primary key + foreign keys + indices. "
					+ "Ideal para que un LLM entienda una tabla completa antes de generar una query.")*/
	public TableSnapshot getTableSnapshot(
			@ToolParam(required = false,
					description = "Schema objetivo. Si es null usa el default del usuario JDBC.")
			String schema,
			@ToolParam(description = "Nombre exacto de la tabla. Obligatorio.")
			String table
	) {
		log.info("MCP db_get_table_snapshot schema={} table={}", schema, table);
		TableSnapshot snapshot = metadataService.getTableSnapshot(schema, table);
		log.debug("db_get_table_snapshot table={} columns={} fks={} indexes={} hasPk={}",
				table, snapshot.columns().size(), snapshot.foreignKeys().size(),
				snapshot.indexes().size(), snapshot.primaryKey() != null);
		return snapshot;
	}

}
