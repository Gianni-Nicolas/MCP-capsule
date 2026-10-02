package com.capsula.mcp.server.metadata.service;

import com.capsula.mcp.server.metadata.model.ConstraintInfo;
import com.capsula.mcp.server.metadata.model.ForeignKeyInfo;
import com.capsula.mcp.server.metadata.model.IndexInfo;
import com.capsula.mcp.server.metadata.model.TableSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JdbcMetadataServiceTest {

	/** Schema donde el schema.sql crea las tablas demo (ver SET SCHEMA en schema.sql). */
	private static final String SCHEMA = "PRUEBA_MCP";

	@Autowired
	private MetadataService metadataService;

	@Test
	void shouldExposeSchemasAndTablesFromConfiguredDatasource() {
		var schemas = metadataService.listSchemas();
		assertFalse(schemas.isEmpty(), "Se esperaba al menos un schema accesible");
		assertTrue(
				schemas.stream().anyMatch(s -> SCHEMA.equalsIgnoreCase(s.name())),
				"Se esperaba encontrar el schema " + SCHEMA
		);

		var tables = metadataService.listTables(SCHEMA, "%", 0);
		assertTrue(
				tables.stream().anyMatch(t -> "CUSTOMER".equalsIgnoreCase(t.name())),
				"Se esperaba encontrar la tabla CUSTOMER"
		);
		assertTrue(
				tables.stream().anyMatch(t -> "BANK_ACCOUNT".equalsIgnoreCase(t.name())),
				"Se esperaba encontrar la tabla BANK_ACCOUNT"
		);
	}

	@Test
	void shouldExposeColumnsForKnownTable() {
		var columns = metadataService.listColumns(SCHEMA, "BANK_ACCOUNT");
		assertFalse(columns.isEmpty(), "Se esperaba metadata de columnas para BANK_ACCOUNT");
		assertTrue(
				columns.stream()
						.anyMatch(c -> "customer_id".equals(c.name().toLowerCase(Locale.ROOT))),
				"Se esperaba la columna customer_id"
		);
	}

	@Test
	void shouldNormalizeIdentifierCaseInLowerCaseInput() {
		var columns = metadataService.listColumns("prueba_mcp", "bank_account");
		assertFalse(columns.isEmpty(), "El servicio debe normalizar identifiers a mayusculas en H2");
	}

	@Test
	void shouldReturnSnapshotWithPrimaryKey() {
		TableSnapshot snapshot = metadataService.getTableSnapshot(SCHEMA, "BANK_ACCOUNT");
		assertNotNull(snapshot.primaryKey(), "BANK_ACCOUNT debe tener primary key");
		assertTrue(
				snapshot.primaryKey().columns().stream()
						.anyMatch("ID"::equalsIgnoreCase),
				"La PK de BANK_ACCOUNT debe incluir la columna ID"
		);
		assertFalse(snapshot.columns().isEmpty(), "Snapshot debe traer columnas");
	}

	@Test
	void snapshotShouldIncludeForeignKeysAndIndexes() {
		TableSnapshot snapshot = metadataService.getTableSnapshot(SCHEMA, "BANK_ACCOUNT");
		assertNotNull(snapshot.foreignKeys(), "El snapshot debe traer lista de FKs (aunque este vacia)");
		assertNotNull(snapshot.indexes(), "El snapshot debe traer lista de indices (aunque este vacia)");
		assertTrue(
				snapshot.foreignKeys().stream()
						.anyMatch(fk -> "CUSTOMER".equalsIgnoreCase(fk.pkTable())),
				"BANK_ACCOUNT debe tener FK saliente a CUSTOMER"
		);
	}

	@Test
	void shouldExcludeSystemSchemasByDefault() {
		var schemas = metadataService.listSchemas(false);
		assertTrue(
				schemas.stream().noneMatch(s -> "INFORMATION_SCHEMA".equalsIgnoreCase(s.name())),
				"INFORMATION_SCHEMA no deberia estar visible por default"
		);
	}

	@Test
	void shouldRejectInvalidIdentifiers() {
		assertThrows(IllegalArgumentException.class,
				() -> metadataService.listColumns(SCHEMA, "employees; DROP TABLE users;"));
	}

	@Test
	void shouldReturnImportedForeignKeysFromBankAccountToCustomer() {
		List<ForeignKeyInfo> fks = metadataService.getForeignKeys(SCHEMA, "BANK_ACCOUNT");
		assertFalse(fks.isEmpty(), "BANK_ACCOUNT deberia tener al menos una FK saliente");
		ForeignKeyInfo fk = fks.stream()
				.filter(f -> "CUSTOMER".equalsIgnoreCase(f.pkTable()))
				.findFirst()
				.orElseThrow();
		assertTrue("customer_id".equalsIgnoreCase(fk.fkColumn()),
				"La columna origen deberia ser customer_id");
		assertNotNull(fk.updateRule(), "updateRule debe estar seteado");
		assertNotNull(fk.deleteRule(), "deleteRule debe estar seteado");
	}

	@Test
	void shouldReturnExportedForeignKeysFromCustomerToBankAccount() {
		List<ForeignKeyInfo> fks = metadataService.getExportedForeignKeys(SCHEMA, "CUSTOMER");
		assertFalse(fks.isEmpty(), "CUSTOMER deberia tener FKs entrantes desde BANK_ACCOUNT");
		assertTrue(
				fks.stream().anyMatch(fk -> "BANK_ACCOUNT".equalsIgnoreCase(fk.fkTable())),
				"Alguna FK entrante deberia venir de BANK_ACCOUNT"
		);
	}

	@Test
	void shouldReturnIndexesGroupedByName() {
		List<IndexInfo> indexes = metadataService.getIndexes(SCHEMA, "BANK_ACCOUNT");
		assertFalse(indexes.isEmpty(), "BANK_ACCOUNT deberia tener indices (PK + custom + FK)");
		assertTrue(
				indexes.stream().anyMatch(i -> !i.columns().isEmpty()),
				"Los indices deben tener sus columnas resueltas"
		);
	}

	@Test
	void shouldReturnConstraintsWithPkUniqueAndFk() {
		List<ConstraintInfo> constraints = metadataService.getConstraints(SCHEMA, "BANK_ACCOUNT");
		assertTrue(
				constraints.stream().anyMatch(c -> "PRIMARY_KEY".equals(c.type())),
				"Debe existir constraint PRIMARY_KEY"
		);
		assertTrue(
				constraints.stream().anyMatch(c -> "UNIQUE".equals(c.type())),
				"Debe existir constraint UNIQUE (account_number es UNIQUE en el schema demo)"
		);
		assertTrue(
				constraints.stream().anyMatch(c -> "FOREIGN_KEY".equals(c.type())),
				"Debe existir constraint FOREIGN_KEY hacia CUSTOMER/BRANCH"
		);
	}

	@Test
	void constraintPrimaryKeyShouldMatchIdColumn() {
		List<ConstraintInfo> constraints = metadataService.getConstraints(SCHEMA, "BANK_ACCOUNT");
		ConstraintInfo pk = constraints.stream()
				.filter(c -> "PRIMARY_KEY".equals(c.type()))
				.findFirst()
				.orElseThrow();
		assertEquals(1, pk.columns().size(), "La PK de BANK_ACCOUNT es simple (una columna)");
		assertTrue("ID".equalsIgnoreCase(pk.columns().getFirst()), "La PK debe ser la columna ID");
	}

}