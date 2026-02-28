package db;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseManager {

    private static final String DB_NAME = "mp3player.db";
    private static DataBaseManager instance;
    private Connection connection;

    private DataBaseManager() {}

    public static synchronized DataBaseManager getInstance() {
        if(instance == null) {
            instance = new DataBaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Path dbPath = Paths.get(
                    System.getProperty("user.home"),
                    ".mp3player",
                    DB_NAME
            );
            //Ensure directory exists
            dbPath.getParent().toFile().mkdirs();

            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);

            // Fail fast if something went wrong
            if (connection == null) {
                throw new SQLException(
                        "Failed to create connection. " +
                                "Is sqlite-jdbc on the classpath?"
                );
            }

            //Enable WAL mode for better concurrent performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }
        }
        return connection;
    }

    public void initialize() throws SQLException {
        Connection conn = getConnection();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_path   TEXT    NOT NULL UNIQUE,
                    title       TEXT,
                    artist      TEXT,
                    album       TEXT,
                    genre       TEXT,
                    duration_ms INTEGER DEFAULT 0,
                    year        INTEGER,
                    date_added  TEXT    DEFAULT (datetime('now')),
                    last_played TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS playlists (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    name       TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now')),
                    updated_at TEXT DEFAULT (datetime('now'))
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS playlist_songs (
                    playlist_id INTEGER NOT NULL,
                    song_id     INTEGER NOT NULL,
                    position    INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (playlist_id, song_id),
                    FOREIGN KEY (playlist_id)
                        REFERENCES playlists(id) ON DELETE CASCADE,
                    FOREIGN KEY (song_id)
                        REFERENCES songs(id) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                );
            """);

            // Indexes for common queries
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_songs_artist
                    ON songs(artist);
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_songs_album
                    ON songs(album);
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_playlist_songs_pos
                    ON playlist_songs(playlist_id, position);
            """);
        }
    }

    public void shutdown() {
        try {
            if(connection != null && !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
