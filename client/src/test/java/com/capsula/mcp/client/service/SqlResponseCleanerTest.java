package com.capsula.mcp.client.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlResponseCleaner")
class SqlResponseCleanerTest {

	private final SqlResponseCleaner cleaner = new SqlResponseCleaner();

	@Test
	@DisplayName("given null input when clean is called then it returns an empty string")
	void returnsEmptyOnNull() {
		assertThat(cleaner.clean(null)).isEmpty();
	}

	@Test
	@DisplayName("given blank input when clean is called then it returns an empty string")
	void returnsEmptyOnBlank() {
		assertThat(cleaner.clean("   \n\t  ")).isEmpty();
	}

	@Test
	@DisplayName("given markdown fences with language when clean is called then they are stripped")
	void stripsFencesWithLanguage() {
		String raw = "```sql\nSELECT 1 FROM dual\n```";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT 1 FROM dual");
	}

	@Test
	@DisplayName("given markdown fences without language when clean is called then they are stripped")
	void stripsFencesWithoutLanguage() {
		String raw = "```\nSELECT 1\n```";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT 1");
	}

	@Test
	@DisplayName("given an opening fence without newline when clean is called then only the closing fence is removed")
	void fenceWithoutNewlineKeepsContent() {
		// Caso borde: empieza con ``` pero no hay '\n' -> no entra al substring de apertura.
		String raw = "```SELECT 1```";
		// El cierre ``` se remueve; el bloque de apertura no (no hay newline).
		assertThat(cleaner.clean(raw)).isEqualTo("```SELECT 1");
	}

	@Test
	@DisplayName("given newlines and multiple spaces when clean is called then they are collapsed into a single line")
	void collapsesWhitespaceToSingleLine() {
		String raw = "SELECT a,\n  b\nFROM   t\n\nWHERE a = 1";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT a, b FROM t WHERE a = 1");
	}

	@Test
	@DisplayName("given whitespace inside string literals when clean is called then it is also collapsed")
	void collapsesWhitespaceInsideStringLiterals() {
		String raw = "SELECT *\nFROM t\nWHERE name = 'Juan   Perez'";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT * FROM t WHERE name = 'Juan Perez'");
	}

	@Test
	@DisplayName("given a newline inside a string literal when clean is called then it is collapsed")
	void collapsesNewlineInsideStringLiteral() {
		String raw = "SELECT 'linea1\nlinea2' FROM t";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT 'linea1 linea2' FROM t");
	}

	@Test
	@DisplayName("given escaped single quotes when clean is called then surrounding whitespace is collapsed")
	void collapsesAroundEscapedSingleQuotes() {
		String raw = "SELECT *  FROM t  WHERE name = 'O''Brien   test'";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT * FROM t WHERE name = 'O''Brien test'");
	}

	@Test
	@DisplayName("given fenced SQL with extra spaces when clean is called then fences are stripped and whitespace collapsed")
	void stripsFencesAndCollapses() {
		String raw = "```sql\nSELECT a\nFROM   t\n```";
		assertThat(cleaner.clean(raw)).isEqualTo("SELECT a FROM t");
	}
}