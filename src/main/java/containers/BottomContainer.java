package containers;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.scene.layout.RowConstraints;
import javafx.geometry.VPos;
import music_player.MusicPlayerAccess;
import util.ImageCache;

/**
 * BottomContainer manages the playback controls and song information at the bottom.
 * 3-Pillar Layout: Left (Info), Center (Controls), Right (Volume).
 */
public class BottomContainer {

    private static final Image DEFAULT_ALBUM_COVER = new Image(BottomContainer.class.getResourceAsStream("/assets/generic-album-cover.jpeg"));

    private GridPane bottomLayout;
    private GridPane playbackBox;

    // Playback control buttons
    private Button playPauseButton;
    private Button nextButton;
    private Button prevButton;
    private Button shuffleButton;
    private Button repeatButton;

    // Volume control components
    private Slider volumeSlider;
    private ProgressBar volumeBar;
    private StackPane volumeSliderBar;

    // Song progress components
    private Slider songSlider;
    private ProgressBar songBar;
    private StackPane songSliderBar;
    private boolean isDragging = false;

    // Song information display
    private Label songTitle;
    private Label songArtist;
    private ImageView miniCover;

    // Dependencies and callbacks
    private final MusicPlayerAccess musicPlayer;
    private final Consumer<Region> toggleViewCallback;
    private final Runnable loadPlaylistSongCallback;

    public BottomContainer(MusicPlayerAccess musicPlayer, Consumer<Region> toggleViewCallback, Runnable loadPlaylistSongCallback) {
        this.musicPlayer = musicPlayer;
        this.toggleViewCallback = toggleViewCallback;
        this.loadPlaylistSongCallback = loadPlaylistSongCallback;
    }

    public void initialize(Region nowPlayingView) {
        bottomLayout = new GridPane();
        bottomLayout.getStyleClass().add("bottom-layout");
        bottomLayout.setPadding(new Insets(0, 15, 0, 15));
        bottomLayout.setMinHeight(100);
        bottomLayout.setMaxHeight(100);

        // Allow clicking the bar to toggle the Now Playing view
        // We filter the target to avoid toggling when clicking buttons/sliders
        bottomLayout.setOnMouseClicked(event -> {
            Object target = event.getTarget();
            if (target == bottomLayout || target instanceof HBox || target instanceof VBox || target instanceof Label || target instanceof ImageView) {
                toggleViewCallback.accept(nowPlayingView);
            }
        });

        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setPercentWidth(30);
        leftCol.setHalignment(HPos.LEFT);

        ColumnConstraints centerCol = new ColumnConstraints();
        centerCol.setPercentWidth(40);
        centerCol.setHalignment(HPos.CENTER);

        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setPercentWidth(30);
        rightCol.setHalignment(HPos.RIGHT);

        bottomLayout.getColumnConstraints().addAll(leftCol, centerCol, rightCol);

        // Row Constraints for vertical centering
        RowConstraints progressRow = new RowConstraints();
        progressRow.setPrefHeight(5); // Fixed small height for progress line
        progressRow.setValignment(VPos.TOP);

        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS); // Take remaining space
        contentRow.setValignment(VPos.CENTER); // Vertical Center!

        bottomLayout.getRowConstraints().addAll(progressRow, contentRow);

        initProgressSection();
        initControlsLeft();   // Controls now on the Left
        initInfoCenter();     // Info now in the Center
        initRightSection();    // Volume on the Right
    }

    private void initProgressSection() {
        initSongSlider();
        // Progress bar spans all 3 columns at the very top (row 0)
        bottomLayout.add(songSliderBar, 0, 0, 3, 1);
        GridPane.setMargin(songSliderBar, new Insets(0, -15, 0, -15));
    }

    private void initControlsLeft() {
        playbackBox = new GridPane();
        playbackBox.setAlignment(Pos.CENTER_LEFT);
        addPlaybackButtons(playbackBox);
        bottomLayout.add(playbackBox, 0, 1);
    }

    private void initInfoCenter() {
        HBox centerBox = new HBox(12);
        centerBox.setAlignment(Pos.CENTER);

        miniCover = new ImageView(DEFAULT_ALBUM_COVER);
        miniCover.setFitHeight(56);
        miniCover.setFitWidth(56);
        miniCover.getStyleClass().add("mini-cover");
        Rectangle clip = new Rectangle(56, 56);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        miniCover.setClip(clip);

        VBox textStack = new VBox(2);
        textStack.setAlignment(Pos.CENTER_LEFT);

        songTitle = new Label("No Song Selected");
        songTitle.getStyleClass().add("bottom-title-label");
        
        songArtist = new Label("Unknown Artist");
        songArtist.getStyleClass().add("bottom-artist-label");

        textStack.getChildren().addAll(songTitle, songArtist);
        centerBox.getChildren().addAll(miniCover, textStack);

        bottomLayout.add(centerBox, 1, 1);
    }

    private void initRightSection() {
        HBox rightBox = new HBox(15);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        initVolumeSlider();
        rightBox.getChildren().add(volumeSliderBar);
        
        bottomLayout.add(rightBox, 2, 1);
    }

    public void updateSongLabels() {
        String title = musicPlayer.getSongTitle();
        String artist = musicPlayer.getSongArtist();
        byte[] imgData = musicPlayer.getSongAlbumImage();

        songTitle.setText(title != null ? title : "No Song Selected");
        songArtist.setText(artist != null ? artist : "Unknown Artist");

        if (imgData != null) {
            miniCover.setImage(new Image(new java.io.ByteArrayInputStream(imgData), 100, 100, true, true));
        } else {
            miniCover.setImage(DEFAULT_ALBUM_COVER);
        }
    }

    public GridPane getBottomLayout() { return bottomLayout; }
    public Button getPlayPauseButton() { return playPauseButton; }
    public Button getNextButton() { return nextButton; }
    public Button getPrevButton() { return prevButton; }
    public Slider getVolumeSlider() { return volumeSlider; }
    public Slider getSongSlider() { return songSlider; }
    public boolean isDragging() { return isDragging; }
    public void setDragging(boolean d) { this.isDragging = d; }

    public void updateVolumeSlider() {
        this.volumeSlider.setMin(0);
        this.volumeSlider.setMax(100);
        this.volumeSlider.setValue(this.musicPlayer.getCurrentVolume());
    }

    public void updateSongSlider() {
        this.songSlider.setMin(0);
        this.songSlider.setMax(this.musicPlayer.getClipLength());
        this.songSlider.setValue(0);
    }

    public void updateSongSliderPosition() {
        this.songSlider.setValue(this.musicPlayer.getClipCurrentValue());
    }

    public void updatePlayPauseButton() {
        if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
            ImageCache.setButtonImage(playPauseButton, "new-pause.png");
        } else {
            ImageCache.setButtonImage(playPauseButton, "new-play.png");
        }
    }

    public void updateShuffleButtonStyle() {
        if (this.musicPlayer.isShuffle()) {
            this.shuffleButton.setStyle("-fx-background-color: rgba(80, 127, 247, 0.4); -fx-background-radius: 100px; -fx-opacity: 1.0;");
        } else {
            this.shuffleButton.setStyle("-fx-background-color: transparent; -fx-opacity: 0.6;");
        }
    }

    public void updateRepeatButtonStyle() {
        if (this.musicPlayer.isRepeat()) {
            this.repeatButton.setStyle("-fx-background-color: rgba(80, 127, 247, 0.4); -fx-background-radius: 100px; -fx-opacity: 1.0;");
        } else {
            this.repeatButton.setStyle("-fx-background-color: transparent; -fx-opacity: 0.6;");
        }
    }

    private void addPlaybackButtons(GridPane playbackBox) {
        playPauseButton = new Button();
        ImageCache.setButtonImage(playPauseButton, "new-play.png");
        playPauseButton.setOnAction(e -> {
            if (this.musicPlayer.hasClip()) {
                if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) this.musicPlayer.stop();
                else this.musicPlayer.start();
            }
        });

        nextButton = new Button();
        ImageCache.setButtonImage(nextButton, "next.png");
        nextButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist()) {
                this.musicPlayer.stop();
                this.loadPlaylistSongCallback.run();
            }
        });

        prevButton = new Button();
        ImageCache.setButtonImage(prevButton, "prev.png");
        prevButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist()) {
                boolean wasRunning = this.musicPlayer.isRunning();
                this.musicPlayer.stop();
                if (this.musicPlayer.loadPreviousSong() != null && wasRunning) this.musicPlayer.start();
            }
        });

        shuffleButton = new Button();
        ImageCache.setButtonImage(shuffleButton, "shuffle.png");
        updateShuffleButtonStyle();
        shuffleButton.setOnAction(e -> {
            this.musicPlayer.setShuffle(!this.musicPlayer.isShuffle());
            updateShuffleButtonStyle();
        });

        repeatButton = new Button();
        ImageCache.setButtonImage(repeatButton, "repeat.png");
        updateRepeatButtonStyle();
        repeatButton.setOnAction(e -> {
            this.musicPlayer.setRepeat(!this.musicPlayer.isRepeat());
            updateRepeatButtonStyle();
        });

        FlowPane playbackButtons = new FlowPane();
        playbackButtons.setHgap(12);
        playbackButtons.setAlignment(Pos.CENTER);
        playbackButtons.setPadding(new Insets(5));
        playbackButtons.getChildren().addAll(shuffleButton, prevButton, playPauseButton, nextButton, repeatButton);
        playbackBox.add(playbackButtons, 0, 0);
    }

    private void initVolumeSlider() {
        this.volumeBar = new ProgressBar(0);
        this.volumeBar.getStyleClass().add("volume-bar");
        this.volumeBar.setMaxWidth(120);

        this.volumeSlider = new Slider(0, 100, 50);
        this.volumeSlider.setPrefWidth(120);
        this.volumeSlider.valueProperty().addListener((obs, old, val) -> this.musicPlayer.volumeChange(val.doubleValue()));
        this.volumeSlider.getStyleClass().add("volume-slider");

        setupSliderPopup(this.volumeSlider, v -> String.format("%.0f%%", v));
        this.volumeBar.progressProperty().bind(this.volumeSlider.valueProperty().divide(this.volumeSlider.maxProperty()));

        this.volumeSliderBar = new StackPane(volumeBar, volumeSlider);
        this.volumeSliderBar.setAlignment(Pos.CENTER);
    }

    private void initSongSlider() {
        this.songBar = new ProgressBar(0.0);
        this.songBar.getStyleClass().add("song-bar");
        this.songBar.setMaxWidth(Double.MAX_VALUE);

        this.songSlider = new Slider(0, 100, 0);
        this.songSlider.getStyleClass().add("song-slider");

        setupSliderPopup(this.songSlider, hoverValue -> {
            double timeUnit = 44231.5636364;
            int minutes = (int) (hoverValue / timeUnit) / 60;
            int seconds = (int) (hoverValue / timeUnit) % 60;
            return String.format("%d:%02d", minutes, seconds);
        });

        this.songSlider.setOnMousePressed(e -> isDragging = true);
        this.songSlider.setOnMouseReleased(e -> {
            this.musicPlayer.setSongPosition((int) this.songSlider.getValue());
            isDragging = false;
        });

        this.songSlider.valueProperty().addListener((obs, old, val) -> {
            if (this.songSlider.getMax() > 0) this.songBar.setProgress(val.doubleValue() / this.songSlider.getMax());
        });

        this.songSliderBar = new StackPane(songBar, songSlider);
        this.songBar.prefWidthProperty().bind(this.songSliderBar.widthProperty());
    }

    private void setupSliderPopup(Slider slider, Function<Double, String> formatter) {
        Label label = new Label();
        Popup popup = new Popup();
        popup.getContent().add(label);
        slider.setOnMouseMoved(e -> {
            double percentage = Math.max(0, Math.min(1, e.getX() / slider.getWidth()));
            label.setText(formatter.apply(slider.getMin() + (percentage * (slider.getMax() - slider.getMin()))));
            popup.setAnchorX(e.getScreenX() + 10);
            popup.setAnchorY(e.getScreenY() - 30);
        });
        slider.setOnMouseEntered(e -> popup.show(slider, e.getScreenX() + 10, e.getScreenY() - 30));
        slider.setOnMouseExited(e -> popup.hide());
    }
}
