package com.capsula.mcp.server.metadata.service;

import com.capsula.mcp.server.metadata.config.MetadataProperties;
import com.capsula.mcp.server.metadata.exception.MetadataAccessException;
import com.capsula.mcp.server.metadata.model.ColumnInfo;
import com.capsula.mcp.server.metadata.model.ConstraintInfo;
import com.capsula.mcp.server.metadata.model.ForeignKeyInfo;
import com.capsula.mcp.server.metadata.model.IndexInfo;
import com.capsula.mcp.server.metadata.model.PrimaryKeyInfo;
import com.capsula.mcp.server.metadata.model.SchemaInfo;
import com.capsula.mcp.server.metadata.model.TableInfo;
import com.capsula.mcp.server.metadata.model.TableSnapshot;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class JdbcMetadataService implements MetadataService {

	/** Identifier valido conservador: letras, digitos, guion bajo. Sin comillas ni espacios. */
	private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

	private final DataSource dataSource;
	private final MetadataProperties properties;

	public JdbcMetadataService(DataSource dataSource, MetadataProperties properties) {
		this.dataSource = dataSource;
		this.properties = properties;
	}

	@Override
	public List<SchemaInfo> listSchemas(boolean includeSystem) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			List<SchemaInfo> schemas = new ArrayList<>();
			try (ResultSet rs = metaData.getSchemas()) {
				while (rs.next()) {
					String name = rs.getString("TABLE_SCHEM");
					if (includeSystem || !isExcluded(name)) {
						schemas.add(new SchemaInfo(name));
					}
				}
			}
			return schemas;
		}
		catch (SQLException ex) {
			throw wrap("No se pudo listar schemas", ex);
		}
	}

	@Override
	public List<TableInfo> listTables(String schema, String tablePattern, int maxResults) {
		int limit = resolveLimit(maxResults);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			IdentifierNormalizer normalizer = IdentifierNormalizer.from(metaData);
			String schemaN = normalizer.normalize(schema);
			String patternN = normalizer.normalizePattern(tablePattern);

			List<TableInfo> tables = new ArrayList<>();
			try (ResultSet rs = metaData.getTables(null, schemaN, patternN, new String[]{"TABLE", "VIEW"})) {
				while (rs.next() && tables.size() < limit) {
					String schemaName = rs.getString("TABLE_SCHEM");
					if (isExcluded(schemaName)) {
						continue;
					}
					tables.add(new TableInfo(
							schemaName,
							rs.getString("TABLE_NAME"),
							rs.getString("TABLE_TYPE"),
							rs.getString("REMARKS")
					));
				}
			}
			return tables;
		}
		catch (SQLException ex) {
			throw wrap("No se pudo listar tablas", ex);
		}
	}

	@Override
	public List<ColumnInfo> listColumns(String schema, String table) {
		requireTable(table);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			IdentifierNormalizer normalizer = IdentifierNormalizer.from(metaData);
			String schemaN = normalizer.normalize(schema);
			String tableN = normalizer.normalize(table);

			List<ColumnInfo> columns = new ArrayList<>();
			try (ResultSet rs = metaData.getColumns(null, schemaN, tableN, "%")) {
				while (rs.next()) {
					columns.add(new ColumnInfo(
							rs.getString("TABLE_SCHEM"),
							rs.getString("TABLE_NAME"),
							rs.getString("COLUMN_NAME"),
							rs.getInt("DATA_TYPE"),
							rs.getString("TYPE_NAME"),
							rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
							rs.getString("COLUMN_DEF"),
							rs.getInt("COLUMN_SIZE"),
							rs.getInt("DECIMAL_DIGITS"),
							rs.getInt("ORDINAL_POSITION")
					));
				}
			}
			return columns;
		}
		catch (SQLException ex) {
			throw wrap("No se pudo listar columnas de " + table, ex);
		}
	}

	@Override
	public Optional<PrimaryKeyInfo> getPrimaryKey(String schema, String table) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			IdentifierNormalizer normalizer = IdentifierNormalizer.from(metaData);
			String schemaN = normalizer.normalize(schema);
			String tableN = normalizer.normalize(table);

			String pkName = null;
			String resolvedSchema = schemaN;
			String resolvedTable = tableN;
			List<String> columns = new ArrayList<>();
			try (ResultSet rs = metaData.getPrimaryKeys(null, schemaN, tableN)) {
				while (rs.next()) {
					pkName = rs.getString("PK_NAME");
					resolvedSchema = rs.getString("TABLE_SCHEM");
					resolvedTable = rs.getString("TABLE_NAME");
					columns.add(rs.getString("COLUMN_NAME"));
				}
			}
			if (columns.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new PrimaryKeyInfo(resolvedSchema, resolvedTable, pkName, columns));
		}
		catch (SQLException ex) {
			throw wrap("No se pudo obtener PK de " + table, ex);
		}
	}

	@Override
	public List<ForeignKeyInfo> getForeignKeys(String schema, String table) {
		return readForeignKeys(schema, table, true);
	}

	@Override
	public List<ForeignKeyInfo> getExportedForeignKeys(String schema, String table) {
		return readForeignKeys(schema, table, false);
	}

	@Override
	public List<IndexInfo> getIndexes(String schema, String table) {
		requireTable(table);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			IdentifierNormalizer normalizer = IdentifierNormalizer.from(metaData);
			String schemaN = normalizer.normalize(schema);
			String tableN = normalizer.normalize(table);

			// Agrupamos por INDEX_NAME manteniendo orden de columnas por ORDINAL_POSITION.
			Map<String, IndexAccumulator> byName = new LinkedHashMap<>();
			try (ResultSet rs = metaData.getIndexInfo(null, schemaN, tableN, false, true)) {
				while (rs.next()) {
					// Los rows con TYPE=tableIndexStatistic tienen INDEX_NAME null: se descartan.
					if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
						continue;
					}
					String idxName = rs.getString("INDEX_NAME");
					if (idxName == null) {
						continue;
					}
					// Extraemos fuera del lambda para poder propagar SQLException.
					String idxSchema = rs.getString("TABLE_SCHEM");
					String idxTable = rs.getString("TABLE_NAME");
					boolean unique = !rs.getBoolean("NON_UNIQUE");
					String type = describeIndexType(rs.getShort("TYPE"));
					String column = rs.getString("COLUMN_NAME");

					IndexAccumulator acc = byName.computeIfAbsent(idxName,
							k -> new IndexAccumulator(idxSchema, idxTable, k, unique, type));
					acc.columns.add(column);
				}
			}
			List<IndexInfo> result = new ArrayList<>(byName.size());
			for (IndexAccumulator acc : byName.values()) {
				result.add(new IndexInfo(acc.schema, acc.table, acc.name, acc.unique, acc.type, acc.columns));
			}
			return result;
		}
		catch (SQLException ex) {
			throw wrap("No se pudo obtener indices de " + table, ex);
		}
	}

	@Override
	public List<ConstraintInfo> getConstraints(String schema, String table) {
		requireTable(table);
		List<ConstraintInfo> result = new ArrayList<>();

		// 1) PRIMARY KEY
		getPrimaryKey(schema, table).ifPresent(pk ->
				result.add(new ConstraintInfo(
						pk.schema(), pk.table(),
						pk.pkName() != null ? pk.pkName() : "PK_" + pk.table(),
						"PRIMARY_KEY", pk.columns(), null)));

		// 2) UNIQUE (indices unicos que NO son la PK)
		List<String> pkColumns = result.isEmpty() ? List.of() : result.getFirst().columns();
		for (IndexInfo idx : getIndexes(schema, table)) {
			if (idx.unique() && !sameColumns(idx.columns(), pkColumns)) {
				result.add(new ConstraintInfo(
						idx.schema(), idx.table(), idx.indexName(),
						"UNIQUE", idx.columns(), null));
			}
		}

		// 3) FOREIGN KEYS (agrupamos por fkName para PK/FK compuestas)
		Map<String, ConstraintAccumulator> fkByName = new LinkedHashMap<>();
		for (ForeignKeyInfo fk : getForeignKeys(schema, table)) {
			String key = fk.fkName() != null ? fk.fkName() : "FK_" + fk.fkTable() + "_" + fk.fkColumn();
			ConstraintAccumulator acc = fkByName.computeIfAbsent(key, k -> new ConstraintAccumulator(
					fk.fkSchema(), fk.fkTable(), k));
			acc.columns.add(fk.fkColumn());
		}
		for (ConstraintAccumulator acc : fkByName.values()) {
			result.add(new ConstraintInfo(acc.schema, acc.table, acc.name, "FOREIGN_KEY", acc.columns, null));
		}

		// CHECK constraints se omiten: no hay API estandar en DatabaseMetaData.
		return result;
	}

	@Override
	public TableSnapshot getTableSnapshot(String schema, String table) {
		requireTable(table);
		List<TableInfo> found = listTables(schema, table, 1);
		if (found.isEmpty()) {
			throw new MetadataAccessException(
					"Tabla no encontrada: " + (schema == null ? "" : schema + ".") + table, null);
		}
		TableInfo tableInfo = found.getFirst();
		List<ColumnInfo> columns = listColumns(tableInfo.schema(), tableInfo.name());
		PrimaryKeyInfo pk = getPrimaryKey(tableInfo.schema(), tableInfo.name()).orElse(null);
		List<ForeignKeyInfo> fks = getForeignKeys(tableInfo.schema(), tableInfo.name());
		List<IndexInfo> idxs = getIndexes(tableInfo.schema(), tableInfo.name());
		return new TableSnapshot(tableInfo, columns, pk, fks, idxs);
	}

	// --- Helpers ---

	private List<ForeignKeyInfo> readForeignKeys(String schema, String table, boolean imported) {
		requireTable(table);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			IdentifierNormalizer normalizer = IdentifierNormalizer.from(metaData);
			String schemaN = normalizer.normalize(schema);
			String tableN = normalizer.normalize(table);

			List<ForeignKeyInfo> fks = new ArrayList<>();
			try (ResultSet rs = imported
					? metaData.getImportedKeys(null, schemaN, tableN)
					: metaData.getExportedKeys(null, schemaN, tableN)) {
				while (rs.next()) {
					fks.add(new ForeignKeyInfo(
							rs.getString("FK_NAME"),
							rs.getString("PK_NAME"),
							rs.getString("FKTABLE_SCHEM"),
							rs.getString("FKTABLE_NAME"),
							rs.getString("FKCOLUMN_NAME"),
							rs.getString("PKTABLE_SCHEM"),
							rs.getString("PKTABLE_NAME"),
							rs.getString("PKCOLUMN_NAME"),
							rs.getInt("KEY_SEQ"),
							describeFkRule(rs.getShort("UPDATE_RULE")),
							describeFkRule(rs.getShort("DELETE_RULE"))
					));
				}
			}
			return fks;
		}
		catch (SQLException ex) {
			throw wrap("No se pudieron obtener FKs de " + table, ex);
		}
	}

	private static String describeFkRule(short rule) {
		return switch (rule) {
			case DatabaseMetaData.importedKeyCascade -> "CASCADE";
			case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
			case DatabaseMetaData.importedKeySetNull -> "SET_NULL";
			case DatabaseMetaData.importedKeyNoAction -> "NO_ACTION";
			case DatabaseMetaData.importedKeySetDefault -> "SET_DEFAULT";
			default -> "UNKNOWN";
		};
	}

	private static String describeIndexType(short type) {
		return switch (type) {
			case DatabaseMetaData.tableIndexClustered -> "CLUSTERED";
			case DatabaseMetaData.tableIndexHashed -> "HASHED";
			case DatabaseMetaData.tableIndexOther -> "OTHER";
			case DatabaseMetaData.tableIndexStatistic -> "STATISTIC";
			default -> "UNKNOWN";
		};
	}

	private static boolean sameColumns(List<String> a, List<String> b) {
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).equalsIgnoreCase(b.get(i))) {
				return false;
			}
		}
		return true;
	}

	private int resolveLimit(int requested) {
		int configured = properties.getMaxResults();
		if (requested <= 0) {
			return configured;
		}
		return Math.min(requested, configured);
	}

	private boolean isExcluded(String schema) {
		if (schema == null) {
			return false;
		}
		return properties.getExcludedSchemas().contains(schema.toUpperCase(Locale.ROOT));
	}

	private static void requireTable(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("El campo 'table' es obligatorio");
		}
		if (!SAFE_IDENTIFIER.matcher(value).matches()) {
			throw new IllegalArgumentException(
					"El campo 'table' contiene caracteres invalidos: " + value);
		}
	}

	private static MetadataAccessException wrap(String message, SQLException ex) {
		return new MetadataAccessException(
				message + " (SQLState=" + ex.getSQLState() + ", " + ex.getMessage() + ")",
				ex.getSQLState(),
				ex
		);
	}

	// Estructuras internas para agrupacion.
	private static final class IndexAccumulator {
		final String schema;
		final String table;
		final String name;
		final boolean unique;
		final String type;
		final List<String> columns = new ArrayList<>();

		IndexAccumulator(String schema, String table, String name, boolean unique, String type) {
			this.schema = schema;
			this.table = table;
			this.name = name;
			this.unique = unique;
			this.type = type;
		}
	}

	private static final class ConstraintAccumulator {
		final String schema;
		final String table;
		final String name;
		final List<String> columns = new ArrayList<>();

		ConstraintAccumulator(String schema, String table, String name) {
			this.schema = schema;
			this.table = table;
			this.name = name;
		}
	}

}
