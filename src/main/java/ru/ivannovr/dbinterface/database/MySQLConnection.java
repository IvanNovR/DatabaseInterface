package ru.ivannovr.dbinterface.database;

import ru.ivannovr.dbinterface.model.DatabaseType;

public class MySQLConnection extends DatabaseConnection {
    public MySQLConnection(String server, String database, String username, String password) {
        super(String.format("jdbc:mysql://%s/%s?useSSL=false&serverTimezone=UTC",
                server, database), username, password);
        logger.info("MySQLConnection initialized for server: {}, database: {}", server, database);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }
}