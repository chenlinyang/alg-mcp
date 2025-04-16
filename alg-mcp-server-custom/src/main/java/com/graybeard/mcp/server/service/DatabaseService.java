package com.graybeard.mcp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graybeard.mcp.server.mcp.annotation.McpPrompt;
import com.graybeard.mcp.server.mcp.exception.McpException;
import com.graybeard.mcp.server.mcp.annotation.McpTool;
import com.graybeard.mcp.server.mcp.annotation.McpParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class DatabaseService {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Resource
    private DataSource dataSource;

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @McpTool(description = "Execute a SELECT query on the jdbc database")
    public String read_query(@McpParam(description = "SELECT SQL query to execute") String query) {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    if (value != null) {
                        row.put(columnName, value.toString());
                    } else {
                        row.put(columnName, null);
                    }
                }
                results.add(row);
            }
            return mapper.writeValueAsString(results);

        } catch (Exception e) {
            throw new McpException("Query execution failed: " + e.getMessage(), e);
        }
    }

    @McpTool(description = "Execute a INSERT, UPDATE or DELETE query on the jdbc database")
    public String write_query(@McpParam(description = "INSERT, UPDATE or DELETE SQL query to execute") String query) {
        if (query.strip().toUpperCase().startsWith("SELECT")) {
            throw new McpException("SELECT queries are not allowed for write_query");
        }

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            return "Query executed successfully";
        } catch (Exception e) {
            throw new McpException("Query execution failed: " + e.getMessage(), e);
        }
    }

    @McpTool(description = "List all tables in the jdbc database")
    public String list_tables() {
        log.debug("Listing tables");
        log.error("Listing tables");
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE" });

            List<Map<String, String>> tables = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> table = new HashMap<>();
                table.put("TABLE_CAT", rs.getString("TABLE_CAT"));
                table.put("TABLE_SCHEM", rs.getString("TABLE_SCHEM"));
                table.put("TABLE_NAME", rs.getString("TABLE_NAME"));
                tables.add(table);
            }
            return mapper.writeValueAsString(tables);
        } catch (Exception e) {
            throw new McpException("Failed to list tables: " + e.getMessage(), e);
        }
    }

    @McpTool(description = "Create new table in the jdbc database")
    public String create_table(@McpParam(description = "CREATE TABLE SQL statement") String query) {
        if (!query.strip().toUpperCase().startsWith("CREATE TABLE")) {
            throw new McpException("Only CREATE TABLE statements are allowed");
        }
        return write_query(query);
    }

    @McpTool(description = "Describe table")
    public String describe_table(@McpParam(description = "Catalog name", required = false) String catalog,
            @McpParam(description = "Schema name", required = false) String schema,
            @McpParam(description = "Table name") String table) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(catalog, schema, table, null);

            List<Map<String, String>> columns = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                column.put("TYPE_NAME", rs.getString("TYPE_NAME"));
                column.put("COLUMN_SIZE", rs.getString("COLUMN_SIZE"));
                column.put("NULLABLE", rs.getString("IS_NULLABLE"));
                column.put("REMARKS", rs.getString("REMARKS"));
                column.put("COLUMN_DEF", rs.getString("COLUMN_DEF"));
                columns.add(column);
            }
            return mapper.writeValueAsString(columns);
        } catch (Exception e) {
            throw new McpException("Failed to describe table: " + e.getMessage());
        }
    }

    @McpTool(description = "Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.")
    public String database_info() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, String> info = new HashMap<>();

            info.put("database_product_name", metaData.getDatabaseProductName());
            info.put("database_product_version", metaData.getDatabaseProductVersion());
            info.put("driver_name", metaData.getDriverName());
            info.put("driver_version", metaData.getDriverVersion());
            //info.put("url", metaData.getURL());
            //info.put("username", metaData.getUserName());
            info.put("max_connections", String.valueOf(metaData.getMaxConnections()));
            info.put("read_only", String.valueOf(metaData.isReadOnly()));
            info.put("supports_transactions", String.valueOf(metaData.supportsTransactions()));
            info.put("sql_keywords", metaData.getSQLKeywords());

            return mapper.writeValueAsString(info);
        } catch (Exception e) {
            throw new McpException("Failed to get database info: " + e.getMessage(), e);
        }
    }

    @McpPrompt(description = "Get SQL dialect about the database.")
    public String getSqlDialect() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return metaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @McpPrompt(description = "Get structure about the database.")
    public String generateDDL() {
        StringBuilder ddl = new StringBuilder();

        try (Connection conn = getConnection()) {
            String catalog = conn.getCatalog();
            DatabaseMetaData metaData = conn.getMetaData();

            ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String createTableStatement = generateCreateTableStatement(catalog, tableName, metaData);
                ddl.append(createTableStatement).append("\n");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ddl.toString();
    }

    private String generateCreateTableStatement(String catalog, String tableName, DatabaseMetaData metaData) {
        StringBuilder createTableStatement = new StringBuilder();

        try {
            ResultSet columns = metaData.getColumns(catalog, null, tableName, null);
            ResultSet pk = metaData.getPrimaryKeys(catalog, null, tableName);
            ResultSet fks = metaData.getImportedKeys(catalog, null, tableName);

            String primaryKeyColumn = "";
            if (pk.next()) {
                primaryKeyColumn = pk.getString("COLUMN_NAME");
            }

            createTableStatement
                    .append("CREATE TABLE ")
                    .append(tableName)
                    .append(" (\n");

            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int size = columns.getInt("COLUMN_SIZE");
                String nullable = columns.getString("IS_NULLABLE").equals("YES") ? " NULL" : " NOT NULL";
                String columnDef = columns.getString("COLUMN_DEF") != null ? " DEFAULT " + columns.getString("COLUMN_DEF") : "";
                String comment = columns.getString("REMARKS");

                createTableStatement
                        .append("  ")
                        .append(columnName)
                        .append(" ")
                        .append(columnType)
                        .append("(")
                        .append(size)
                        .append(")")
                        .append(nullable)
                        .append(columnDef);

                if (columnName.equals(primaryKeyColumn)) {
                    createTableStatement.append(" PRIMARY KEY");
                }

                createTableStatement.append(",\n");

                if (comment != null && !comment.isEmpty()) {
                    createTableStatement
                            .append("  COMMENT ON COLUMN ")
                            .append(tableName)
                            .append(".")
                            .append(columnName)
                            .append(" IS '")
                            .append(comment)
                            .append("',\n");
                }
            }

            while (fks.next()) {
                String fkColumnName = fks.getString("FKCOLUMN_NAME");
                String pkTableName = fks.getString("PKTABLE_NAME");
                String pkColumnName = fks.getString("PKCOLUMN_NAME");
                createTableStatement
                        .append("  FOREIGN KEY (")
                        .append(fkColumnName)
                        .append(") REFERENCES ")
                        .append(pkTableName)
                        .append("(")
                        .append(pkColumnName)
                        .append("),\n");
            }

            if (createTableStatement.charAt(createTableStatement.length() - 2) == ',') {
                createTableStatement.delete(createTableStatement.length() - 2, createTableStatement.length());
            }

            createTableStatement.append(");\n");

            ResultSet tableRemarks = metaData.getTables(null, null, tableName, null);
            if (tableRemarks.next()) {
                String tableComment = tableRemarks.getString("REMARKS");
                if (tableComment != null && !tableComment.isEmpty()) {
                    createTableStatement
                            .append("COMMENT ON TABLE ")
                            .append(tableName)
                            .append(" IS '")
                            .append(tableComment)
                            .append("';\n");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return createTableStatement.toString();
    }

}
