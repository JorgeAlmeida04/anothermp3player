package containers;

import eu.iamgio.animated.binding.Animated;
import eu.iamgio.animated.binding.presets.AnimatedScale;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.LyricsTrack;
import music_player.MusicPlayerAccess;
import queue.QueueCell;
import queue.QueueItem;
import queue.QueueSongData;
import util.ImageCache;

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

    // Default album cover loaded once and cached
    private static final Image DEFAULT_ALBUM_COVER;

    static {
        InputStream is = CenterContainer.class.getResourceAsStream(
            "/assets/generic-album-cover.jpeg"
        );
        if (is == null) {
            throw new RuntimeException(
                "Default album cover not found: /assets/generic-album-cover.jpeg"
            );
        }
        DEFAULT_ALBUM_COVER = new Image(is);
    }

    // Main layout containers
    private GridPane coverQueueContainer; // Now playing view with cover and queue
    private StackPane nowPlayingWrapper; // Wrapper with blurred background + coverQueueContainer
    private ImageView blurredBackground; // Blurred album cover background for now playing
    private Rectangle darkOverlay; // Semi-transparent dark overlay for readability
    private ListView<QueueItem> queue; // List view showing upcoming songs
    private ImageView songCover; // Album artwork display
    private TilePane mainPage; // Home page with song grid/list
    private StackPane homeNowPlayingContainer; // Root container stacking both views

    // Now playing right panel tabs (Queue / Lyrics)
    private VBox rightPanelContent;
    private Label queueTabLabel;
    private Label lyricsTabLabel;
    private VBox queueContainerNode;
    private ScrollPane lyricsScrollPane;
    private VBox lyricsContent;
    private Label lyricsEmptyLabel;
    private Label lyricsUnsyncedLabel;
    private boolean showingLyricsTab = false;

    // Lyrics state
    private services.LyricsService lyricsService;
    private model.LyricsTrack currentLyricsTrack;
    private final List<Label> syncedLyricsLineLabels = new ArrayList<>();
    private int currentHighlightedLyricsIndex = -1;
    private Timeline lyricsScrollAnimation;
    private static final double LYRICS_TARGET_VIEWPORT_RATIO = 0.5;
    private static final double LYRICS_SCROLL_ANIMATION_MS = 260;
    private static final double LYRICS_BASE_FONT_SIZE_PX = 14;
    private static final double LYRICS_ACTIVE_FONT_SIZE_PX = 16;
    private static final int LYRICS_CONTEXT_WINDOW = 2;
    private static final String LYRICS_ACTIVE_TEXT_COLOR = "white";
    private static final String LYRICS_NEAR_CONTEXT_TEXT_COLOR = "#a8a8a8";
    private static final String LYRICS_FAR_TEXT_COLOR = "#8a8a8a";
    private static final double LYRICS_ACTIVE_SCALE = 1.06;
    private static final double LYRICS_NEAR_CONTEXT_SCALE = 1.01;
    private static final double LYRICS_NORMAL_SCALE = 1.0;
    private static final double LYRICS_SCALE_ANIMATION_MS = 140;

    // Home page controls/state
    private HBox homeControlsBar;
    private javafx.scene.control.ComboBox<String> sortComboBox;
    private Button viewToggleButton;
    private boolean isGridView = true;
    private static final String SORT_TITLE = "Title (A-Z)";
    private static final String SORT_ARTIST = "Artist (A-Z)";
    private static final String SORT_ALBUM = "Album (A-Z)";

    // Dependencies and callbacks
    private final MusicPlayerAccess musicPlayer; // Interface to music player model
    private final Consumer<List<File>> onPlaylistLoadedCallback; // Callback when playlist is loaded
    private final Consumer<File> onSongSelectedFromQueue; // Callback when song selected from queue
    private final Runnable onPlayPauseToggle; // Callback for play/pause toggle
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
     * @param loadQueueView             Callback to load queue view
     */
    public CenterContainer(
        MusicPlayerAccess musicPlayer,
        Consumer<List<File>> onPlaylistLoadedCallback,
        Consumer<File> onSongSelectedFromQueue,
        Runnable onPlayPauseToggle,
        Runnable loadQueueView
    ) {
        this.musicPlayer = musicPlayer;
        this.onPlaylistLoadedCallback = onPlaylistLoadedCallback;
        this.onSongSelectedFromQueue = onSongSelectedFromQueue;
        this.onPlayPauseToggle = onPlayPauseToggle;
        this.loadQueueViewCallback = loadQueueView;
        this.lyricsService = new services.LyricsService();
    }

    /**
     * Initializes all components of the center container.
     * Must be called after construction before using the container.
     *
     * @param layout The main BorderPane layout for binding cover dimensions
     */
    public void initialize(BorderPane layout) {
        initWindowCenter(layout);
        initBlurredBackground(layout);
        initHomePage();
        createHomeNowPlayingContainer();
    }

    // ==================== Getters ====================

    /**
     * Returns the now playing wrapper (StackPane with blurred background + content).
     * This is the node that should be animated/toggled for the now playing view.
     */
    public StackPane getNowPlayingWrapper() {
        return nowPlayingWrapper;
    }

    /**
     * Returns the inner cover/queue GridPane container.
     * @deprecated Use {@link #getNowPlayingWrapper()} for external references.
     */
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
    public void setUpdateCallbacks(
        Runnable updateVolumeSlider,
        Runnable updateSongSlider,
        Runnable updateSongLabels
    ) {
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
            this.queue.getItems().add(
                new QueueItem(file, file.getName(), "", "", null)
            );
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
                        img = new Image(
                            new ByteArrayInputStream(data.imageData),
                            40,
                            40,
                            true,
                            true
                        );
                    }

                    QueueItem item = new QueueItem(
                        file,
                        data.title,
                        data.artist,
                        data.duration,
                        img
                    );

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
     * @param setTitleCallback Callback to set window title when song is selected
     * @param updateUiCallback Callback to update UI when song is selected
     */
    public void fillTheHomePage(
        List<File> playlist,
        Consumer<String> setTitleCallback,
        Runnable updateUiCallback
    ) {
        if (playlist == null) return;

        // Clear existing content
        this.mainPage.getChildren().clear();
        this.mainPage.setPadding(new Insets(20));

        // Background task to load metadata first, then render sorted/view-mode UI
        Task<List<HomeSongItem>> metadataTask = new Task<>() {
            @Override
            protected List<HomeSongItem> call() throws Exception {
                List<HomeSongItem> items = new ArrayList<>();

                for (int i = 0; i < playlist.size(); i++) {
                    File file = playlist.get(i);
                    QueueSongData data = musicPlayer.getQueueData(file);

                    Image img;
                    if (data.imageData != null) {
                        img = new Image(
                            new ByteArrayInputStream(data.imageData),
                            100,
                            100,
                            true,
                            true
                        );
                    } else {
                        img = DEFAULT_ALBUM_COVER;
                    }

                    items.add(
                        new HomeSongItem(
                            file,
                            data.title,
                            data.artist,
                            data.album,
                            data.duration,
                            img
                        )
                    );

                    Thread.sleep(10);
                }

                return items;
            }
        };

        metadataTask.setOnSucceeded(event -> {
            List<HomeSongItem> items = metadataTask.getValue();
            renderHomePageItems(items, setTitleCallback, updateUiCallback);
        });

        Thread thread = new Thread(metadataTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates the album cover image from the current song's metadata.
     * Also updates the blurred background for the now playing view.
     * Falls back to default cover if no album image is available.
     */
    public void updateSongCover() {
        byte[] albumImage = musicPlayer.getSongAlbumImage();
        Image coverImage;
        if (albumImage != null) {
            coverImage = new Image(
                new ByteArrayInputStream(albumImage),
                400,
                400,
                true,
                true
            );
        } else {
            coverImage = DEFAULT_ALBUM_COVER;
        }
        this.songCover.setImage(coverImage);
        updateBlurredBackground(coverImage);
    }

    /**
     * Updates the blurred background ImageView with the given image.
     * The image is displayed scaled-to-fill with a heavy Gaussian blur.
     *
     * @param image The album cover image to use as background
     */
    private void updateBlurredBackground(Image image) {
        if (this.blurredBackground != null) {
            this.blurredBackground.setImage(image);
        }
    }

    // ==================== Initialization Methods ====================

    /**
     * Initializes the blurred background layer for the now playing view.
     * Creates a StackPane wrapper containing:
     * 1. A blurred ImageView (album cover scaled to fill)
     * 2. A dark semi-transparent overlay for readability
     * 3. The coverQueueContainer (actual content) on top
     *
     * @param layout The main BorderPane for binding dimensions
     */
    private void initBlurredBackground(BorderPane layout) {
        // Create the wrapper StackPane first so we can bind children to it
        this.nowPlayingWrapper = new StackPane();
        this.nowPlayingWrapper.setAlignment(Pos.CENTER);

        // Create the blurred background ImageView
        this.blurredBackground = new ImageView(DEFAULT_ALBUM_COVER);
        this.blurredBackground.setPreserveRatio(false); // Stretch to fill
        this.blurredBackground.setSmooth(true);
        this.blurredBackground.setEffect(new GaussianBlur(80)); // Heavy blur
        this.blurredBackground.setManaged(false); // Don't influence wrapper's layout size

        // Bind to the wrapper's own size so it fills exactly the now playing area
        this.blurredBackground.fitWidthProperty().bind(
            this.nowPlayingWrapper.widthProperty()
        );
        this.blurredBackground.fitHeightProperty().bind(
            this.nowPlayingWrapper.heightProperty()
        );

        // Create a dark semi-transparent overlay for readability
        this.darkOverlay = new Rectangle();
        this.darkOverlay.setFill(Color.rgb(0, 0, 0, 0.55)); // 55% black overlay
        this.darkOverlay.setManaged(false); // Don't influence wrapper's layout size
        this.darkOverlay.widthProperty().bind(
            this.nowPlayingWrapper.widthProperty()
        );
        this.darkOverlay.heightProperty().bind(
            this.nowPlayingWrapper.heightProperty()
        );

        // Stack layers: blurred bg -> dark overlay -> content
        this.nowPlayingWrapper.getChildren().addAll(
            this.blurredBackground,
            this.darkOverlay,
            this.coverQueueContainer
        );

        // Clip the wrapper to its own bounds to prevent overflow onto bottom bar
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.nowPlayingWrapper.widthProperty());
        clip.heightProperty().bind(this.nowPlayingWrapper.heightProperty());
        this.nowPlayingWrapper.setClip(clip);

        // Transfer initial state from coverQueueContainer to wrapper
        this.nowPlayingWrapper.setVisible(false);
        this.nowPlayingWrapper.setMouseTransparent(true);
        this.nowPlayingWrapper.setManaged(false);
        this.nowPlayingWrapper.setTranslateY(10000);

        // The coverQueueContainer no longer needs to manage its own visibility/position
        this.coverQueueContainer.setVisible(true);
        this.coverQueueContainer.setMouseTransparent(false);
        this.coverQueueContainer.setManaged(true);
        this.coverQueueContainer.setTranslateY(0);
    }

    /**
     * Initializes the now playing view with album cover and queue.
     *
     * @param layout The main BorderPane for binding cover dimensions
     */
    private void initWindowCenter(BorderPane layout) {
        // Create container for now playing view
        this.coverQueueContainer = new GridPane();
        this.coverQueueContainer.getStyleClass().add("cover-queue-container");
        this.coverQueueContainer.setVgap(1);
        this.coverQueueContainer.setMaxWidth(Double.MAX_VALUE);
        this.coverQueueContainer.setAlignment(Pos.CENTER);

        // Two columns: cover (70%) and queue (30%)
        ColumnConstraints col1 = new ColumnConstraints(),
            col2 = new ColumnConstraints();
        col1.setPercentWidth(70);
        col1.setHalignment(HPos.CENTER);
        col2.setPercentWidth(30);
        col2.setHalignment(HPos.RIGHT);
        this.coverQueueContainer.getColumnConstraints().addAll(col1, col2);

        // Initialize album cover with animation wrapper
        setSongCover(layout);
        Animated animatedCover = new Animated(
            this.songCover,
            new AnimatedScale()
        );

        // Initialize queue list
        initQueueView();

        // Create right panel with tabs + content
        createNowPlayingRightPanel();

        // Add cover and right panel to grid
        this.coverQueueContainer.add(animatedCover, 0, 0);
        this.coverQueueContainer.add(this.rightPanelContent, 1, 0);
    }

    /**
     * Initializes the queue list view with custom cell factory.
     */
    private void initQueueView() {
        this.queue = new ListView<>();
        this.queue.prefHeightProperty().bind(
            this.songCover.fitHeightProperty()
        );
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
    public void setupQueueSelectionHandler(
        List<File> playlist,
        Consumer<String> setTitleCallback,
        Button playPauseButton
    ) {
        this.queue.setOnMouseClicked(event -> {
            int index = this.queue.getSelectionModel().getSelectedIndex();
            if (
                index >= 0 &&
                index < this.queue.getItems().size() &&
                playlist != null
            ) {
                // Stop current playback
                if (this.musicPlayer.isRunning()) {
                    this.musicPlayer.stop();
                }

                // Load selected song
                this.musicPlayer.setPlaylistPosition(index);
                this.musicPlayer.changeSong(playlist.get(index));

                // Update UI
                setTitleCallback.accept(
                    playlist.get(index).getName() + " ~ Another MP3 Player"
                );
                if (
                    updateVolumeSliderCallback != null
                ) updateVolumeSliderCallback.run();
                if (
                    updateSongSliderCallback != null
                ) updateSongSliderCallback.run();
                if (
                    updateSongLabelsCallback != null
                ) updateSongLabelsCallback.run();

                // Lyrics refresh for selected song
                loadLyricsForCurrentSong();

                // Start playback
                this.musicPlayer.start();
                ImageCache.setButtonImage(playPauseButton, "new-pause.png");
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
        loadLyricsForCurrentSong();
        updateLyricsHighlight();
    }

    /**
     * Initializes the album cover image view.
     * Binds size to main layout and adds click handler for play/pause.
     * Uses cached default album cover instead of reading from disk each time.
     *
     * @param layout The main BorderPane for size binding
     */
    private void setSongCover(BorderPane layout) {
        this.songCover = new ImageView(DEFAULT_ALBUM_COVER);
        this.songCover.setPreserveRatio(true);

        // Bind size to 70% of main layout
        this.songCover.fitWidthProperty().bind(
            layout.widthProperty().multiply(0.7)
        );
        this.songCover.fitHeightProperty().bind(
            layout.heightProperty().multiply(0.7)
        );

        // Click on cover to toggle play/pause
        this.songCover.setOnMouseClicked(event -> {
            if (this.musicPlayer.hasClip()) {
                if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
                    this.musicPlayer.stop();
                } else {
                    this.musicPlayer.start();
                }
                if (onPlayPauseToggle != null) onPlayPauseToggle.run();
            }
        });
    }

    /**
     * Initializes the home page container.
     */
    private void initHomePage() {
        this.mainPage = new TilePane();
        this.mainPage.setAlignment(Pos.TOP_LEFT);
        this.mainPage.setPrefColumns(4);
        this.mainPage.setHgap(20);
        this.mainPage.setVgap(20);

        this.homeControlsBar = new HBox();
        this.homeControlsBar.setSpacing(12);
        this.homeControlsBar.setPadding(new Insets(0, 0, 12, 0));
        this.homeControlsBar.setAlignment(Pos.CENTER_LEFT);
        this.homeControlsBar.getStyleClass().add("home-controls-bar");

        this.viewToggleButton = new Button("List View");
        this.viewToggleButton.getStyleClass().add("home-view-toggle-button");

        this.sortComboBox = new javafx.scene.control.ComboBox<>();
        this.sortComboBox.getItems().addAll(
            SORT_TITLE,
            SORT_ARTIST,
            SORT_ALBUM
        );
        this.sortComboBox.setValue(SORT_TITLE);
        this.sortComboBox.getStyleClass().add("home-sort-combo");

        this.homeControlsBar.getChildren().addAll(
            this.viewToggleButton,
            this.sortComboBox
        );
    }

    /**
     * Sets up the "Load Songs" button on the home page.
     *
     * @param multipleFileSelectionCallback Callback to open file selection dialog
     */
    public void setupHomePageLoadButton(
        Runnable multipleFileSelectionCallback
    ) {
        Button loadSongsButton = new Button("Load Songs");
        loadSongsButton.getStyleClass().add("home-page-load-songs-button");
        loadSongsButton.setOnMouseClicked(event -> {
            if (multipleFileSelectionCallback != null) {
                multipleFileSelectionCallback.run();
            }
        });

        this.mainPage.getChildren().add(loadSongsButton);
    }

    private void renderHomePageItems(
        List<HomeSongItem> sourceItems,
        Consumer<String> setTitleCallback,
        Runnable updateUiCallback
    ) {
        if (sourceItems == null) return;

        this.mainPage.getChildren().clear();
        applyCurrentViewMode();

        List<HomeSongItem> items = new ArrayList<>(sourceItems);
        items.sort(getCurrentComparator());

        for (HomeSongItem item : items) {
            final int index = findPlaylistIndex(item.file);
            if (index < 0) continue;

            Node rowOrCard = this.isGridView
                ? createGridCard(
                      item,
                      index,
                      setTitleCallback,
                      updateUiCallback
                  )
                : createListRow(
                      item,
                      index,
                      setTitleCallback,
                      updateUiCallback
                  );

            this.mainPage.getChildren().add(rowOrCard);
        }

        this.viewToggleButton.setOnAction(e -> {
            this.isGridView = !this.isGridView;
            this.viewToggleButton.setText(
                this.isGridView ? "List View" : "Grid View"
            );
            renderHomePageItems(
                sourceItems,
                setTitleCallback,
                updateUiCallback
            );
        });

        this.sortComboBox.setOnAction(e ->
            renderHomePageItems(sourceItems, setTitleCallback, updateUiCallback)
        );
    }

    private Comparator<HomeSongItem> getCurrentComparator() {
        String selected =
            this.sortComboBox != null
                ? this.sortComboBox.getValue()
                : SORT_TITLE;

        if (SORT_ARTIST.equals(selected)) {
            return Comparator.comparing((HomeSongItem i) ->
                normalize(i.artist)
            ).thenComparing(i -> normalize(i.title));
        }
        if (SORT_ALBUM.equals(selected)) {
            return Comparator.comparing((HomeSongItem i) ->
                normalize(i.album)
            ).thenComparing(i -> normalize(i.title));
        }

        return Comparator.comparing((HomeSongItem i) ->
            normalize(i.title)
        ).thenComparing(i -> normalize(i.artist));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private int findPlaylistIndex(File file) {
        List<File> playlist = this.musicPlayer.getPlaylist();
        if (playlist == null) return -1;

        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).equals(file)) return i;
        }
        return -1;
    }

    private Node createGridCard(
        HomeSongItem item,
        int playlistIndex,
        Consumer<String> setTitleCallback,
        Runnable updateUiCallback
    ) {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(8));
        vbox.setSpacing(6);
        vbox.getStyleClass().add("home-grid-card");

        ImageView imageView = new ImageView(item.image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        Label title = new Label(item.title);
        Label artist = new Label(item.artist);

        vbox.getChildren().addAll(imageView, title, artist);
        attachSongClickHandler(
            vbox,
            playlistIndex,
            setTitleCallback,
            updateUiCallback
        );
        return vbox;
    }

    private Node createListRow(
        HomeSongItem item,
        int playlistIndex,
        Consumer<String> setTitleCallback,
        Runnable updateUiCallback
    ) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(12);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.getStyleClass().add("home-list-row");

        ImageView imageView = new ImageView(item.image);
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);

        VBox textBox = new VBox();
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setSpacing(2);

        Label title = new Label(item.title);
        Label subtitle = new Label(item.artist + " • " + item.album);

        textBox.getChildren().addAll(title, subtitle);
        row.getChildren().addAll(imageView, textBox);

        attachSongClickHandler(
            row,
            playlistIndex,
            setTitleCallback,
            updateUiCallback
        );
        return row;
    }

    private void attachSongClickHandler(
        Node node,
        int playlistIndex,
        Consumer<String> setTitleCallback,
        Runnable updateUiCallback
    ) {
        node.setOnMouseClicked(event -> {
            List<File> playlist = musicPlayer.getPlaylist();
            if (
                playlist == null ||
                playlistIndex < 0 ||
                playlistIndex >= playlist.size()
            ) return;

            if (musicPlayer.isRunning()) {
                musicPlayer.stop();
            }

            musicPlayer.setPlaylistPosition(playlistIndex);
            musicPlayer.changeSong(playlist.get(playlistIndex));

            setTitleCallback.accept(
                playlist.get(playlistIndex).getName() + " ~ Another MP3 Player"
            );
            if (updateUiCallback != null) {
                updateUiCallback.run();
            }

            if (loadQueueViewCallback != null) {
                loadQueueViewCallback.run();
            }

            updateQueueSelection();
            loadLyricsForCurrentSong();
            musicPlayer.start();
        });
    }

    private void applyCurrentViewMode() {
        this.mainPage.setAlignment(Pos.TOP_LEFT);

        if (this.isGridView) {
            this.mainPage.setPrefColumns(4);
            this.mainPage.setHgap(20);
            this.mainPage.setVgap(20);
        } else {
            this.mainPage.setPrefColumns(1);
            this.mainPage.setHgap(0);
            this.mainPage.setVgap(6);
        }
    }

    private static class HomeSongItem {

        final File file;
        final String title;
        final String artist;
        final String album;
        final String duration;
        final Image image;

        HomeSongItem(
            File file,
            String title,
            String artist,
            String album,
            String duration,
            Image image
        ) {
            this.file = file;
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "Unknown Artist";
            this.album = album != null ? album : "Unknown Album";
            this.duration = duration != null ? duration : "0:00";
            this.image = image;
        }
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

        VBox homeContainer = new VBox(this.homeControlsBar, scrollPane);
        homeContainer.setAlignment(Pos.TOP_LEFT);
        homeContainer.setPadding(new Insets(10, 14, 10, 14));
        homeContainer.setFillWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Make the scroll pane and tile pane use available width to avoid right-side whitespace
        scrollPane.prefWidthProperty().bind(homeContainer.widthProperty());
        this.mainPage.prefWidthProperty().bind(
            scrollPane.widthProperty().subtract(18)
        );

        // Bind home page height to scroll pane
        this.mainPage.prefHeightProperty().bind(
            scrollPane.heightProperty().subtract(2)
        );

        this.homeNowPlayingContainer.getChildren().add(homeContainer);
    }

    private void createNowPlayingRightPanel() {
        this.queueTabLabel = new Label("UP NEXT");
        this.queueTabLabel.getStyleClass().add("queue-header-selected");

        this.lyricsTabLabel = new Label("LYRICS");
        this.lyricsTabLabel.getStyleClass().add("queue-header-unselected");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(this.queueTabLabel, this.lyricsTabLabel, spacer);
        header.setSpacing(20);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("queue-header-container");

        this.queueContainerNode = new VBox(this.queue);
        VBox.setVgrow(this.queue, Priority.ALWAYS);
        this.queueContainerNode.setPadding(new Insets(0, 0, 0, 0));

        this.lyricsContent = new VBox();
        this.lyricsContent.setSpacing(8);
        this.lyricsContent.setPadding(new Insets(8, 8, 8, 8));

        this.lyricsEmptyLabel = new Label(
            "No local lyrics found.\nAdd a .lrc or .txt file with the same song name."
        );
        this.lyricsUnsyncedLabel = new Label("Unsynced lyrics (text only)");

        this.lyricsScrollPane = new ScrollPane(this.lyricsContent);
        this.lyricsScrollPane.setFitToWidth(true);
        this.lyricsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.lyricsScrollPane.setVbarPolicy(
            ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        this.lyricsScrollPane.getStyleClass().add("modern-scroll-pane");

        // Constrain right-panel body height to follow album cover height
        this.queueContainerNode.prefHeightProperty().bind(
            this.songCover.fitHeightProperty()
        );
        this.queueContainerNode.maxHeightProperty().bind(
            this.songCover.fitHeightProperty()
        );
        this.lyricsScrollPane.prefHeightProperty().bind(
            this.songCover.fitHeightProperty()
        );
        this.lyricsScrollPane.maxHeightProperty().bind(
            this.songCover.fitHeightProperty()
        );

        StackPane body = new StackPane(
            this.queueContainerNode,
            this.lyricsScrollPane
        );
        body.setAlignment(Pos.TOP_LEFT);
        body.setMinHeight(0);
        body.prefHeightProperty().bind(this.songCover.fitHeightProperty());
        body.maxHeightProperty().bind(this.songCover.fitHeightProperty());
        VBox.setVgrow(body, Priority.NEVER);

        this.queueContainerNode.setMaxWidth(Double.MAX_VALUE);
        this.queueContainerNode.setAlignment(Pos.TOP_LEFT);
        this.lyricsScrollPane.setMaxWidth(Double.MAX_VALUE);
        this.lyricsScrollPane.setMinHeight(0);

        this.rightPanelContent = new VBox(header, body);
        this.rightPanelContent.setPadding(new Insets(0, 25, 0, 0));
        this.rightPanelContent.setAlignment(Pos.TOP_LEFT);
        this.rightPanelContent.setFillWidth(true);
        this.rightPanelContent.setMinHeight(0);
        this.rightPanelContent.maxHeightProperty().bind(
            this.songCover.fitHeightProperty().add(header.heightProperty())
        );
        VBox.setVgrow(body, Priority.NEVER);

        this.queueTabLabel.setOnMouseClicked(e -> switchToQueueTab());
        this.lyricsTabLabel.setOnMouseClicked(e -> switchToLyricsTab());

        switchToQueueTab();
    }

    private void switchToQueueTab() {
        this.showingLyricsTab = false;
        this.queueContainerNode.setVisible(true);
        this.queueContainerNode.setManaged(true);
        this.lyricsScrollPane.setVisible(false);
        this.lyricsScrollPane.setManaged(false);

        this.queueContainerNode.toFront();

        this.queueTabLabel.getStyleClass().remove("queue-header-unselected");
        if (
            !this.queueTabLabel.getStyleClass().contains(
                "queue-header-selected"
            )
        ) {
            this.queueTabLabel.getStyleClass().add("queue-header-selected");
        }

        this.lyricsTabLabel.getStyleClass().remove("queue-header-selected");
        if (
            !this.lyricsTabLabel.getStyleClass().contains(
                "queue-header-unselected"
            )
        ) {
            this.lyricsTabLabel.getStyleClass().add("queue-header-unselected");
        }
    }

    private void switchToLyricsTab() {
        this.showingLyricsTab = true;
        this.queueContainerNode.setVisible(false);
        this.queueContainerNode.setManaged(false);
        this.lyricsScrollPane.setVisible(true);
        this.lyricsScrollPane.setManaged(true);

        this.lyricsScrollPane.toFront();

        this.lyricsTabLabel.getStyleClass().remove("queue-header-unselected");
        if (
            !this.lyricsTabLabel.getStyleClass().contains(
                "queue-header-selected"
            )
        ) {
            this.lyricsTabLabel.getStyleClass().add("queue-header-selected");
        }

        this.queueTabLabel.getStyleClass().remove("queue-header-selected");
        if (
            !this.queueTabLabel.getStyleClass().contains(
                "queue-header-unselected"
            )
        ) {
            this.queueTabLabel.getStyleClass().add("queue-header-unselected");
        }

        loadLyricsForCurrentSong();
    }

    public void loadLyricsForCurrentSong() {
        this.currentLyricsTrack = null;
        this.currentHighlightedLyricsIndex = -1;
        this.syncedLyricsLineLabels.clear();
        this.lyricsContent.getChildren().clear();

        if (!this.musicPlayer.hasPlaylist() || !this.musicPlayer.hasClip()) {
            this.lyricsContent.getChildren().add(this.lyricsEmptyLabel);
            return;
        }

        List<File> playlist = this.musicPlayer.getPlaylist();
        int pos = this.musicPlayer.getPlaylistPosition();
        if (playlist == null || pos < 0 || pos >= playlist.size()) {
            this.lyricsContent.getChildren().add(this.lyricsEmptyLabel);
            return;
        }

        File currentSong = playlist.get(pos);
        Optional<LyricsTrack> maybe = this.lyricsService.resolveLyrics(
            currentSong
        );
        if (maybe.isEmpty()) {
            this.lyricsContent.getChildren().add(this.lyricsEmptyLabel);
            return;
        }

        this.currentLyricsTrack = maybe.get();

        if (!this.currentLyricsTrack.isSynced()) {
            this.lyricsUnsyncedLabel.setStyle(
                "-fx-text-fill: #aaaaaa; -fx-font-size: 12px;"
            );
            Label text = new Label(this.currentLyricsTrack.getRawText());
            text.setWrapText(true);
            text.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            this.lyricsContent.getChildren().addAll(
                this.lyricsUnsyncedLabel,
                text
            );
            return;
        }

        for (model.LyricsLine line : this.currentLyricsTrack.getLines()) {
            Label l = new Label(
                line.getText().isBlank() ? " " : line.getText()
            );
            l.setWrapText(true);
            l.setMaxWidth(Double.MAX_VALUE);
            l.setStyle(buildSyncedLyricsStyleForDistance(Integer.MAX_VALUE));
            l.setScaleX(LYRICS_NORMAL_SCALE);
            l.setScaleY(LYRICS_NORMAL_SCALE);
            this.syncedLyricsLineLabels.add(l);
            this.lyricsContent.getChildren().add(l);
        }

        updateLyricsHighlight();
    }

    public void updateLyricsHighlight() {
        if (
            this.currentLyricsTrack == null ||
            !this.currentLyricsTrack.isSynced() ||
            this.currentLyricsTrack.getLines().isEmpty() ||
            !this.musicPlayer.hasClip()
        ) {
            return;
        }

        long playbackMs = this.musicPlayer.getPlaybackPositionMs();
        int activeIndex = findActiveLyricsIndexByTime(playbackMs);
        if (activeIndex == this.currentHighlightedLyricsIndex) return;

        applySyncedLyricsVisualState(activeIndex);

        if (
            activeIndex >= 0 && activeIndex < this.syncedLyricsLineLabels.size()
        ) {
            this.currentHighlightedLyricsIndex = activeIndex;

            if (this.showingLyricsTab) {
                Platform.runLater(() -> scrollToActiveLyricsLine(activeIndex));
            }
        }
    }

    private int findActiveLyricsIndexByTime(long currentMs) {
        List<model.LyricsLine> lines = this.currentLyricsTrack.getLines();
        int lo = 0,
            hi = lines.size() - 1,
            ans = 0;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).getTimeMs() <= currentMs) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    private void scrollToActiveLyricsLine(int activeIndex) {
        if (this.syncedLyricsLineLabels.isEmpty()) return;
        if (
            activeIndex < 0 || activeIndex >= this.syncedLyricsLineLabels.size()
        ) return;

        Label activeLabel = this.syncedLyricsLineLabels.get(activeIndex);

        double contentHeight =
            this.lyricsContent.getBoundsInLocal().getHeight();
        double viewportHeight =
            this.lyricsScrollPane.getViewportBounds().getHeight();

        if (
            contentHeight <= 0 ||
            viewportHeight <= 0 ||
            contentHeight <= viewportHeight
        ) {
            this.lyricsScrollPane.setVvalue(0);
            return;
        }

        // Compute active line center in content coordinates
        double lineTop = activeLabel.getBoundsInParent().getMinY();
        double lineHeight = activeLabel.getBoundsInParent().getHeight();
        double lineCenter = lineTop + (lineHeight / 2.0);

        // Keep active line near configured viewport ratio (Apple/Spotify-like feel)
        double targetPixel =
            lineCenter - (viewportHeight * LYRICS_TARGET_VIEWPORT_RATIO);
        double maxPixel = contentHeight - viewportHeight;
        double clampedPixel = Math.max(0, Math.min(maxPixel, targetPixel));
        double targetV = clampedPixel / maxPixel;

        double currentV = this.lyricsScrollPane.getVvalue();
        if (Math.abs(currentV - targetV) < 0.0015) {
            this.lyricsScrollPane.setVvalue(targetV);
            return;
        }

        // Persistent easing timeline: stop and retarget instead of spawning a new one every tick
        if (this.lyricsScrollAnimation != null) {
            this.lyricsScrollAnimation.stop();
        }

        this.lyricsScrollAnimation = new Timeline(
            new KeyFrame(
                Duration.millis(LYRICS_SCROLL_ANIMATION_MS),
                new KeyValue(
                    this.lyricsScrollPane.vvalueProperty(),
                    targetV,
                    javafx.animation.Interpolator.EASE_BOTH
                )
            )
        );
        this.lyricsScrollAnimation.play();
    }

    private void applySyncedLyricsVisualState(int activeIndex) {
        if (this.syncedLyricsLineLabels.isEmpty()) return;

        for (int i = 0; i < this.syncedLyricsLineLabels.size(); i++) {
            Label lineLabel = this.syncedLyricsLineLabels.get(i);
            int distance = (activeIndex >= 0)
                ? Math.abs(i - activeIndex)
                : Integer.MAX_VALUE;

            lineLabel.setStyle(buildSyncedLyricsStyleForDistance(distance));

            double targetScale = computeSyncedLyricsScaleForDistance(distance);
            animateSyncedLyricsLineScale(lineLabel, targetScale);
        }
    }

    private String buildSyncedLyricsStyleForDistance(int distance) {
        if (distance == 0) {
            return (
                "-fx-text-fill: " +
                LYRICS_ACTIVE_TEXT_COLOR +
                "; -fx-font-size: " +
                LYRICS_ACTIVE_FONT_SIZE_PX +
                "px; -fx-font-weight: bold;"
            );
        }

        String color =
            distance <= LYRICS_CONTEXT_WINDOW
                ? LYRICS_NEAR_CONTEXT_TEXT_COLOR
                : LYRICS_FAR_TEXT_COLOR;

        return (
            "-fx-text-fill: " +
            color +
            "; -fx-font-size: " +
            LYRICS_BASE_FONT_SIZE_PX +
            "px; -fx-font-weight: normal;"
        );
    }

    private double computeSyncedLyricsScaleForDistance(int distance) {
        if (distance == 0) {
            return LYRICS_ACTIVE_SCALE;
        }
        if (distance <= LYRICS_CONTEXT_WINDOW) {
            return LYRICS_NEAR_CONTEXT_SCALE;
        }
        return LYRICS_NORMAL_SCALE;
    }

    private void animateSyncedLyricsLineScale(
        Label lineLabel,
        double targetScale
    ) {
        double currentScaleX = lineLabel.getScaleX();
        double currentScaleY = lineLabel.getScaleY();

        if (
            Math.abs(currentScaleX - targetScale) < 0.0001 &&
            Math.abs(currentScaleY - targetScale) < 0.0001
        ) {
            return;
        }

        Object existing = lineLabel
            .getProperties()
            .get("lyricsScaleTransition");
        if (existing instanceof ScaleTransition) {
            ((ScaleTransition) existing).stop();
        }

        ScaleTransition transition = new ScaleTransition(
            Duration.millis(LYRICS_SCALE_ANIMATION_MS),
            lineLabel
        );
        transition.setFromX(currentScaleX);
        transition.setFromY(currentScaleY);
        transition.setToX(targetScale);
        transition.setToY(targetScale);
        transition.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        transition.setOnFinished(e ->
            lineLabel.getProperties().remove("lyricsScaleTransition")
        );

        lineLabel.getProperties().put("lyricsScaleTransition", transition);
        transition.play();
    }
}
