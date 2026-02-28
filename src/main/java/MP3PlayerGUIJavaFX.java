import containers.*;
import db.DataBaseManager;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import music_player.MusicPlayerAccess;
import music_player.MusicPlayerModel;
import net.yetihafen.javafx.customcaption.CustomCaption;
import services.SongService;
import util.ImageCache;

/**
 * Main application class for the MP3 Player.
 * This class serves as the entry point and orchestrates the UI containers.
 *
 * Architecture:
 * - Uses a BorderPane layout with three main containers:
 *   - TopContainer: Menu bar for file operations
 *   - CenterContainer: Home page and now playing views
 *   - BottomContainer: Playback controls and song info
 *
 * The class implements Observer to receive updates from the music player model
 * and update the UI accordingly.
 */
public class MP3PlayerGUIJavaFX extends Application implements Observer {

    // Update interval for GUI refresh (in seconds)
    private static final double DEFAULT_UPDATE_DURATION = 0.1;

    // Music player model
    private MusicPlayerModel musicPlayer;

    // UI Containers
    private BottomContainer bottomContainer; // Playback controls
    private CenterContainer centerContainer; // Main content area
    private TopContainer topContainer; // Menu bar

    // State
    private List<File> playlist; // Current playlist

    // JavaFX components
    private Stage window; // Main application window
    private Scene scene; // Main scene
    private BorderPane layout; // Root layout container

    private SongService songService;

    // Animation state for now playing view
    private boolean isOpen = false;

    // ==================== Application Lifecycle ====================

    /**
     * Initializes the music player model before the UI is created.
     * This is called before start() by the JavaFX framework.
     */
    @Override
    public void init() throws Exception {
        this.musicPlayer = new MusicPlayerModel();
        this.musicPlayer.addObserver(this);

        DataBaseManager.getInstance().initialize();
        this.songService = new SongService();
        this.songService.deletedFiles();
    }

    /**
     * Creates and displays the main application window.
     * Sets up all UI containers and starts the update timer.
     *
     * @param stage The primary stage provided by JavaFX
     * @throws Exception If initialization fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Create root layout
        layout = new BorderPane();

        // Configure main window
        window = stage;
        window.setTitle("No Song Selected ~ Another MP3 Player");
        window.getIcons().add(ImageCache.getImage("amp3p.png"));
        window.setOnCloseRequest(e -> {
            System.out.println("Closing");
            window.close();
        });

        // Initialize all UI containers
        initContainers();

        loadPersistedLibraryOnStartup();

        // Start periodic GUI updates for song progress
        KeyFrame updater = new KeyFrame(
            Duration.seconds(DEFAULT_UPDATE_DURATION),
            e -> notifyGUI()
        );
        Timeline t = new Timeline(updater);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        // Assemble the layout
        this.layout.setBottom(this.bottomContainer.getBottomLayout());
        this.bottomContainer.getBottomLayout().setVisible(false); // Hidden until song loaded
        this.layout.setTop(this.topContainer.getMenuBar());
        this.layout.setCenter(
            this.centerContainer.getHomeNowPlayingContainer()
        );

        // Create and configure scene
        scene = new Scene(this.layout, 1260, 720);
        scene.getStylesheets().add("Default_Theme.css");

        // Display window
        window.setScene(scene);
        window.show();

        // Apply custom window caption (Windows 11 style)
        CustomCaption.setImmersiveDarkMode(this.window, true);

        // Initialize now playing view state (hidden by default)
        this.centerContainer.getNowPlayingWrapper().setVisible(false);
        this.centerContainer.getNowPlayingWrapper().setMouseTransparent(true);
        Platform.runLater(() -> {
            this.centerContainer.getNowPlayingWrapper().setTranslateY(2000);
        });
    }

    private void shutdown() {}

    // ==================== Container Initialization ====================

    /**
     * Creates and configures all UI containers.
     * Order matters due to dependencies between containers.
     *
     * @throws Exception If container initialization fails
     */
    private void initContainers() throws Exception {
        // Initialize center container first (needed by bottom container for toggle callback)
        centerContainer = new CenterContainer(
            (MusicPlayerAccess) musicPlayer,
            this::onPlaylistLoaded,
            this::onSongSelectedFromQueue,
            this::onPlayPauseToggle,
            this::loadQueueView
        );
        centerContainer.initialize(layout);

        // Initialize top container (menu bar)
        topContainer = new TopContainer(
            (MusicPlayerAccess) musicPlayer,
            window,
            this::onSongLoaded,
            this::onPlaylistLoaded,
            this::loadPlaylistSong,
            this::loadQueueView
        );
        topContainer.initialize();

        // Initialize bottom container (playback controls)
        bottomContainer = new BottomContainer(
            (MusicPlayerAccess) musicPlayer,
            this::toggleView,
            this::loadPlaylistSong,
            this::updateSongLabels,
            this::onPrevSong
        );
        bottomContainer.initialize(centerContainer.getNowPlayingWrapper());

        // Set up callbacks for center container UI updates
        centerContainer.setUpdateCallbacks(
            this::updateVolumeSlider,
            this::updateSongSlider,
            this::updateSongLabels
        );
        centerContainer.setupHomePageLoadButton(
            this::multipleFileSelectionAndFill
        );
    }

    // ==================== Event Handlers ====================

    /**
     * Handles loading a single song file.
     * Updates the window title and all UI elements.
     *
     * @param songFile The song file to load
     */
    private void onSongLoaded(File songFile) {
        // Stop current playback if any
        if (this.musicPlayer.hasClip() && this.musicPlayer.isRunning()) {
            this.musicPlayer.stop();
        }
        this.musicPlayer.changeSong(songFile);

        // Update UI if song loaded successfully
        if (this.musicPlayer.hasClip()) {
            window.setTitle(songFile.getName() + " ~ Another MP3 Player");
            updateAllUI();
            centerContainer.updateQueueSelection();
        }
    }

    /**
     * Handles playlist loading.
     * Stores the playlist and sets it on the music player.
     *
     * @param playlist The list of songs to load
     */
    private void onPlaylistLoaded(List<File> playlist) {
        try {
            this.playlist = this.songService.mergeAndStore(playlist);
        } catch (SQLException e) {
            e.printStackTrace();
            this.playlist = playlist;
        }

        this.musicPlayer.setPlaylist(this.playlist);
        this.centerContainer.fillTheHomePage(
            this.playlist,
            this::setWindowTitle,
            () -> {
                updateAllUI();
                ImageCache.setButtonImage(
                    bottomContainer.getPlayPauseButton(),
                    "new-pause.png"
                );
            }
        );
        loadQueueView();
    }

    /**
     * Handles song selection from the queue view.
     * Updates window title and all UI elements.
     *
     * @param song The selected song file
     */
    private void onSongSelectedFromQueue(File song) {
        window.setTitle(song.getName() + " ~ Another MP3 Player");
        updateAllUI();
    }

    /**
     * Handles loading the previous song.
     * Updates window title and all UI elements.
     *
     * @param song The previous song file
     */
    private void onPrevSong(File song) {
        window.setTitle(song.getName() + " ~ Another MP3 Player");
        updateAllUI();
        centerContainer.updateQueueSelection();
    }

    /**
     * Handles play/pause toggle from the album cover click.
     */
    private void onPlayPauseToggle() {
        bottomContainer.updatePlayPauseButton();
    }

    // ==================== Playback Control ====================

    /**
     * Loads and plays the next song in the playlist.
     * Handles the transition between songs including UI updates.
     */
    private void loadPlaylistSong() {
        File song;

        // Get next song - either from current playback or start of playlist
        if (this.musicPlayer.hasClip() && this.musicPlayer.isRunning()) {
            this.musicPlayer.stop();
            song = this.musicPlayer.loadNextSong();
        } else {
            song = this.musicPlayer.initPlaylist();
        }

        // Update UI and start playback if song loaded
        if (song != null && this.musicPlayer.hasClip()) {
            window.setTitle(song.getName() + " ~ Another MP3 Player");
            updateAllUI();
            centerContainer.updateQueueSelection();
            this.musicPlayer.start();
            ImageCache.setButtonImage(
                bottomContainer.getPlayPauseButton(),
                "new-pause.png"
            );
        }
    }

    // ==================== View Management ====================

    /**
     * Loads the queue view with current playlist and sets up selection handler.
     */
    private void loadQueueView() {
        centerContainer.loadQueueView(playlist);
        centerContainer.setupQueueSelectionHandler(
            playlist,
            this::setWindowTitle,
            bottomContainer.getPlayPauseButton()
        );
    }

    /**
     * Opens file selection dialog and populates home page with selected songs.
     */
    private void multipleFileSelectionAndFill() {
        List<File> selectedFiles = topContainer.multipleFileSelection();
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        try {
            this.playlist = this.songService.mergeAndStore(selectedFiles);
        } catch (SQLException e) {
            e.printStackTrace();
            this.playlist = new ArrayList<>(selectedFiles);
        }

        this.musicPlayer.setPlaylist(this.playlist);
        centerContainer.getMainPage().getChildren().clear();
        centerContainer.fillTheHomePage(
            this.playlist,
            this::setWindowTitle,
            () -> {
                updateAllUI();
                ImageCache.setButtonImage(
                    bottomContainer.getPlayPauseButton(),
                    "new-pause.png"
                );
            }
        );
        loadQueueView();
    }

    // ==================== UI Update Methods ====================

    /**
     * Updates all UI elements (volume slider, song slider, song labels).
     * Consolidates the repeated pattern of calling all three update methods.
     */
    private void updateAllUI() {
        updateVolumeSlider();
        updateSongSlider();
        updateSongLabels();
    }

    /**
     * Updates the volume slider to match current settings.
     */
    private void updateVolumeSlider() {
        if (bottomContainer != null) {
            bottomContainer.updateVolumeSlider();
        }
    }

    /**
     * Updates the song progress slider for current song length.
     */
    private void updateSongSlider() {
        if (bottomContainer != null) {
            bottomContainer.updateSongSlider();
        }
    }

    /**
     * Updates song title, artist, and album cover displays.
     */
    private void updateSongLabels() {
        if (bottomContainer != null) {
            bottomContainer.updateSongLabels();
        }
        if (centerContainer != null) {
            centerContainer.updateSongCover();
        }
    }

    /**
     * Sets the window title.
     *
     * @param title The new window title
     */
    private void setWindowTitle(String title) {
        window.setTitle(title);
    }

    /**
     * Triggers the music player to notify observers of changes.
     */
    private void notifyGUI() {
        this.musicPlayer.announceChanges();
    }

    // ==================== Animation ====================

    /**
     * Toggles the visibility of a view with slide animation.
     * Used to show/hide the now playing view.
     *
     * @param viewToAnimate The region to animate
     */
    private void toggleView(Region viewToAnimate) {
        boolean opening = !isOpen;

        // Add view to container if opening
        if (opening) {
            StackPane container = centerContainer.getHomeNowPlayingContainer();
            if (!container.getChildren().contains(viewToAnimate)) {
                container.getChildren().add(viewToAnimate);
            }
            viewToAnimate.setVisible(true);
            viewToAnimate.setMouseTransparent(false);
            viewToAnimate.setManaged(true);
        }

        // Calculate animation end position
        double endValue = opening ? 0 : this.layout.getHeight();

        // Create slide animation
        Timeline timeline = new Timeline();

        KeyValue kv = new KeyValue(
            viewToAnimate.translateYProperty(),
            endValue,
            Interpolator.EASE_BOTH
        );

        KeyFrame kf = new KeyFrame(Duration.millis(150), kv);
        timeline.getKeyFrames().addAll(kf);

        // Handle animation completion
        timeline.setOnFinished(event -> {
            if (!opening) {
                viewToAnimate.setVisible(false);
                viewToAnimate.setMouseTransparent(true);
                viewToAnimate.setManaged(false);
                centerContainer
                    .getHomeNowPlayingContainer()
                    .getChildren()
                    .remove(viewToAnimate);
            }
        });

        timeline.play();
        isOpen = opening;
    }

    // ==================== Observer Implementation ====================

    /**
     * Called when the music player model changes.
     * Updates play/pause button, visibility, and song progress.
     *
     * @param o   The observable object
     * @param arg Optional argument (not used)
     */
    /**
     * Loads persisted songs from the database on startup and displays them on the home page.
     * If no songs exist, the home page remains empty until user loads files.
     */
    private void loadPersistedLibraryOnStartup() {
        try {
            List<File> persistedFiles =
                this.songService.getExistingSongFilesFromDb();
            if (persistedFiles == null || persistedFiles.isEmpty()) {
                return;
            }

            this.playlist = persistedFiles;
            this.musicPlayer.setPlaylist(this.playlist);

            centerContainer.fillTheHomePage(
                this.playlist,
                this::setWindowTitle,
                this::updateAllUI
            );
            loadQueueView();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        // Update play/pause button icon
        if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
            ImageCache.setButtonImage(
                bottomContainer.getPlayPauseButton(),
                "new-pause.png"
            );
        } else {
            ImageCache.setButtonImage(
                bottomContainer.getPlayPauseButton(),
                "new-play.png"
            );
        }

        // Handle song loaded state
        if (this.musicPlayer.hasClip()) {
            this.bottomContainer.getBottomLayout().setVisible(true);

            // Auto-advance to next song if current song ended
            if (this.musicPlayer.atEnd()) {
                if (this.musicPlayer.hasPlaylist()) {
                    loadPlaylistSong();
                } else {
                    this.bottomContainer.getBottomLayout().setVisible(false);
                }
            }

            // Update song progress if not being dragged
            if (!bottomContainer.isDragging()) {
                bottomContainer
                    .getSongSlider()
                    .setValue(this.musicPlayer.getClipCurrentValue());
            }

            // Keep synced lyrics highlight in lockstep with playback progress
            centerContainer.updateLyricsHighlight();
        }
    }
}
