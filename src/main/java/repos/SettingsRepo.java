package repos;

import db.DataBaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SettingsRepo handles persistent storage of application-wide user preferences.
 * It uses the 'settings' table in the SQLite database to store key-value pairs.
 */
public class SettingsRepo {

    private static SettingsRepo instance;

    private SettingsRepo() {}

    public static synchronized SettingsRepo getInstance() {
        if (instance == null) {
            instance = new SettingsRepo();
        }
        return instance;
    }

    /**
     * Retrieves a setting value by its key.
     * 
     * @param key          The unique identifier for the setting
     * @param defaultValue The value to return if the key is not found
     * @return The stored value, or defaultValue if not present
     */
    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (
            Connection conn = DataBaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    /**
     * Persists or updates a setting in the database.
     * 
     * @param key   The unique identifier for the setting
     * @param value The value to store (converted to String)
     */
    public void saveSetting(String key, String value) {
        String sql = "INSERT INTO settings(key, value) VALUES(?, ?) " +
                     "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (
            Connection conn = DataBaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to get boolean settings.
     */
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String val = getSetting(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    /**
     * Convenience method to save boolean settings.
     */
    public void saveBooleanSetting(String key, boolean value) {
        saveSetting(key, String.valueOf(value));
    }
}
