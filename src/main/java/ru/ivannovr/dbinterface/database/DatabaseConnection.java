package ru.ivannovr.dbinterface.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.model.DatabaseType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class DatabaseConnection {
    protected static final Logger logger = LogManager.getLogger(DatabaseConnection.class);
    protected Connection connection;
    protected final String url;
    protected final String username;
    protected final String password;

    protected DatabaseConnection(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        logger.info("DatabaseConnection initialized with URL: {}", url);
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
            logger.debug("Database connection established for {}", getDatabaseType().getDisplayName());
        }
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            logger.debug("Database connection closed for {}", getDatabaseType().getDisplayName());
        }
    }

    public abstract DatabaseType getDatabaseType();
}