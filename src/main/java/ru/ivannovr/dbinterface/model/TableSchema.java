package ru.ivannovr.dbinterface.model;

import java.util.ArrayList;
import java.util.List;

public class TableSchema {
    private final String name;
    private final List<String> columns;
    private final List<String> autoIncrementColumns;
    private final List<String> notNullColumns;
    private final List<ForeignKey> foreignKeys; // Ссылки на эту таблицу

    public TableSchema(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
        this.autoIncrementColumns = new ArrayList<>();
        this.notNullColumns = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<String> getAutoIncrementColumns() {
        return autoIncrementColumns;
    }

    public List<String> getNotNullColumns() {
        return notNullColumns;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void addColumn(String column, boolean isAutoIncrement, boolean isNotNull) {
        columns.add(column);
        if (isAutoIncrement) {
            autoIncrementColumns.add(column);
        }
        if (isNotNull) {
            notNullColumns.add(column);
        }
    }

    public void addForeignKey(String childTable, String childColumn, String parentColumn) {
        foreignKeys.add(new ForeignKey(childTable, childColumn, parentColumn));
    }

    @Override
    public String toString() {
        return name;
    }
}