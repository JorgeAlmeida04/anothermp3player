package repos;

import db.DataBaseManager;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Song;

public class SongsRepo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss"
    );

    private final DataBaseManager db = DataBaseManager.getInstance();

    public Song insert(Song song) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO songs
                    (file_path, title, artist, album, genre,
                     duration_ms, year)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (
            PreparedStatement ps = db
                .getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, song.getFilePath());
            ps.setString(2, song.getTitle());
            ps.setString(3, song.getArtist());
            ps.setString(4, song.getAlbum());
            ps.setString(5, song.getGenre());
            ps.setLong(6, song.getDurationMs());
            ps.setObject(7, song.getYear());

            int affected = ps.executeUpdate();

            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        song.setId(keys.getInt(1));
                    }
                }
            } else {
                findByPath(song.getFilePath()).ifPresent(existing ->
                    song.setId(existing.getId())
                );
            }
        }
        return song;
    }

    public void insertBatch(List<Song> songs) throws SQLException {
        if (songs == null || songs.isEmpty()) return;

        String sql = """
                INSERT OR IGNORE INTO songs
                    (file_path, title, artist, album, genre,
                     duration_ms, year)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Connection conn = db.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Song s : songs) {
                ps.setString(1, s.getFilePath());
                ps.setString(2, s.getTitle());
                ps.setString(3, s.getArtist());
                ps.setString(4, s.getAlbum());
                ps.setString(5, s.getGenre());
                ps.setLong(6, s.getDurationMs());
                ps.setObject(7, s.getYear());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public List<Song> findAll() throws SQLException {
        String sql = "SELECT * FROM songs ORDER BY artist, album, title";
        try (
            Statement stmt = db.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            return mapResults(rs);
        }
    }

    public Optional<Song> findById(int id) throws SQLException {
        String sql = "SELECT * FROM songs WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Song> results = mapResults(rs);
                return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
            }
        }
    }

    public Optional<Song> findByPath(String filePath) throws SQLException {
        String sql = "SELECT * FROM songs WHERE file_path = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                List<Song> results = mapResults(rs);
                return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
            }
        }
    }

    public List<Song> search(String query) throws SQLException {
        String sql = """
                SELECT * FROM songs
                WHERE title  LIKE ? COLLATE NOCASE
                   OR artist LIKE ? COLLATE NOCASE
                   OR album  LIKE ? COLLATE NOCASE
                ORDER BY artist, album, title
            """;
        String pattern = "%" + query + "%";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return mapResults(rs);
            }
        }
    }

    public List<String> findAllArtists() throws SQLException {
        String sql = """
                SELECT DISTINCT artist FROM songs
                WHERE artist IS NOT NULL
                ORDER BY artist
            """;
        List<String> artists = new ArrayList<>();
        try (
            Statement stmt = db.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                artists.add(rs.getString("artist"));
            }
        }
        return artists;
    }

    public void update(Song song) throws SQLException {
        String sql = """
                UPDATE songs SET
                    title = ?, artist = ?, album = ?, genre = ?,
                    duration_ms = ?, year = ?, last_played = ?
                WHERE id = ?
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, song.getTitle());
            ps.setString(2, song.getArtist());
            ps.setString(3, song.getAlbum());
            ps.setString(4, song.getGenre());
            ps.setLong(5, song.getDurationMs());
            ps.setObject(6, song.getYear());
            ps.setString(
                7,
                song.getLastPlayed() != null
                    ? song.getLastPlayed().format(FMT)
                    : null
            );
            ps.setInt(8, song.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int songId) throws SQLException {
        String sql = "DELETE FROM songs WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, songId);
            ps.executeUpdate();
        }
    }

    /** Remove songs whose files no longer exist on disk */
    public int pruneOrphans() throws SQLException {
        List<Song> all = findAll();
        int removed = 0;
        for (Song song : all) {
            if (!new File(song.getFilePath()).exists()) {
                delete(song.getId());
                removed++;
            }
        }
        return removed;
    }

    public void markPlayed(int songId) throws SQLException {
        String sql = """
                UPDATE songs SET last_played = datetime('now')
                WHERE id = ?
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, songId);
            ps.executeUpdate();
        }
    }

    private List<Song> mapResults(ResultSet rs) throws SQLException {
        List<Song> songs = new ArrayList<>();
        while (rs.next()) {
            Song s = new Song();
            s.setId(rs.getInt("id"));
            s.setFilePath(rs.getString("file_path"));
            s.setTitle(rs.getString("title"));
            s.setArtist(rs.getString("artist"));
            s.setAlbum(rs.getString("album"));
            s.setGenre(rs.getString("genre"));
            s.setDurationMs(rs.getLong("duration_ms"));
            s.setYear((Integer) rs.getObject("year"));

            String added = rs.getString("date_added");
            if (added != null) {
                s.setDateAdded(LocalDateTime.parse(added, FMT));
            }
            String played = rs.getString("last_played");
            if (played != null) {
                s.setLastPlayed(LocalDateTime.parse(played, FMT));
            }
            songs.add(s);
        }
        return songs;
    }
}
