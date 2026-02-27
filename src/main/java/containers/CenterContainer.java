package containers;

import eu.iamgio.animated.binding.Animated;
import eu.iamgio.animated.binding.presets.AnimatedScale;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import music_player.MusicPlayerAccess;
import queue.QueueCell;
import queue.QueueItem;
import queue.QueueSongData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * CenterContainer manages the main content area of the MP3 player UI.
 * This includes:
 * - Home page with song grid display
 * - Now Playing view with album cover and queue
 * - Animated transitions between views
 * 
 * The container uses a StackPane to layer the home page and now playing views,
 * allowing smooth animated transitions between them.
 */
public class CenterContainer {

    // Main layout containers
    private GridPane coverQueueContainer;       // Now playing view with cover and queue
    private ListView<QueueItem> queue;          // List view showing upcoming songs
    private ImageView songCover;                // Album artwork display
    private TilePane mainPage;                  // Home page with song grid
    private StackPane homeNowPlayingContainer;  // Root container stacking both views

    // Dependencies and callbacks
    private final MusicPlayerAccess musicPlayer;               // Interface to music player model
    private final Consumer<List<File>> onPlaylistLoadedCallback; // Callback when playlist is loaded
    private final Consumer<File> onSongSelectedFromQueue;      // Callback when song selected from queue
    private final Runnable onPlayPauseToggle;                  // Callback for play/pause toggle
    private final Runnable loadQueueViewCallback;
    
    // UI update callbacks (set after initialization)
    private Runnable updateVolumeSliderCallback;
    private Runnable updateSongSliderCallback;
    private Runnable updateSongLabelsCallback;

    /**
     * Constructs a new CenterContainer with the required dependencies and callbacks.
     * 
     * @param musicPlayer               Interface to the music player model
     * @param onPlaylistLoadedCallback  Callback when a playlist is loaded
     * @param onSongSelectedFromQueue   Callback when a song is selected from queue
     * @param onPlayPauseToggle         Callback for play/pause toggle from cover click
     */
    public CenterContainer(MusicPlayerAccess musicPlayer,
                           Consumer<List<File>> onPlaylistLoadedCallback,
                           Consumer<File> onSongSelectedFromQueue,
                           Runnable onPlayPauseToggle,
                           Runnable loadQueueView) {
        this.musicPlayer = musicPlayer;
        this.onPlaylistLoadedCallback = onPlaylistLoadedCallback;
        this.onSongSelectedFromQueue = onSongSelectedFromQueue;
        this.onPlayPauseToggle = onPlayPauseToggle;
        this.loadQueueViewCallback = loadQueueView;
    }

    /**
     * Initializes all components of the center container.
     * Must be called after construction before using the container.
     * 
     * @param layout The main BorderPane layout for binding cover dimensions
     * @throws Exception If initialization fails
     */
    public void initialize(BorderPane layout) throws Exception {
        initWindowCenter(layout);
        initHomePage();
        createHomeNowPlayingContainer();
    }

    // ==================== Getters ====================

    public GridPane getCoverQueueContainer() {
        return coverQueueContainer;
    }

    public TilePane getMainPage() {
        return mainPage;
    }

    public StackPane getHomeNowPlayingContainer() {
        return homeNowPlayingContainer;
    }

    public ImageView getSongCover() {
        return songCover;
    }

    public ListView<QueueItem> getQueue() {
        return queue;
    }

    /**
     * Sets the callbacks for UI updates when songs change.
     * 
     * @param updateVolumeSlider Callback to update volume slider
     * @param updateSongSlider   Callback to update song position slider
     * @param updateSongLabels   Callback to update song info labels
     */
    public void setUpdateCallbacks(Runnable updateVolumeSlider, Runnable updateSongSlider, Runnable updateSongLabels) {
        this.updateVolumeSliderCallback = updateVolumeSlider;
        this.updateSongSliderCallback = updateSongSlider;
        this.updateSongLabelsCallback = updateSongLabels;
    }

    // ==================== Queue Management ====================

    /**
     * Loads the queue view with songs from the playlist.
     * Performs initial fill with basic info, then updates with metadata in background.
     * 
     * @param playlist The list of songs to display in the queue
     */
    public void loadQueueView(List<File> playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        // Clear existing items
        this.queue.getItems().clear();

        // Initial fill with file names (fast)
        for (File file : playlist) {
            this.queue.getItems().add(new QueueItem(file, file.getName(), "", "", null));
        }

        // Background task to load metadata for each song
        Task<Void> metadataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < playlist.size(); i++) {
                    File file = playlist.get(i);
                    QueueSongData data = musicPlayer.getQueueData(file);

                    // Create thumbnail for queue item
                    Image img = null;
                    if (data.imageData != null) {
                        img = new Image(new ByteArrayInputStream(data.imageData), 40, 40, true, true);
                    }

                    QueueItem item = new QueueItem(file, data.title, data.artist, data.duration, img);

                    // Update UI on JavaFX thread
                    final int index = i;
                    Platform.runLater(() -> {
                        if (queue.getItems().size() > index) {
                            queue.getItems().set(index, item);
                        }
                    });

                    // Small delay to prevent UI freezing
                    Thread.sleep(10);
                }
                return null;
            }
        };

        // Run as daemon thread so it doesn't prevent app shutdown
        Thread thread = new Thread(metadataTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ==================== Home Page Management ====================

    /**
     * Populates the home page with a grid of songs from the playlist.
     * Each song is displayed with album art, title, and artist.
     * Loading is done in background thread for better performance.
     * 
     * @param playlist        The list of songs to display
     * @param windowTitleSetter Unused parameter (legacy)
     * @param setTitleCallback Callback to set window title when song is selected
     * @param updateUiCallback Callback to update UI when song is selected
     */
    public void fillTheHomePage(List<File> playlist, String windowTitleSetter, Consumer<String> setTitleCallback, Runnable updateUiCallback) {
        if (playlist == null) return;

        // Clear existing content
        this.mainPage.getChildren().clear();
        this.mainPage.setPadding(new Insets(20));

        // Background task to load song metadata and create UI elements
        Task<Void> metadataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < playlist.size(); i++) {
                    File file = playlist.get(i);
                    int index = i;
                    QueueSongData data = musicPlayer.getQueueData(file);

                    // Load album art or use default
                    Image img = null;
                    if (data.imageData != null) {
                        img = new Image(new ByteArrayInputStream(data.imageData), 100, 100, true, true);
                    } else {
                        // Use default album cover
                        FileInputStream inputStream = new FileInputStream("src/main/resources/assets/generic-album-cover.jpeg");
                        img = new Image(new ByteArrayInputStream(inputStream.readAllBytes()), 100, 100, true, true);
                    }

                    // Create song card
                    VBox vbox = new VBox();
                    vbox.setAlignment(Pos.CENTER);
                    vbox.setPadding(new Insets(5, 5, 5, 5));

                    ImageView imageView = new ImageView();
                    imageView.setImage(img);

                    Label title = new Label();
                    title.setText(data.title);

                    Label artist = new Label();
                    artist.setText(data.artist);

                    vbox.getChildren().addAll(imageView, title, artist);

                    // Add click handler and add to UI on JavaFX thread
                    Platform.runLater(() -> {
                        vbox.setOnMouseClicked(event -> {
                            // Stop current song if playing
                            if (musicPlayer.isRunning()) {
                                musicPlayer.stop();
                            }
                            // Load and play selected song
                            musicPlayer.setPlaylistPosition(index);
                            musicPlayer.changeSong(playlist.get(index));

                            // Update window title and UI
                            setTitleCallback.accept(playlist.get(index).getName() + " ~ Another MP3 Player");
                            if (updateUiCallback != null) {
                                updateUiCallback.run();
                            }

                            if(loadQueueViewCallback != null) {
                                loadQueueViewCallback.run();
                            }

                            updateQueueSelection();
                            musicPlayer.start();
                        });
                        mainPage.getChildren().add(vbox);
                    });

                    // Small delay to prevent UI freezing
                    Thread.sleep(10);
                }
                return null;
            }
        };

        Thread thread = new Thread(metadataTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates the album cover image from the current song's metadata.
     */
    public void updateSongCover() {
        ByteArrayInputStream bis = new ByteArrayInputStream(musicPlayer.getSongAlbumImage());
        this.songCover.setImage(new Image(bis, 400, 400, true, true));
    }

    // ==================== Initialization Methods ====================

    /**
     * Initializes the now playing view with album cover and queue.
     * 
     * @param layout The main BorderPane for binding cover dimensions
     * @throws Exception If initialization fails
     */
    private void initWindowCenter(BorderPane layout) throws Exception {
        // Create container for now playing view
        this.coverQueueContainer = new GridPane();
        this.coverQueueContainer.getStyleClass().add("cover-queue-container");
        this.coverQueueContainer.setVisible(false);          // Initially hidden
        this.coverQueueContainer.setMouseTransparent(true);  // Cannot be clicked initially
        this.coverQueueContainer.setManaged(false);          // Not part of layout initially
        this.coverQueueContainer.setTranslateY(10000);       // Positioned off-screen
        this.coverQueueContainer.setVgap(1);
        this.coverQueueContainer.setMaxWidth(Double.MAX_VALUE);
        this.coverQueueContainer.setAlignment(Pos.CENTER);

        // Two columns: cover (70%) and queue (30%)
        ColumnConstraints col1 = new ColumnConstraints(), col2 = new ColumnConstraints();
        col1.setPercentWidth(70);
        col1.setHalignment(HPos.CENTER);
        col2.setPercentWidth(30);
        col2.setHalignment(HPos.RIGHT);
        this.coverQueueContainer.getColumnConstraints().addAll(col1, col2);

        // Initialize album cover with animation wrapper
        setSongCover(layout);
        Animated animatedCover = new Animated(this.songCover, new AnimatedScale());

        // Initialize queue list
        initQueueView();

        // Create queue header with "UP NEXT" label
        Label upNextLabel = new Label("UP NEXT");
        upNextLabel.getStyleClass().add("queue-header-selected");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox queueHeader = new HBox(upNextLabel, spacer);
        queueHeader.setSpacing(20);
        queueHeader.setPadding(new Insets(10));
        queueHeader.setAlignment(Pos.CENTER_LEFT);
        queueHeader.getStyleClass().add("queue-header-container");

        // Combine header and queue list
        VBox queueContainer = new VBox(queueHeader, this.queue);
        VBox.setVgrow(this.queue, Priority.ALWAYS);
        queueContainer.setPadding(new Insets(0, 25, 0, 0));

        // Add cover and queue to grid
        this.coverQueueContainer.add(animatedCover, 0, 0);
        this.coverQueueContainer.add(queueContainer, 1, 0);
    }

    /**
     * Initializes the queue list view with custom cell factory.
     */
    private void initQueueView() {
        this.queue = new ListView<>();
        this.queue.prefHeightProperty().bind(this.songCover.fitHeightProperty());
        this.queue.getStyleClass().add("queue");
        this.queue.setCellFactory(param -> new QueueCell());
    }

    /**
     * Sets up the click handler for queue item selection.
     * Must be called after playlist is loaded.
     * 
     * @param playlist         The current playlist
     * @param setTitleCallback Callback to update window title
     * @param playPauseButton  Reference to play/pause button for icon update
     */
    public void setupQueueSelectionHandler(List<File> playlist, Consumer<String> setTitleCallback, Button playPauseButton) {
        this.queue.setOnMouseClicked(event -> {
            int index = this.queue.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < this.queue.getItems().size() && playlist != null) {
                // Stop current playback
                if (this.musicPlayer.isRunning()) {
                    this.musicPlayer.stop();
                }

                // Load selected song
                this.musicPlayer.setPlaylistPosition(index);
                this.musicPlayer.changeSong(playlist.get(index));

                // Update UI
                setTitleCallback.accept(playlist.get(index).getName() + " ~ Another MP3 Player");
                if (updateVolumeSliderCallback != null) updateVolumeSliderCallback.run();
                if (updateSongSliderCallback != null) updateSongSliderCallback.run();
                if (updateSongLabelsCallback != null) updateSongLabelsCallback.run();

                // Start playback
                this.musicPlayer.start();
                setImage(playPauseButton, "new-pause.png");
            }
        });
    }

    /**
     * Updates the queue selection to highlight the current song based on playlist position.
     * This should be called whenever the song changes automatically.
     */
    public void updateQueueSelection() {
        int position = this.musicPlayer.getPlaylistPosition();
        if (position >= 0 && position < this.queue.getItems().size()) {
            this.queue.getSelectionModel().select(position);
            this.queue.scrollTo(position);
        }
    }

    /**
     * Initializes the album cover image view.
     * Binds size to main layout and adds click handler for play/pause.
     * 
     * @param layout The main BorderPane for size binding
     * @throws Exception If default image cannot be loaded
     */
    private void setSongCover(BorderPane layout) throws Exception {
        // Load default album cover
        FileInputStream inputStream = new FileInputStream("src/main/resources/assets/generic-album-cover.jpeg");
        Image image = new Image(inputStream);
        this.songCover = new ImageView(image);
        this.songCover.setPreserveRatio(true);

        // Bind size to 70% of main layout
        this.songCover.fitWidthProperty().bind(layout.widthProperty().multiply(0.7));
        this.songCover.fitHeightProperty().bind(layout.heightProperty().multiply(0.7));

        // Click on cover to toggle play/pause
        this.songCover.setOnMouseClicked(event -> {
            if (this.musicPlayer.hasClip()) {
                if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
                    this.musicPlayer.stop();
                    if (onPlayPauseToggle != null) onPlayPauseToggle.run();
                } else {
                    this.musicPlayer.start();
                    if (onPlayPauseToggle != null) onPlayPauseToggle.run();
                }
            }
        });
    }

    /**
     * Initializes the home page container.
     */
    private void initHomePage() {
        this.mainPage = new TilePane();
        this.mainPage.setAlignment(Pos.CENTER);
        this.mainPage.setPrefColumns(4);
        this.mainPage.setHgap(20);
        this.mainPage.setVgap(20);
    }

    /**
     * Sets up the "Load Songs" button on the home page.
     * 
     * @param multipleFileSelectionCallback Callback to open file selection dialog
     */
    public void setupHomePageLoadButton(Runnable multipleFileSelectionCallback) {
        Button loadSongsButton = new Button("Load Songs");
        loadSongsButton.getStyleClass().add("home-page-load-songs-button");
        loadSongsButton.setOnMouseClicked(event -> {
            if (multipleFileSelectionCallback != null) {
                multipleFileSelectionCallback.run();
            }
        });

        this.mainPage.getChildren().add(loadSongsButton);
    }

    /**
     * Creates the main container that stacks home page and now playing views.
     * Allows animated transitions between the two views.
     */
    private void createHomeNowPlayingContainer() {
        this.homeNowPlayingContainer = new StackPane();
        this.homeNowPlayingContainer.setAlignment(Pos.CENTER);

        // Wrap home page in scroll pane for overflow
        ScrollPane scrollPane = new ScrollPane(this.mainPage);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("modern-scroll-pane");

        // Bind home page height to scroll pane
        this.mainPage.prefHeightProperty().bind(scrollPane.heightProperty().subtract(2));

        this.homeNowPlayingContainer.getChildren().add(scrollPane);
    }

    /**
     * Sets the icon image for a button from the assets folder.
     * 
     * @param b        The button to set the image on
     * @param fileName The name of the image file in /assets/
     */
    private void setImage(ButtonBase b, String fileName) {
        String resourcePath = "/assets/" + fileName;
        var inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }

        Image image = new Image(inputStream);
        b.setGraphic(new ImageView(image));
    }
}
