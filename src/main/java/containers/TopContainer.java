package containers;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import music_player.MusicPlayerAccess;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * TopContainer manages the menu bar at the top of the MP3 player window.
 * This includes:
 * - Songs menu: Load individual songs
 * - Playlist menu: Create and load playlists
 * 
 * The container provides file selection dialogs for loading music files
 * and communicates with the main application through callbacks.
 */
public class TopContainer {

    // Menu bar components
    private MenuBar menuBar;        // Main menu bar container
    private Menu songsMenu;         // Menu for single song operations
    private Menu playlistMenu;      // Menu for playlist operations

    // Menu items
    private MenuItem loadSong;      // Opens file dialog for single song
    private MenuItem loadPlaylist;  // Opens file dialog for multiple songs
    private MenuItem createPlaylist; // Placeholder for playlist creation

    // Dependencies and callbacks
    private final MusicPlayerAccess musicPlayer;           // Interface to music player model
    private final Stage window;                            // Main window for file dialogs
    private final Consumer<File> onSongLoaded;             // Callback when single song is loaded
    private final Consumer<List<File>> onPlaylistLoaded;   // Callback when playlist is loaded
    private final Runnable loadPlaylistSongCallback;       // Callback to start playlist playback
    private final Runnable loadQueueViewCallback;          // Callback to update queue view

    /**
     * Constructs a new TopContainer with the required dependencies and callbacks.
     * 
     * @param musicPlayer             Interface to the music player model
     * @param window                  The main stage for positioning file dialogs
     * @param onSongLoaded            Callback when a single song is loaded
     * @param onPlaylistLoaded        Callback when a playlist is loaded
     * @param loadPlaylistSongCallback Callback to start playing the loaded playlist
     * @param loadQueueViewCallback   Callback to update the queue view display
     */
    public TopContainer(MusicPlayerAccess musicPlayer,
                        Stage window,
                        Consumer<File> onSongLoaded,
                        Consumer<List<File>> onPlaylistLoaded,
                        Runnable loadPlaylistSongCallback,
                        Runnable loadQueueViewCallback) {
        this.musicPlayer = musicPlayer;
        this.window = window;
        this.onSongLoaded = onSongLoaded;
        this.onPlaylistLoaded = onPlaylistLoaded;
        this.loadPlaylistSongCallback = loadPlaylistSongCallback;
        this.loadQueueViewCallback = loadQueueViewCallback;
    }

    /**
     * Initializes the menu bar and all menu items.
     * Must be called after construction before using the container.
     */
    public void initialize() {
        menuBar = new MenuBar();
        addMenuBarItems();
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    // ==================== File Selection Methods ====================

    /**
     * Opens a file dialog for selecting multiple MP3 files.
     * Starts in the user's Music directory.
     * 
     * @return List of selected files, or null if cancelled
     */
    public List<File> multipleFileSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select mp3 files");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        return fileChooser.showOpenMultipleDialog(window);
    }

    // ==================== Menu Setup ====================

    /**
     * Creates and configures all menu items and adds them to the menu bar.
     */
    private void addMenuBarItems() {
        // === Songs Menu ===
        songsMenu = new Menu("Songs");

        // Load Song - opens single file selection dialog
        loadSong = new MenuItem("Load Song");
        loadSong.setOnAction(e -> {
            File songFile = fileSelection();
            if (songFile != null) {
                if (onSongLoaded != null) {
                    onSongLoaded.accept(songFile);
                }
                this.musicPlayer.setPlaylistPosition(0);
                this.musicPlayer.start();
            }
        });

        songsMenu.getItems().add(loadSong);

        // === Playlist Menu ===
        playlistMenu = new Menu("Playlist");

        // Create Playlist - placeholder for future functionality
        createPlaylist = new MenuItem("Create Playlist");
        createPlaylist.setOnAction(e -> {
            System.out.println("Creating playlist");
        });

        // Load Playlist - opens multiple file selection dialog
        loadPlaylist = new MenuItem("Load Playlist");
        loadPlaylist.setOnAction(e -> {
            List<File> playlist = multipleFileSelection();
            if (playlist != null && !playlist.isEmpty()) {
                // Notify that playlist was loaded
                if (onPlaylistLoaded != null) {
                    onPlaylistLoaded.accept(playlist);
                }
                // Start playback
                if (loadPlaylistSongCallback != null) {
                    loadPlaylistSongCallback.run();
                }
                // Update queue display
                if (loadQueueViewCallback != null) {
                    loadQueueViewCallback.run();
                }
            }
        });

        playlistMenu.getItems().addAll(createPlaylist, loadPlaylist);

        // Add menus to menu bar
        menuBar.getMenus().addAll(songsMenu, playlistMenu);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Opens a file dialog for selecting a single MP3 file.
     * Starts in the user's Music directory.
     * 
     * @return Selected file, or null if cancelled
     */
    private File fileSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a mp3 file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        return fileChooser.showOpenDialog(window);
    }
}
