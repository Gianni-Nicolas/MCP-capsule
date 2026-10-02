package com.capsula.mcp.client.tool;

import com.capsula.mcp.client.dto.ToolInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ToolInvocationRecorder")
class ToolInvocationRecorderTest {

	private final ToolInvocationRecorder recorder = new ToolInvocationRecorder();

	@Test
	@DisplayName("given a new recorder when snapshot is called then it is empty")
	void snapshotEmptyInitially() {
		assertThat(recorder.snapshot()).isEmpty();
	}

	@Test
	@DisplayName("given several invocations when record is called then they are accumulated in order")
	void recordsInvocations() {
		ToolInvocation a = new ToolInvocation("db_list_tables", null, null);
		ToolInvocation b = new ToolInvocation("db_get_table_columns", null, null);

		recorder.record(a);
		recorder.record(b);

		assertThat(recorder.snapshot()).containsExactly(a, b);
	}

	@Test
	@DisplayName("given recorded invocations when snapshot is called then it returns an immutable copy")
	void snapshotIsImmutable() {
		recorder.record(new ToolInvocation("t", null, null));
		var snapshot = recorder.snapshot();

		assertThatThrownBy(() -> snapshot.add(new ToolInvocation("x", null, null)))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@DisplayName("given recorded invocations when clear is called then the accumulated invocations are emptied")
	void clearEmptiesInvocations() {
		recorder.record(new ToolInvocation("t", null, null));
		recorder.clear();

		assertThat(recorder.snapshot()).isEmpty();
	}
}