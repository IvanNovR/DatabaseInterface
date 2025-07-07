package ru.ivannovr.dbinterface.model;

import ru.ivannovr.dbinterface.database.DatabaseConnection;
import ru.ivannovr.dbinterface.database.MSSQLConnection;
import ru.ivannovr.dbinterface.database.MySQLConnection;
import ru.ivannovr.dbinterface.database.PostgreSQLConnection;
import ru.ivannovr.dbinterface.service.DatabaseService;
import ru.ivannovr.dbinterface.service.MSSQLService;
import ru.ivannovr.dbinterface.service.MySQLService;
import ru.ivannovr.dbinterface.service.PostgreSQLService;

public enum DatabaseType {
    MSSQL("MSSQL") {
        @Override
        public DatabaseConnection createConnection(String server, String database, String username, String password) {
            return new MSSQLConnection(server, database, username, password);
        }

        @Override
        public DatabaseService createDatabaseService(DatabaseConnection connection) {
            return new MSSQLService(connection);
        }
    },
    MYSQL("MySQL") {
        @Override
        public DatabaseConnection createConnection(String server, String database, String username, String password) {
            return new MySQLConnection(server, database, username, password);
        }

        @Override
        public DatabaseService createDatabaseService(DatabaseConnection connection) {
            return new MySQLService(connection);
        }
    },
    POSTGRESQL("PostgreSQL") {
        @Override
        public DatabaseConnection createConnection(String server, String database, String username, String password) {
            return new PostgreSQLConnection(server, database, username, password);
        }

        @Override
        public DatabaseService createDatabaseService(DatabaseConnection connection) {
            return new PostgreSQLService(connection);
        }
    };

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public abstract DatabaseConnection createConnection(String server, String database, String username, String password);

    public abstract DatabaseService createDatabaseService(DatabaseConnection connection);
}