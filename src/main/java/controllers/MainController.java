package controllers;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.Stage;
import music_player.MusicPlayerModel;
import services.SettingsService;
import services.SongService;
import containers.BottomContainer;
import containers.CenterContainer;
import containers.TopContainer;

/**
 * MainController coordinates the interaction between the Model and the UI.
 * It handles event listeners and persistence, removing this bloat from the main Application class.
 */
public class MainController {

    private final MusicPlayerModel model;
    private final SettingsService settingsService;
    private final SongService songService;
    
    private Stage window;
    private BottomContainer bottomContainer;
    private CenterContainer centerContainer;
    private TopContainer topContainer;
    
    // Debounced window bounds saver to reduce I/O during resize
    private javafx.animation.PauseTransition windowBoundsSaveDebouncer;

    public MainController(MusicPlayerModel model) {
        this.model = model;
        this.settingsService = new SettingsService();
        this.songService = new SongService();
    }

    public void initialize(Stage window, BottomContainer bottom, CenterContainer center, TopContainer top) {
        this.window = window;
        this.bottomContainer = bottom;
        this.centerContainer = center;
        this.topContainer = top;

        // Set up View Mode persistence for center container
        this.centerContainer.setOnViewModeChanged(isGrid -> settingsService.saveGridView(isGrid));
        this.centerContainer.setGridView(settingsService.isGridView(true));

        setupModelListeners();
        loadInitialLibrary(); // Load songs first
        applyPersistentSettings(); // Then apply shuffle/repeat to those songs
    }

    private void setupModelListeners() {
        model.addPropertyChangeListener(evt -> Platform.runLater(() -> {
            String propertyName = evt.getPropertyName();

            switch (propertyName) {
                case MusicPlayerModel.EVENT_TRACK_CHANGED -> {
                    bottomContainer.updatePlayPauseButton();
                    bottomContainer.updateSongLabels();
                    bottomContainer.updateSongSlider();
                    centerContainer.updateSongCover();
                    centerContainer.updateQueueSelection();
                    File songFile = (File) evt.getNewValue();
                    if (songFile != null) window.setTitle(songFile.getName() + " ~ Another MP3 Player");
                }
                case MusicPlayerModel.EVENT_PLAYBACK_STATE -> bottomContainer.updatePlayPauseButton();
                case MusicPlayerModel.EVENT_PLAYLIST_CHANGED -> {
                    centerContainer.loadQueueView(model.getPlaylist());
                    centerContainer.setupQueueSelectionHandler(model.getPlaylist());
                }
                case MusicPlayerModel.EVENT_PLAYLIST_POSITION_CHANGED -> centerContainer.updateQueueSelection();
                case "shuffle" -> {
                    settingsService.saveShuffle((boolean) evt.getNewValue());
                    bottomContainer.updateShuffleButtonStyle();
                }
                case "repeat" -> {
                    settingsService.saveRepeat((boolean) evt.getNewValue());
                    bottomContainer.updateRepeatButtonStyle();
                }
                case MusicPlayerModel.EVENT_VOLUME_CHANGED -> settingsService.saveVolume((double) evt.getNewValue());
                case MusicPlayerModel.EVENT_PLAYER_STATE -> handlePeriodicUpdate();
            }
        }));
    }

    private void handlePeriodicUpdate() {
        if (model.hasClip()) {
            bottomContainer.getBottomLayout().setVisible(true);
            bottomContainer.updatePlayPauseButton();

            if (model.atEnd()) {
                if (model.isRepeat()) model.rewindToStart();
                else if (model.hasPlaylist()) loadNextSong();
                else bottomContainer.getBottomLayout().setVisible(false);
            }

            if (!bottomContainer.isDragging()) {
                bottomContainer.updateSongSliderPosition();
            }
            centerContainer.updateLyricsHighlight();
        }
    }

    private void applyPersistentSettings() {
        // Window
        double x = settingsService.getWindowX(-1);
        double y = settingsService.getWindowY(-1);
        double w = settingsService.getWindowWidth(1150);
        double h = settingsService.getWindowHeight(797);
        if (x != -1 && y != -1) {
            window.setX(x);
            window.setY(y);
        }
        window.setWidth(w);
        window.setHeight(h);

        // Initialize debouncer for window bounds saves (reduces I/O during resize)
        windowBoundsSaveDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        windowBoundsSaveDebouncer.setOnFinished(e -> saveWindowBounds());
        
        window.xProperty().addListener((o, old, val) -> debouncedSaveWindowBounds());
        window.yProperty().addListener((o, old, val) -> debouncedSaveWindowBounds());
        window.widthProperty().addListener((o, old, val) -> debouncedSaveWindowBounds());
        window.heightProperty().addListener((o, old, val) -> debouncedSaveWindowBounds());

        // Model
        model.setShuffle(settingsService.isShuffle(false));
        model.setRepeat(settingsService.isRepeat(false));
        model.volumeChange(settingsService.getVolume(50.0));

        // UI
        bottomContainer.updateShuffleButtonStyle();
        bottomContainer.updateRepeatButtonStyle();
        bottomContainer.updateVolumeSlider();
    }
    
    private void debouncedSaveWindowBounds() {
        // Restart the debouncer timer - saves will only happen after 500ms of no changes
        if (windowBoundsSaveDebouncer != null) {
            windowBoundsSaveDebouncer.playFromStart();
        }
    }

    private void saveWindowBounds() {
        settingsService.saveWindowBounds(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    }

    private void loadInitialLibrary() {
        try {
            List<File> persisted = songService.getExistingSongFilesFromDb();
            if (persisted != null && !persisted.isEmpty()) {
                model.setPlaylist(persisted);
                centerContainer.fillTheHomePage(persisted);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Actions called from UI ---

    public void loadNextSong() {
        model.loadNextSong();
        if (model.hasClip()) model.start();
    }

    public void onPlaylistLoaded(List<File> files) {
        try {
            List<File> playlist = songService.mergeAndStore(files);
            model.setPlaylist(playlist);
            centerContainer.fillTheHomePage(playlist);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
