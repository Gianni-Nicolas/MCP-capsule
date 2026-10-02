package com.capsula.mcp.server.metadata.tool;

import com.capsula.mcp.server.metadata.model.ColumnInfo;
import com.capsula.mcp.server.metadata.model.ConstraintInfo;
import com.capsula.mcp.server.metadata.model.IndexInfo;
import com.capsula.mcp.server.metadata.model.PrimaryKeyInfo;
import com.capsula.mcp.server.metadata.model.SchemaInfo;
import com.capsula.mcp.server.metadata.model.TableInfo;
import com.capsula.mcp.server.metadata.model.TableSnapshot;
import com.capsula.mcp.server.metadata.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de la capa de tools MCP. Se mockea {@link MetadataService}
 * para verificar que cada tool traduce sus parametros y delega correctamente,
 * sin necesidad de una base de datos real.
 */
class DatabaseMetadataToolsTest {

	private MetadataService service;
	private DatabaseMetadataTools tools;

	@BeforeEach
	void setUp() {
		service = mock(MetadataService.class);
		tools = new DatabaseMetadataTools(service);
	}

	@Test
	void listSchemasDefaultsToExcludeSystem() {
		when(service.listSchemas(anyBoolean())).thenReturn(List.of(new SchemaInfo("PUBLIC")));

		var result = tools.listSchemas(null);

		verify(service).listSchemas(false);
		assertEquals(1, result.size());
	}

	@Test
	void listSchemasForwardsTrueWhenIncludeSystem() {
		when(service.listSchemas(anyBoolean())).thenReturn(List.of(new SchemaInfo("SYS")));

		tools.listSchemas(Boolean.TRUE);

		verify(service).listSchemas(true);
	}

	@Test
	void listTablesForwardsParamsAndDefaultsMaxResults() {
		when(service.listTables(eq("PUBLIC"), eq("EMP%"), anyInt()))
				.thenReturn(List.of(new TableInfo("PUBLIC", "EMPLOYEE", "TABLE", null)));

		var result = tools.listTables("PUBLIC", "EMP%", null);

		// null maxResults debe traducirse a 0 (default en el service)
		verify(service).listTables("PUBLIC", "EMP%", 0);
		assertEquals(1, result.size());
	}

	@Test
	void listTablesForwardsExplicitMaxResults() {
		when(service.listTables(eq("PUBLIC"), eq("%"), anyInt())).thenReturn(List.of());

		tools.listTables("PUBLIC", "%", 50);

		verify(service).listTables("PUBLIC", "%", 50);
	}

	@Test
	void getTableColumnsDelegatesToService() {
		var columns = List.of(
				new ColumnInfo("PUBLIC", "EMPLOYEE", "ID", 4, "BIGINT",
						false, null, 19, 0, 1));
		when(service.listColumns("PUBLIC", "EMPLOYEE")).thenReturn(columns);

		var result = tools.getTableColumns("PUBLIC", "EMPLOYEE");

		verify(service).listColumns("PUBLIC", "EMPLOYEE");
		assertSame(columns, result);
	}

	@Test
	void getForeignKeysDefaultsToImportedDirection() {
		when(service.getForeignKeys("PUBLIC", "EMPLOYEE")).thenReturn(List.of());

		tools.getForeignKeys("PUBLIC", "EMPLOYEE", null);

		verify(service).getForeignKeys("PUBLIC", "EMPLOYEE");
	}

	@Test
	void getForeignKeysUsesExportedWhenDirectionExported() {
		when(service.getExportedForeignKeys("PUBLIC", "DEPARTMENT")).thenReturn(List.of());

		tools.getForeignKeys("PUBLIC", "DEPARTMENT", "exported");

		verify(service).getExportedForeignKeys("PUBLIC", "DEPARTMENT");
	}

	@Test
	void getForeignKeysDirectionIsCaseInsensitive() {
		when(service.getExportedForeignKeys("PUBLIC", "DEPARTMENT")).thenReturn(List.of());

		tools.getForeignKeys("PUBLIC", "DEPARTMENT", "EXPORTED");

		verify(service).getExportedForeignKeys("PUBLIC", "DEPARTMENT");
	}

	@Test
	void getIndexesDelegatesToService() {
		var indexes = List.of(
				new IndexInfo("PUBLIC", "EMPLOYEE", "IDX_DEP", false, "OTHER",
						List.of("DEPARTMENT_ID")));
		when(service.getIndexes("PUBLIC", "EMPLOYEE")).thenReturn(indexes);

		var result = tools.getIndexes("PUBLIC", "EMPLOYEE");

		verify(service).getIndexes("PUBLIC", "EMPLOYEE");
		assertSame(indexes, result);
	}

	@Test
	void getConstraintsDelegatesToService() {
		var constraints = List.of(
				new ConstraintInfo("PUBLIC", "EMPLOYEE", "PK_EMPLOYEE",
						"PRIMARY_KEY", List.of("ID"), null));
		when(service.getConstraints("PUBLIC", "EMPLOYEE")).thenReturn(constraints);

		var result = tools.getConstraints("PUBLIC", "EMPLOYEE");

		verify(service).getConstraints("PUBLIC", "EMPLOYEE");
		assertSame(constraints, result);
	}

	@Test
	void getTableSnapshotDelegatesToService() {
		var snapshot = new TableSnapshot(
				new TableInfo("PUBLIC", "EMPLOYEE", "TABLE", null),
				List.of(),
				new PrimaryKeyInfo("PUBLIC", "EMPLOYEE", "PK_EMPLOYEE", List.of("ID")),
				List.of(),
				List.of());
		when(service.getTableSnapshot("PUBLIC", "EMPLOYEE")).thenReturn(snapshot);

		var result = tools.getTableSnapshot("PUBLIC", "EMPLOYEE");

		verify(service).getTableSnapshot("PUBLIC", "EMPLOYEE");
		assertSame(snapshot, result);
	}

}