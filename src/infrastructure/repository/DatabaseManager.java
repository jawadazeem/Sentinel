package infrastructure.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // The singleton instance of DB
    private static DatabaseManager instance;
    private Connection connection;
    private final String URL = "jdbc:sqlite:sentinel.db";

    private DatabaseManager() {
        try {
            this.connection = DriverManager.getConnection(URL);
            initializeTables();
            System.out.println("Sentinel Database Initialized.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create Device Inventory Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS devices (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    battery_level REAL DEFAULT 100.0
                )
            """);

            // Create Alarm Log Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alarm_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    device_type TEXT,
                    device_id TEXT,
                    alarm_status TEXT,
                    alarm_severity TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create User Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(100) PRIMARY KEY,
                    password_hash TEXT NOT NULL
                )
            """);

            // Create Receivers Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subscribers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    status TEXT DEFAULT 'ACTIVE'
                )
            """);
        }
    }
}
