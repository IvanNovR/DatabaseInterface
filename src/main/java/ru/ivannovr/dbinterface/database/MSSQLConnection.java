package ru.ivannovr.dbinterface.database;

import ru.ivannovr.dbinterface.model.DatabaseType;

public class MSSQLConnection extends DatabaseConnection {
    public MSSQLConnection(String server, String database, String username, String password) {
        super(String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database), username, password);
        logger.info("MSSQLConnection initialized for server: {}, database: {}", server, database);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MSSQL;
    }
}