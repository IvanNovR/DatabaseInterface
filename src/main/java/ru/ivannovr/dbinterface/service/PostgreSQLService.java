package ru.ivannovr.dbinterface.service;

import ru.ivannovr.dbinterface.database.DatabaseConnection;

public class PostgreSQLService extends DatabaseService {
    public PostgreSQLService(DatabaseConnection connection) {
        super(connection);
    }

    @Override
    protected String escapeIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    protected String getSchema() {
        return "public";
    }
}