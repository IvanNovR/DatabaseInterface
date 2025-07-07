package ru.ivannovr.dbinterface.service;

import ru.ivannovr.dbinterface.database.DatabaseConnection;

public class MSSQLService extends DatabaseService {
    public MSSQLService(DatabaseConnection connection) {
        super(connection);
    }

    @Override
    protected String escapeIdentifier(String identifier) {
        return "[" + identifier + "]";
    }
}