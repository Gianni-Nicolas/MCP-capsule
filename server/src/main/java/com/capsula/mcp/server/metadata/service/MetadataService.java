package com.capsula.mcp.server.metadata.service;

import com.capsula.mcp.server.metadata.model.ColumnInfo;
import com.capsula.mcp.server.metadata.model.ConstraintInfo;
import com.capsula.mcp.server.metadata.model.ForeignKeyInfo;
import com.capsula.mcp.server.metadata.model.IndexInfo;
import com.capsula.mcp.server.metadata.model.PrimaryKeyInfo;
import com.capsula.mcp.server.metadata.model.SchemaInfo;
import com.capsula.mcp.server.metadata.model.TableInfo;
import com.capsula.mcp.server.metadata.model.TableSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Contrato agnostico de vendor para exponer metadata del esquema activo.
 * Todas las implementaciones deben ser thread-safe y read-only.
 */
public interface MetadataService {

	List<SchemaInfo> listSchemas(boolean includeSystem);

	default List<SchemaInfo> listSchemas() {
		return listSchemas(false);
	}

	List<TableInfo> listTables(String schema, String tablePattern, int maxResults);

	default List<TableInfo> listTables(String schema, String tablePattern) {
		return listTables(schema, tablePattern, Integer.MAX_VALUE);
	}

	List<ColumnInfo> listColumns(String schema, String table);

	Optional<PrimaryKeyInfo> getPrimaryKey(String schema, String table);

	/** FKs que salen desde esta tabla hacia otras (referencias de esta tabla). */
	List<ForeignKeyInfo> getForeignKeys(String schema, String table);

	/** FKs de otras tablas que apuntan a esta (referencias entrantes). */
	List<ForeignKeyInfo> getExportedForeignKeys(String schema, String table);

	List<IndexInfo> getIndexes(String schema, String table);

	/**
	 * Agrega PK + UNIQUE + FK en una sola vista.
	 * CHECK constraints se omiten por falta de soporte estandar en JDBC.
	 */
	List<ConstraintInfo> getConstraints(String schema, String table);

	TableSnapshot getTableSnapshot(String schema, String table);

}
