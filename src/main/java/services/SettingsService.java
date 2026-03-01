package services;

import repos.SettingsRepo;
import java.util.prefs.Preferences;

/**
 * SettingsService provides a unified API for all application settings.
 * It abstracts the underlying storage (SQLite vs Preferences API).
 */
public class SettingsService {

    private final SettingsRepo dbRepo;
    private final Preferences windowPrefs;
    private final Preferences audioPrefs;

    public SettingsService() {
        this.dbRepo = SettingsRepo.getInstance();
        // Use string paths instead of class literals to avoid illegal imports from the default package
        this.windowPrefs = Preferences.userRoot().node("anothermp3player/ui/window");
        this.audioPrefs = Preferences.userRoot().node("anothermp3player/audio");
    }

    // --- Window Settings (Preferences API) ---

    public double getWindowX(double defaultValue) { return windowPrefs.getDouble("win_x", defaultValue); }
    public double getWindowY(double defaultValue) { return windowPrefs.getDouble("win_y", defaultValue); }
    public double getWindowWidth(double defaultValue) { return windowPrefs.getDouble("win_w", defaultValue); }
    public double getWindowHeight(double defaultValue) { return windowPrefs.getDouble("win_h", defaultValue); }

    public void saveWindowBounds(double x, double y, double w, double h) {
        windowPrefs.putDouble("win_x", x);
        windowPrefs.putDouble("win_y", y);
        windowPrefs.putDouble("win_w", w);
        windowPrefs.putDouble("win_h", h);
    }

    // --- Audio Settings (Preferences API) ---

    public double getVolume(double defaultValue) {
        return audioPrefs.getDouble("volume", defaultValue);
    }

    public void saveVolume(double volume) {
        audioPrefs.putDouble("volume", volume);
    }

    // --- Playback Settings (Database) ---

    public boolean isShuffle(boolean defaultValue) {
        return dbRepo.getBooleanSetting("shuffle", defaultValue);
    }

    public void saveShuffle(boolean shuffle) {
        dbRepo.saveBooleanSetting("shuffle", shuffle);
    }

    public boolean isRepeat(boolean defaultValue) {
        return dbRepo.getBooleanSetting("repeat", defaultValue);
    }

    public void saveRepeat(boolean repeat) {
        dbRepo.saveBooleanSetting("repeat", repeat);
    }

    // --- UI Settings (Database) ---

    public boolean isGridView(boolean defaultValue) {
        return dbRepo.getBooleanSetting("view_mode_grid", defaultValue);
    }

    public void saveGridView(boolean isGrid) {
        dbRepo.saveBooleanSetting("view_mode_grid", isGrid);
    }
}
