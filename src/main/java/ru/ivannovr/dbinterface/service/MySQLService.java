package ru.ivannovr.dbinterface.service;

import ru.ivannovr.dbinterface.database.DatabaseConnection;

public class MySQLService extends DatabaseService {
    public MySQLService(DatabaseConnection connection) {
        super(connection);
    }

    @Override
    protected String escapeIdentifier(String identifier) {
        return "`" + identifier + "`";
    }
}