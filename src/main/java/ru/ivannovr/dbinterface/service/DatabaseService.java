package ru.ivannovr.dbinterface.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.database.DatabaseConnection;
import ru.ivannovr.dbinterface.model.ForeignKey;
import ru.ivannovr.dbinterface.model.TableSchema;
import ru.ivannovr.dbinterface.utils.RecordValidator;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class DatabaseService {
    protected static final Logger logger = LogManager.getLogger(DatabaseService.class);
    protected final DatabaseConnection connection;

    protected DatabaseService(DatabaseConnection connection) {
        this.connection = connection;
    }

    public List<TableSchema> getTables() throws SQLException {
        List<TableSchema> tables = new ArrayList<>();
        try (Connection conn = connection.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                TableSchema table = new TableSchema(tableName);

                // Получение столбцов, автоинкремента и NOT NULL ограничений
                try (ResultSet columns = conn.getMetaData().getColumns(null, getSchema(), tableName, null)) {
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                        String isNullable = columns.getString("IS_NULLABLE");
                        boolean autoIncrement = "YES".equalsIgnoreCase(isAutoIncrement);
                        boolean notNull = "NO".equalsIgnoreCase(isNullable);
                        table.addColumn(columnName, autoIncrement, notNull);
                    }
                }

                // Получение внешних ключей, ссылающихся на эту таблицу
                try (ResultSet fkRs = conn.getMetaData().getExportedKeys(null, getSchema(), tableName)) {
                    while (fkRs.next()) {
                        String childTable = fkRs.getString("FKTABLE_NAME");
                        String childColumn = fkRs.getString("FKCOLUMN_NAME");
                        String parentColumn = fkRs.getString("PKCOLUMN_NAME");
                        table.addForeignKey(childTable, childColumn, parentColumn);
                    }
                }

                tables.add(table);
            }
            logger.info("Retrieved {} tables from database ({})", tables.size(), connection.getDatabaseType().getDisplayName());
            return tables;
        }
    }

    public ObservableList<ObservableList<String>> getTableData(String tableName) throws SQLException {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        String query = "SELECT * FROM " + escapeIdentifier(tableName);
        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }
            logger.info("Retrieved {} rows for table {} ({})", data.size(), tableName, connection.getDatabaseType().getDisplayName());
            return data;
        }
    }

    public ObservableList<ObservableList<String>> searchTableData(String tableName, String searchText) throws SQLException {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        TableSchema table = getTables().stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Table not found: " + tableName));

        List<String> columns = table.getColumns();
        String query = "SELECT * FROM " + escapeIdentifier(tableName) + " WHERE " +
                columns.stream()
                        .map(col -> escapeIdentifier(col) + " LIKE ?")
                        .collect(Collectors.joining(" OR "));
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 1; i <= columns.size(); i++) {
                stmt.setString(i, "%" + searchText + "%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getString(i));
                    }
                    data.add(row);
                }
            }
            logger.debug("Search query executed for table {}: {}, found {} rows ({})", tableName, query, data.size(), connection.getDatabaseType().getDisplayName());
            return data;
        }
    }

    public void addRecord(String tableName, List<String> columns, List<String> values) throws SQLException {
        TableSchema table = getTables().stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Table not found: " + tableName));

        RecordValidator.validateRecord(tableName, columns, values, table.getNotNullColumns());

        String query = "INSERT INTO " + escapeIdentifier(tableName) + " (" +
                columns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", ")) +
                ") VALUES (" +
                columns.stream().map(col -> "?").collect(Collectors.joining(", ")) +
                ")";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                stmt.setString(i + 1, value != null && !value.trim().isEmpty() ? value : null);
            }
            logger.debug("Executing insert query: {}, Values: {} ({})", query, values, connection.getDatabaseType().getDisplayName());
            stmt.executeUpdate();
        }
    }

    public void updateRecord(String tableName, List<String> columns, List<String> newValues, List<String> oldValues) throws SQLException {
        TableSchema table = getTables().stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Table not found: " + tableName));

        RecordValidator.validateRecord(tableName, columns, newValues, table.getNotNullColumns());

        List<String> nonAutoColumns = new ArrayList<>();
        List<String> nonAutoNewValues = new ArrayList<>();
        RecordValidator.filterNonAutoIncrement(table, columns, newValues, nonAutoColumns, nonAutoNewValues);

        String query = "UPDATE " + escapeIdentifier(tableName) + " SET " +
                nonAutoColumns.stream()
                        .map(col -> escapeIdentifier(col) + " = ?")
                        .collect(Collectors.joining(", ")) +
                " WHERE " +
                columns.stream()
                        .map(col -> escapeIdentifier(col) + " = ?")
                        .collect(Collectors.joining(" AND "));
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < nonAutoNewValues.size(); i++) {
                String value = nonAutoNewValues.get(i);
                stmt.setString(i + 1, value != null && !value.trim().isEmpty() ? value : null);
            }
            for (int i = 0; i < oldValues.size(); i++) {
                stmt.setString(nonAutoNewValues.size() + i + 1, oldValues.get(i));
            }
            logger.debug("Executing update query: {}, Values: {} ({})", query, newValues, connection.getDatabaseType().getDisplayName());
            stmt.executeUpdate();
        }
    }

    public Map<String, List<ObservableList<String>>> findRelatedRecords(String tableName, List<String> columns, List<String> values) throws SQLException {
        Map<String, List<ObservableList<String>>> relatedRecords = new HashMap<>();
        Set<String> visitedTables = new HashSet<>();
        findRelatedRecordsRecursive(tableName, columns, values, relatedRecords, visitedTables);
        return relatedRecords;
    }

    protected void findRelatedRecordsRecursive(String tableName, List<String> columns, List<String> values,
                                               Map<String, List<ObservableList<String>>> relatedRecords,
                                               Set<String> visitedTables) throws SQLException {
        if (visitedTables.contains(tableName)) {
            logger.debug("Skipping table {} to avoid cyclic dependency ({})", tableName, connection.getDatabaseType().getDisplayName());
            return;
        }
        visitedTables.add(tableName);

        TableSchema table = getTables().stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Table not found: " + tableName));

        for (ForeignKey fk : table.getForeignKeys()) {
            String childTable = fk.getChildTable();
            String childColumn = fk.getChildColumn();
            String parentColumn = fk.getParentColumn();

            int parentColumnIndex = table.getColumns().indexOf(parentColumn);
            if (parentColumnIndex == -1) {
                logger.warn("Parent column {} not found in table {} ({})", parentColumn, tableName, connection.getDatabaseType().getDisplayName());
                continue;
            }

            List<ObservableList<String>> childRecords = findChildRecords(childTable, childColumn, values.get(parentColumnIndex));
            if (!childRecords.isEmpty()) {
                relatedRecords.computeIfAbsent(childTable, k -> new ArrayList<>()).addAll(childRecords);
                logger.debug("Found {} related records in table {} for {} = {} ({})", childRecords.size(), childTable, parentColumn, values.get(parentColumnIndex), connection.getDatabaseType().getDisplayName());
                processChildRecords(childTable, childRecords, relatedRecords, visitedTables);
            }
        }

        visitedTables.remove(tableName);
    }

    private List<ObservableList<String>> findChildRecords(String childTable, String childColumn, String parentValue) throws SQLException {
        List<ObservableList<String>> childRecords = new ArrayList<>();
        String query = "SELECT * FROM " + escapeIdentifier(childTable) + " WHERE " + escapeIdentifier(childColumn) + " = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parentValue);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getString(i));
                    }
                    childRecords.add(row);
                }
            }
        }
        return childRecords;
    }

    private void processChildRecords(String childTable, List<ObservableList<String>> childRecords,
                                     Map<String, List<ObservableList<String>>> relatedRecords,
                                     Set<String> visitedTables) throws SQLException {
        TableSchema childTableSchema = getTables().stream()
                .filter(t -> t.getName().equals(childTable))
                .findFirst()
                .orElseThrow(() -> new SQLException("Child table not found: " + childTable));

        for (ObservableList<String> childRecord : childRecords) {
            findRelatedRecordsRecursive(childTable, childTableSchema.getColumns(), childRecord, relatedRecords, visitedTables);
        }
    }

    public void deleteRecord(String tableName, List<String> columns, List<String> values) throws SQLException {
        String query = "DELETE FROM " + escapeIdentifier(tableName) + " WHERE " +
                columns.stream()
                        .map(col -> escapeIdentifier(col) + " = ?")
                        .collect(Collectors.joining(" AND "));
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i + 1, values.get(i));
            }
            logger.debug("Executing delete query: {}, Values: {} ({})", query, values, connection.getDatabaseType().getDisplayName());
            stmt.executeUpdate();
        }
    }

    public void cascadeDelete(String tableName, List<String> columns, List<String> values) throws SQLException {
        Map<String, List<ObservableList<String>>> relatedRecords = findRelatedRecords(tableName, columns, values);
        List<Map.Entry<String, ObservableList<String>>> deletionOrder = new ArrayList<>();
        collectDeletionOrder(tableName, columns, values, relatedRecords, deletionOrder, new HashSet<>());

        for (Map.Entry<String, ObservableList<String>> entry : deletionOrder) {
            String childTable = entry.getKey();
            ObservableList<String> childRecord = entry.getValue();
            TableSchema childTableSchema = getTables().stream()
                    .filter(t -> t.getName().equals(childTable))
                    .findFirst()
                    .orElseThrow(() -> new SQLException("Child table not found: " + childTable));
            deleteRecord(childTable, childTableSchema.getColumns(), childRecord);
        }

        deleteRecord(tableName, columns, values);
        logger.info("Cascaded delete completed for table {}, values: {} ({})", tableName, values, connection.getDatabaseType().getDisplayName());
    }

    protected void collectDeletionOrder(String tableName, List<String> columns, List<String> values,
                                        Map<String, List<ObservableList<String>>> relatedRecords,
                                        List<Map.Entry<String, ObservableList<String>>> deletionOrder,
                                        Set<String> visitedTables) throws SQLException {
        if (visitedTables.contains(tableName)) {
            return;
        }
        visitedTables.add(tableName);

        TableSchema table = getTables().stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Table not found: " + tableName));

        for (ForeignKey fk : table.getForeignKeys()) {
            String childTable = fk.getChildTable();
            List<ObservableList<String>> childRecords = relatedRecords.getOrDefault(childTable, new ArrayList<>());
            TableSchema childTableSchema = getTables().stream()
                    .filter(t -> t.getName().equals(childTable))
                    .findFirst()
                    .orElseThrow(() -> new SQLException("Child table not found: " + childTable));

            for (ObservableList<String> childRecord : childRecords) {
                collectDeletionOrder(childTable, childTableSchema.getColumns(), childRecord, relatedRecords, deletionOrder, visitedTables);
                deletionOrder.add(new AbstractMap.SimpleEntry<>(childTable, childRecord));
            }
        }

        visitedTables.remove(tableName);
    }

    protected abstract String escapeIdentifier(String identifier);

    protected String getSchema() {
        return null; // По умолчанию схема не используется
    }
}