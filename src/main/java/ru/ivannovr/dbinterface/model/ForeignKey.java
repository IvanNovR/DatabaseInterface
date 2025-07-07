package ru.ivannovr.dbinterface.model;

public class ForeignKey {
    private final String childTable;
    private final String childColumn;
    private final String parentColumn;

    public ForeignKey(String childTable, String childColumn, String parentColumn) {
        this.childTable = childTable;
        this.childColumn = childColumn;
        this.parentColumn = parentColumn;
    }

    public String getChildTable() {
        return childTable;
    }

    public String getChildColumn() {
        return childColumn;
    }

    public String getParentColumn() {
        return parentColumn;
    }
}
