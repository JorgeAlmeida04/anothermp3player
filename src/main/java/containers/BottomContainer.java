package containers;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import music_player.MusicPlayerAccess;
import util.ImageCache;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * BottomContainer manages the bottom section of the MP3 player UI.
 * This includes:
 * - Playback controls (play/pause, next, previous)
 * - Song progress slider
 * - Volume slider
 * - Song title and artist display
 * 
 * The container uses a GridPane layout with two rows:
 * - Row 0: Song slider/progress bar
 * - Row 1: Playback controls, song info, and volume slider
 */
public class BottomContainer {

    // Main layout containers
    private GridPane bottomLayout;      // Root container for the bottom section
    private GridPane playbackBox;       // Container for playback controls and song info

    // Playback control buttons
    private Button playPauseButton;     // Toggles between play and pause states
    private Button nextButton;          // Skips to next song in playlist
    private Button prevButton;          // Returns to previous song in playlist

    // Volume control components
    private Slider volumeSlider;        // Draggable volume control (0-100)
    private ProgressBar volumeBar;      // Visual progress indicator for volume
    private StackPane volumeSliderBar;  // Container stacking progress bar and slider

    // Song progress components
    private Slider songSlider;          // Draggable song position control
    private ProgressBar songBar;        // Visual progress indicator for song position
    private StackPane songSliderBar;    // Container stacking progress bar and slider
    private boolean isDragging = false; // Flag to track if user is dragging the song slider

    // Song information display
    private Label songTitle;            // Displays current song title
    private Label songArtist;           // Displays current song artist
    private VBox labelVBox;             // Container for title and artist labels

    // Dependencies and callbacks
    private final MusicPlayerAccess musicPlayer;           // Interface to music player model
    private final Consumer<Region> toggleViewCallback;     // Callback to toggle now playing view
    private final Runnable loadPlaylistSongCallback;       // Callback to load next playlist song
    private final Runnable updateSongLabelsCallback;       // Callback to update song labels
    private final Consumer<File> onPrevSongCallback;       // Callback when previous song is loaded

    /**
     * Constructs a new BottomContainer with the required dependencies and callbacks.
     * 
     * @param musicPlayer              Interface to the music player model
     * @param toggleViewCallback       Callback to toggle the now playing view visibility
     * @param loadPlaylistSongCallback Callback to load the next song in playlist
     * @param updateSongLabelsCallback Callback to update song information labels
     * @param onPrevSongCallback       Callback when a previous song is loaded
     */
    public BottomContainer(MusicPlayerAccess musicPlayer,
                           Consumer<Region> toggleViewCallback,
                           Runnable loadPlaylistSongCallback,
                           Runnable updateSongLabelsCallback,
                           Consumer<File> onPrevSongCallback) {
        this.musicPlayer = musicPlayer;
        this.toggleViewCallback = toggleViewCallback;
        this.loadPlaylistSongCallback = loadPlaylistSongCallback;
        this.updateSongLabelsCallback = updateSongLabelsCallback;
        this.onPrevSongCallback = onPrevSongCallback;
    }

    /**
     * Initializes the bottom container layout.
     * Must be called after construction before using the container.
     * 
     * @param coverQueueContainer The region to toggle visibility for when clicking bottom layout
     */
    public void initialize(Region coverQueueContainer) {
        initBottomLayout(coverQueueContainer);
    }

    // ==================== Getters ====================

    public GridPane getBottomLayout() {
        return this.bottomLayout;
    }

    public Button getPlayPauseButton() {
        return playPauseButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Button getPrevButton() {
        return prevButton;
    }

    public Slider getVolumeSlider() {
        return volumeSlider;
    }

    public Slider getSongSlider() {
        return songSlider;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    // ==================== Update Methods ====================

    /**
     * Resets the volume slider to default values.
     * Sets range to 0-100 with initial value from the music player.
     */
    public void updateVolumeSlider() {
        this.volumeSlider.setMin(0);
        this.volumeSlider.setMax(100);
        this.volumeSlider.setValue(this.musicPlayer.getCurrentVolume());
    }

    /**
     * Updates the song slider to match the current song's length.
     * Resets position to 0.
     */
    public void updateSongSlider() {
        this.songSlider.setMax(this.musicPlayer.getClipLength());
        this.songSlider.setMin(0);
        this.songSlider.setValue(0);
    }

    /**
     * Updates the song title and artist labels from the music player.
     */
    public void updateSongLabels() {
        this.songTitle.setText(this.musicPlayer.getSongTitle());
        this.songArtist.setText(this.musicPlayer.getSongArtist());
    }

    /**
     * Updates the play/pause button icon based on current playback state.
     * Shows pause icon when playing, play icon when stopped/paused.
     */
    public void updatePlayPauseButton() {
        if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
            ImageCache.setButtonImage(playPauseButton, "new-pause.png");
        } else {
            ImageCache.setButtonImage(playPauseButton, "new-play.png");
        }
    }

    /**
     * Updates the song slider position to match current playback position.
     * Only updates if the user is not currently dragging the slider.
     */
    public void updateSongSliderPosition() {
        if (!this.isDragging) {
            this.songSlider.setValue(this.musicPlayer.getClipCurrentValue());
        }
    }

    // ==================== Initialization Methods ====================

    /**
     * Initializes the main bottom layout container.
     * Creates a two-row grid with song slider on top and controls below.
     * 
     * @param coverQueueContainer The region to show/hide when clicking this layout
     */
    private void initBottomLayout(Region coverQueueContainer) {
        // Create main container with styling
        this.bottomLayout = new GridPane();
        this.bottomLayout.getStyleClass().add("bottom-layout");
        this.bottomLayout.setHgap(1);
        this.bottomLayout.setMaxWidth(Double.MAX_VALUE);
        this.bottomLayout.setAlignment(Pos.CENTER);
        this.bottomLayout.setPadding(new Insets(0, 5, 5, 5));

        // Configure column to fill available width
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        column1.setFillWidth(true);
        this.bottomLayout.getColumnConstraints().addAll(column1);

        // Initialize playback controls container
        this.playbackBox = new GridPane();
        this.playbackBox.setVgap(2);
        this.playbackBox.setMaxWidth(Double.MAX_VALUE);
        this.playbackBox.setAlignment(Pos.CENTER);
        this.playbackBox.setPadding(new Insets(5));

        // Configure three columns: left (buttons), center (song info), right (volume)
        ColumnConstraints col1 = new ColumnConstraints(), col2 = new ColumnConstraints(), col3 = new ColumnConstraints();
        col1.setPercentWidth(25);
        col1.setHalignment(HPos.LEFT);      // Buttons aligned left
        col2.setPercentWidth(50);
        col2.setHalignment(HPos.CENTER);    // Song info centered
        col3.setPercentWidth(25);
        col3.setHalignment(HPos.RIGHT);     // Volume aligned right
        playbackBox.getColumnConstraints().addAll(col1, col2, col3);

        // Initialize all sub-components
        addPlaybackButtons();
        initSongSlider();
        initVolumeSlider();
        initSongInfoVisualizer();

        // Add components to playback box
        this.playbackBox.add(labelVBox, 1, 0);
        this.playbackBox.add(volumeSliderBar, 2, 0);

        // Add rows to main layout
        this.bottomLayout.add(songSliderBar, 0, 0);
        this.bottomLayout.add(playbackBox, 0, 1);

        // Click anywhere on bottom layout to toggle now playing view
        this.bottomLayout.setOnMouseClicked(event -> {
            if (toggleViewCallback != null) {
                toggleViewCallback.accept(coverQueueContainer);
            }
        });
    }

    /**
     * Creates and configures the playback control buttons.
     * Buttons are: previous, play/pause, next.
     */
    private void addPlaybackButtons() {
        // Play/Pause button - toggles playback state
        playPauseButton = new Button();
        ImageCache.setButtonImage(playPauseButton, "new-play.png");
        playPauseButton.setOnAction(e -> {
            if (this.musicPlayer.hasClip()) {
                if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
                    this.musicPlayer.stop();
                    ImageCache.setButtonImage(playPauseButton, "new-play.png");
                } else {
                    this.musicPlayer.start();
                    ImageCache.setButtonImage(playPauseButton, "new-pause.png");
                }
            }
        });

        // Next button - loads next song in playlist
        nextButton = new Button();
        ImageCache.setButtonImage(nextButton, "next.png");
        nextButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist() && loadPlaylistSongCallback != null) {
                loadPlaylistSongCallback.run();
            }
        });

        // Previous button - loads previous song in playlist
        prevButton = new Button();
        ImageCache.setButtonImage(prevButton, "prev.png");
        prevButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist()) {
                boolean wasRunning = this.musicPlayer.isRunning();
                this.musicPlayer.stop();
                File song = this.musicPlayer.loadPreviousSong();
                if (song != null && onPrevSongCallback != null) {
                    onPrevSongCallback.accept(song);
                    if (wasRunning) {
                        this.musicPlayer.start();
                        ImageCache.setButtonImage(this.playPauseButton, "new-pause.png");
                    }
                }
            }
        });

        // Container for all playback buttons
        FlowPane playbackButtons = new FlowPane();
        playbackButtons.setHgap(5);
        playbackButtons.setAlignment(Pos.CENTER);
        playbackButtons.setPadding(new Insets(5));
        playbackButtons.getChildren().addAll(prevButton, playPauseButton, nextButton);

        playbackBox.add(playbackButtons, 0, 0);
    }

    /**
     * Creates the song information display area.
     * Contains title and artist labels styled with custom fonts.
     */
    private void initSongInfoVisualizer() {
        labelVBox = new VBox();
        labelVBox.setSpacing(10);
        labelVBox.setPadding(new Insets(10, 10, 0, 10));
        labelVBox.setAlignment(Pos.CENTER);

        // Song title with bold font
        songTitle = new Label("Song Title");
        songTitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.REGULAR, 25));

        // Song artist with semi-bold font
        songArtist = new Label("Song Artist");
        songArtist.setFont(Font.font("Comic Sans MS", FontWeight.SEMI_BOLD, FontPosture.REGULAR, 16));

        labelVBox.getChildren().addAll(songTitle, songArtist);
    }

    /**
     * Creates and configures the volume slider with progress bar.
     * Range: 0-100, initial value: 10.
     */
    private void initVolumeSlider() {
        // Progress bar shows visual fill level
        this.volumeBar = new ProgressBar(0);
        this.volumeBar.getStyleClass().add("volume-bar");
        this.volumeBar.setMaxWidth(Double.MAX_VALUE);

        // Slider allows user control
        this.volumeSlider = new Slider(0, 100, 10);
        this.volumeSlider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        this.volumeSlider.setMaxHeight(80);
        this.volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.musicPlayer.volumeChange(newValue.doubleValue());
        });
        this.volumeSlider.getStyleClass().add("volume-slider");

        // Show percentage popup on hover
        setupSliderPopup(this.volumeSlider, value -> String.format("%.0f%%", value));

        // Bind progress bar to slider value
        this.volumeBar.progressProperty().bind(
                this.volumeSlider.valueProperty().divide(this.volumeSlider.maxProperty())
        );

        // Stack progress bar behind slider
        this.volumeSliderBar = new StackPane();
        this.volumeSliderBar.getChildren().addAll(this.volumeBar, this.volumeSlider);
        this.volumeSliderBar.setAlignment(Pos.CENTER);
    }

    /**
     * Creates and configures the song progress slider with progress bar.
     * Shows current playback position and allows seeking.
     */
    private void initSongSlider() {
        // Progress bar shows visual playback position
        this.songBar = new ProgressBar(0.0);
        this.songBar.getStyleClass().add("song-bar");
        this.songBar.setMaxWidth(Double.MAX_VALUE);

        // Slider allows user to seek
        this.songSlider = new Slider(0, 100, 0);
        this.songSlider.setShowTickMarks(false);
        this.songSlider.setShowTickLabels(false);
        this.songSlider.setMajorTickUnit(60 * 44231.5636364);  // Tick unit for time display
        this.songSlider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        this.songSlider.getStyleClass().add("song-slider");

        // Show time popup on hover (converts frames to minutes:seconds)
        setupSliderPopup(this.songSlider, hoverValue -> {
            if (this.songSlider.getMax() > 0) {
                double timeUnit = 44231.5636364;  // Frames per second conversion factor
                int minutes = (int) (hoverValue / timeUnit) / 60;
                int seconds = (int) (hoverValue / timeUnit) % 60;
                return String.format("%d:%02d", minutes, seconds);
            }
            return "0:00";
        });

        // Track when user is dragging to prevent auto-update
        this.songSlider.setOnMousePressed(event -> {
            this.isDragging = true;
        });

        // Seek to position when user releases slider
        this.songSlider.setOnMouseReleased(event -> {
            this.musicPlayer.setSongPosition(((int) this.songSlider.getValue()));
            this.isDragging = false;
        });

        // Update progress bar when slider value changes
        this.songSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.songSlider.getMax() > 0) {
                this.songBar.setProgress(newValue.doubleValue() / this.songSlider.getMax());
            } else {
                this.songBar.setProgress(0);
            }
        });

        // Update progress bar when song length changes
        this.songSlider.maxProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                this.songBar.setProgress(this.songSlider.getValue() / newValue.doubleValue());
            } else {
                this.songBar.setProgress(0);
            }
        });

        // Stack progress bar behind slider
        this.songSliderBar = new StackPane();
        this.songSliderBar.getChildren().addAll(songBar, songSlider);
        this.songSliderBar.setAlignment(Pos.CENTER);

        // Bind progress bar width to container
        this.songBar.prefWidthProperty().bind(this.songSliderBar.widthProperty());
    }

    /**
     * Sets up a popup tooltip that follows the mouse cursor over a slider.
     * Shows formatted value based on the provided formatter function.
     * 
     * @param slider    The slider to attach the popup to
     * @param formatter Function to convert slider value to display string
     */
    private void setupSliderPopup(Slider slider, Function<Double, String> formatter) {
        Label label = new Label();
        Popup popup = new Popup();
        popup.getContent().add(label);

        // Update popup content and position on mouse move
        slider.setOnMouseMoved(event -> {
            double mouseX = event.getX();
            double totalWidth = slider.getWidth();

            if (totalWidth <= 0) return;

            // Calculate value at mouse position
            double percentage = Math.max(0, Math.min(1, mouseX / totalWidth));

            double min = slider.getMin();
            double max = slider.getMax();
            double hoverValue = min + (percentage * (max - min));

            label.setText(formatter.apply(hoverValue));

            // Position popup near cursor
            popup.setAnchorX(event.getScreenX() + 10);
            popup.setAnchorY(event.getScreenY() - 30);
        });

        // Show popup on mouse enter
        slider.setOnMouseEntered(event -> popup.show(slider, event.getScreenX() + 10, event.getScreenY() - 30));
        // Hide popup on mouse exit
        slider.setOnMouseExited(event -> popup.hide());
    }
}
