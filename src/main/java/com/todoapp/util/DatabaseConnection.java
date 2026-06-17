package com.todoapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mengelola koneksi ke database SQLite dan inisialisasi tabel.
 */
public class DatabaseConnection {

    private static final String DATA_DIR = "data";
    private static final String DB_NAME = "todo.db";
    private static final String DB_URL = "jdbc:sqlite:" + DATA_DIR + "/" + DB_NAME;

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                ensureDataDirectoryExists();
                connection = DriverManager.getConnection(DB_URL);
                initializeSchema(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal terkoneksi ke database: " + e.getMessage(), e);
        }
        return connection;
    }

    private static void ensureDataDirectoryExists() {
        try {
            Path dataPath = Paths.get(DATA_DIR);
            Path attachmentsPath = Paths.get(DATA_DIR, "attachments");
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
            if (!Files.exists(attachmentsPath)) {
                Files.createDirectories(attachmentsPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal membuat folder data: " + e.getMessage(), e);
        }
    }

    private static void initializeSchema(Connection conn) throws SQLException {
        String createTaskTable = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                deadline TEXT,
                priority TEXT NOT NULL DEFAULT 'MEDIUM',
                status TEXT NOT NULL DEFAULT 'PENDING',
                category TEXT,
                attachment_path TEXT,
                attachment_name TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            );
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTaskTable);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Gagal menutup koneksi database: " + e.getMessage());
        }
    }
}
