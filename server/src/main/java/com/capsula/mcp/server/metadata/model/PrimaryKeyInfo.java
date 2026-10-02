package com.capsula.mcp.server.metadata.model;

import java.util.List;

public record PrimaryKeyInfo(String schema, String table, String pkName, List<String> columns) {
}

