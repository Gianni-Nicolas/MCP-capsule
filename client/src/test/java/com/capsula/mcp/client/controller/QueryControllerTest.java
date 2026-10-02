package com.capsula.mcp.client.controller;

import com.capsula.mcp.client.dto.QueryRequest;
import com.capsula.mcp.client.dto.QueryResponse;
import com.capsula.mcp.client.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryController")
class QueryControllerTest {

	@Mock
	private QueryService queryService;

	@InjectMocks
	private QueryController controller;

	@Test
	@DisplayName("given a request when query is called then it delegates to QueryService and returns its response")
	void delegatesToService() {
		QueryRequest request = new QueryRequest("clientes con tarjeta");
		QueryResponse expected = new QueryResponse("SELECT 1");
		when(queryService.generate(request)).thenReturn(expected);

		QueryResponse actual = controller.query(request);

		assertThat(actual).isSameAs(expected);
		verify(queryService).generate(request);
	}
}