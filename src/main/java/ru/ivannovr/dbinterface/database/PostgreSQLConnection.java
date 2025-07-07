package ru.ivannovr.dbinterface.database;

import ru.ivannovr.dbinterface.model.DatabaseType;

public class PostgreSQLConnection extends DatabaseConnection {
    public PostgreSQLConnection(String server, String database, String username, String password) {
        super(String.format("jdbc:postgresql://%s/%s",
                server, database), username, password);
        logger.info("PostgreSQLConnection initialized for server: {}, database: {}", server, database);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }
}