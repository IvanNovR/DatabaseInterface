package ru.ivannovr.dbinterface.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.model.TableSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecordValidator {
    private static final Logger logger = LogManager.getLogger(RecordValidator.class);

    public static void validateRecord(String tableName, List<String> columns, List<String> values, List<String> notNullColumns) throws SQLException {
        if (columns.size() != values.size()) {
            logger.warn("Mismatch between columns and values for table {}: {} columns, {} values", tableName, columns.size(), values.size());
            throw new SQLException("Mismatch between columns and values: " + columns.size() + " columns, " + values.size() + " values");
        }

        List<String> missingFields = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            String value = values.get(i);
            if (notNullColumns.contains(column) && (value == null || value.trim().isEmpty())) {
                missingFields.add(column);
            }
        }

        if (!missingFields.isEmpty()) {
            logger.warn("Missing required fields for table {}: {}", tableName, missingFields);
            throw new SQLException("Missing required fields: " + String.join(", ", missingFields));
        }
    }

    public static void filterNonAutoIncrement(TableSchema table, List<String> columns, List<String> values,
                                              List<String> nonAutoColumns, List<String> nonAutoValues) {
        List<String> autoIncrementColumns = table.getAutoIncrementColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (!autoIncrementColumns.contains(columns.get(i))) {
                nonAutoColumns.add(columns.get(i));
                nonAutoValues.add(values.get(i));
            }
        }
        logger.debug("Filtered non-auto-increment columns for table {}: {}", table.getName(), nonAutoColumns);
    }
}