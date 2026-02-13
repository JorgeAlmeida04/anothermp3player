package containers;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import music_player.MusicPlayerAccess;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

public class BottomContainer {

    private GridPane bottomLayout;
    private GridPane playbackBox;

    private Button playPauseButton;
    private Button nextButton;
    private Button prevButton;

    private Slider volumeSlider;
    private ProgressBar volumeBar;
    private StackPane volumeSliderBar;

    private Slider songSlider;
    private ProgressBar songBar;
    private StackPane songSliderBar;
    private boolean isDragging = false;

    private Label songTitle;
    private Label songArtist;
    private VBox labelVBox;

    private final MusicPlayerAccess musicPlayer;
    private final Consumer<Region> toggleViewCallback;
    private final Runnable loadPlaylistSongCallback;
    private final Runnable updateSongLabelsCallback;
    private final Consumer<File> onPrevSongCallback;

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

    public void initialize(Region coverQueueContainer) throws Exception {
        initBottomLayout(coverQueueContainer);
    }

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

    public void updateVolumeSlider() {
        this.volumeSlider.setMin(0);
        this.volumeSlider.setMax(100);
        this.volumeSlider.setValue(10);
    }

    public void updateSongSlider() {
        this.songSlider.setMax(this.musicPlayer.getClipLength());
        this.songSlider.setMin(0);
        this.songSlider.setValue(0);
    }

    public void updateSongLabels() {
        this.songTitle.setText(this.musicPlayer.getSongTitle());
        this.songArtist.setText(this.musicPlayer.getSongArtist());
    }

    public void updatePlayPauseButton() {
        if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
            setImage(playPauseButton, "new-pause.png");
        } else {
            setImage(playPauseButton, "new-play.png");
        }
    }

    public void updateSongSliderPosition() {
        if (!this.isDragging) {
            this.songSlider.setValue(this.musicPlayer.getClipCurrentValue());
        }
    }

    private void initBottomLayout(Region coverQueueContainer) throws Exception {
        this.bottomLayout = new GridPane();
        this.bottomLayout.getStyleClass().add("bottom-layout");
        this.bottomLayout.setHgap(1);
        this.bottomLayout.setMaxWidth(Double.MAX_VALUE);
        this.bottomLayout.setAlignment(Pos.CENTER);
        this.bottomLayout.setPadding(new Insets(0, 5, 5, 5));

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        column1.setFillWidth(true);
        this.bottomLayout.getColumnConstraints().addAll(column1);

        this.playbackBox = new GridPane();
        this.playbackBox.setVgap(2);
        this.playbackBox.setMaxWidth(Double.MAX_VALUE);
        this.playbackBox.setAlignment(Pos.CENTER);
        this.playbackBox.setPadding(new Insets(5, 5, 5, 5));

        ColumnConstraints col1 = new ColumnConstraints(), col2 = new ColumnConstraints(), col3 = new ColumnConstraints();
        col1.setPercentWidth(25);
        col1.setHalignment(HPos.LEFT);
        col2.setPercentWidth(50);
        col2.setHalignment(HPos.CENTER);
        col3.setPercentWidth(25);
        col3.setHalignment(HPos.RIGHT);
        playbackBox.getColumnConstraints().addAll(col1, col2, col3);

        addPlaybackButtons();
        initSongSlider();
        initVolumeSlider();
        initSongInfoVisualizer();

        this.playbackBox.add(labelVBox, 1, 0);
        this.playbackBox.add(volumeSliderBar, 2, 0);

        this.bottomLayout.add(songSliderBar, 0, 0);
        this.bottomLayout.add(playbackBox, 0, 1);

        this.bottomLayout.setOnMouseClicked(event -> {
            if (toggleViewCallback != null) {
                toggleViewCallback.accept(coverQueueContainer);
            }
        });
    }

    private void addPlaybackButtons() {
        playPauseButton = new Button();
        setImage(playPauseButton, "new-play.png");
        playPauseButton.setOnAction(e -> {
            if (this.musicPlayer.hasClip()) {
                if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
                    this.musicPlayer.stop();
                    setImage(playPauseButton, "new-play.png");
                } else {
                    this.musicPlayer.start();
                    setImage(playPauseButton, "new-pause.png");
                }
            }
        });

        nextButton = new Button();
        setImage(nextButton, "next.png");
        nextButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist() && loadPlaylistSongCallback != null) {
                loadPlaylistSongCallback.run();
            }
        });

        prevButton = new Button();
        setImage(prevButton, "prev.png");
        prevButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist()) {
                boolean wasRunning = this.musicPlayer.isRunning();
                this.musicPlayer.stop();
                File song = this.musicPlayer.loadPreviousSong();
                if (song != null && onPrevSongCallback != null) {
                    onPrevSongCallback.accept(song);
                    if (wasRunning) {
                        this.musicPlayer.start();
                        setImage(this.playPauseButton, "new-pause.png");
                    }
                }
            }
        });

        FlowPane playbackButtons = new FlowPane();
        playbackButtons.setHgap(5);
        playbackButtons.setAlignment(Pos.CENTER);
        playbackButtons.setPadding(new Insets(5, 5, 5, 5));
        playbackButtons.getChildren().addAll(prevButton, playPauseButton, nextButton);

        playbackBox.add(playbackButtons, 0, 0);
    }

    private void initSongInfoVisualizer() {
        labelVBox = new VBox();
        labelVBox.setSpacing(10);
        labelVBox.setPadding(new Insets(10, 10, 0, 10));
        labelVBox.setAlignment(Pos.CENTER);

        songTitle = new Label("Song Title");
        songTitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.REGULAR, 25));

        songArtist = new Label("Song Artist");
        songArtist.setFont(Font.font("Comic Sans MS", FontWeight.SEMI_BOLD, FontPosture.REGULAR, 16));

        labelVBox.getChildren().addAll(songTitle, songArtist);
    }

    private void initVolumeSlider() {
        this.volumeBar = new ProgressBar(0);
        this.volumeBar.getStyleClass().add("volume-bar");
        this.volumeBar.setMaxWidth(Double.MAX_VALUE);

        this.volumeSlider = new Slider(0, 100, 10);
        this.volumeSlider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        this.volumeSlider.setMaxHeight(80);
        this.volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.musicPlayer.volumeChange(newValue.doubleValue());
        });
        this.volumeSlider.getStyleClass().add("volume-slider");

        setupSliderPopup(this.volumeSlider, value -> String.format("%.0f%%", value));

        this.volumeBar.progressProperty().bind(
                this.volumeSlider.valueProperty().divide(this.volumeSlider.maxProperty())
        );

        this.volumeSliderBar = new StackPane();
        this.volumeSliderBar.getChildren().addAll(this.volumeBar, this.volumeSlider);
        this.volumeSliderBar.setAlignment(Pos.CENTER);
    }

    private void initSongSlider() {
        this.songBar = new ProgressBar(0.0);
        this.songBar.getStyleClass().add("song-bar");
        this.songBar.setMaxWidth(Double.MAX_VALUE);

        this.songSlider = new Slider(0, 100, 0);
        this.songSlider.setShowTickMarks(false);
        this.songSlider.setShowTickLabels(false);
        this.songSlider.setMajorTickUnit(60 * 44231.5636364);
        this.songSlider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        this.songSlider.getStyleClass().add("song-slider");

        setupSliderPopup(this.songSlider, hoverValue -> {
            if (this.songSlider.getMax() > 0) {
                double timeUnit = 44231.5636364;
                int minutes = (int) (hoverValue / timeUnit) / 60;
                int seconds = (int) (hoverValue / timeUnit) % 60;
                return String.format("%d:%02d", minutes, seconds);
            }
            return "0:00";
        });

        this.songSlider.setOnMousePressed(event -> {
            this.isDragging = true;
        });

        this.songSlider.setOnMouseReleased(event -> {
            this.musicPlayer.setSongPosition(((int) this.songSlider.getValue()));
            this.isDragging = false;
        });

        this.songSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.songSlider.getMax() > 0) {
                this.songBar.setProgress(newValue.doubleValue() / this.songSlider.getMax());
            } else {
                this.songBar.setProgress(0);
            }
        });

        this.songSlider.maxProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                this.songBar.setProgress(this.songSlider.getValue() / newValue.doubleValue());
            } else {
                this.songBar.setProgress(0);
            }
        });

        this.songSliderBar = new StackPane();
        this.songSliderBar.getChildren().addAll(songBar, songSlider);
        this.songSliderBar.setAlignment(Pos.CENTER);

        this.songBar.prefWidthProperty().bind(this.songSliderBar.widthProperty());
    }

    private void setupSliderPopup(Slider slider, Function<Double, String> formatter) {
        Label label = new Label();
        Popup popup = new Popup();
        popup.getContent().add(label);

        slider.setOnMouseMoved(event -> {
            double mouseX = event.getX();
            double totalWidth = slider.getWidth();

            if (totalWidth <= 0) return;

            double percentage = mouseX / totalWidth;
            percentage = Math.max(0, Math.min(1, percentage));

            double min = slider.getMin();
            double max = slider.getMax();
            double hoverValue = min + (percentage * (max - min));

            label.setText(formatter.apply(hoverValue));

            popup.setAnchorX(event.getScreenX() + 10);
            popup.setAnchorY(event.getScreenY() - 30);
        });

        slider.setOnMouseEntered(event -> popup.show(slider, event.getScreenX() + 10, event.getScreenY() - 30));
        slider.setOnMouseExited(event -> popup.hide());
    }

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
