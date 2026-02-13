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

public class CenterContainer {

    private GridPane coverQueueContainer;
    private ListView<QueueItem> queue;
    private ImageView songCover;
    private FlowPane mainPage;
    private StackPane homeNowPlayingContainer;

    private final MusicPlayerAccess musicPlayer;
    private final Consumer<List<File>> onPlaylistLoadedCallback;
    private final Consumer<File> onSongSelectedFromQueue;
    private final Runnable onPlayPauseToggle;
    private Runnable updateVolumeSliderCallback;
    private Runnable updateSongSliderCallback;
    private Runnable updateSongLabelsCallback;

    public CenterContainer(MusicPlayerAccess musicPlayer,
                           Consumer<List<File>> onPlaylistLoadedCallback,
                           Consumer<File> onSongSelectedFromQueue,
                           Runnable onPlayPauseToggle) {
        this.musicPlayer = musicPlayer;
        this.onPlaylistLoadedCallback = onPlaylistLoadedCallback;
        this.onSongSelectedFromQueue = onSongSelectedFromQueue;
        this.onPlayPauseToggle = onPlayPauseToggle;
    }

    public void initialize(BorderPane layout) throws Exception {
        initWindowCenter(layout);
        initHomePage();
        createHomeNowPlayingContainer();
    }

    public GridPane getCoverQueueContainer() {
        return coverQueueContainer;
    }

    public FlowPane getMainPage() {
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

    public void setUpdateCallbacks(Runnable updateVolumeSlider, Runnable updateSongSlider, Runnable updateSongLabels) {
        this.updateVolumeSliderCallback = updateVolumeSlider;
        this.updateSongSliderCallback = updateSongSlider;
        this.updateSongLabelsCallback = updateSongLabels;
    }

    public void loadQueueView(List<File> playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        this.queue.getItems().clear();

        for (File file : playlist) {
            this.queue.getItems().add(new QueueItem(file, file.getName(), "", "", null));
        }

        Task<Void> metadataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < playlist.size(); i++) {
                    File file = playlist.get(i);
                    QueueSongData data = musicPlayer.getQueueData(file);

                    Image img = null;
                    if (data.imageData != null) {
                        img = new Image(new ByteArrayInputStream(data.imageData), 40, 40, true, true);
                    }

                    QueueItem item = new QueueItem(file, data.title, data.artist, data.duration, img);

                    final int index = i;
                    Platform.runLater(() -> {
                        if (queue.getItems().size() > index) {
                            queue.getItems().set(index, item);
                        }
                    });

                    Thread.sleep(10);
                }
                return null;
            }
        };

        Thread thread = new Thread(metadataTask);
        thread.setDaemon(true);
        thread.start();
    }

    public void fillTheHomePage(List<File> playlist, String windowTitleSetter, Consumer<String> setTitleCallback, Runnable updateUiCallback) {
        if (playlist == null) return;

        this.mainPage.getChildren().clear();
        this.mainPage.setVgap(7);

        if (playlist.size() < 8) {
            this.mainPage.setHgap(playlist.size());
        } else {
            this.mainPage.setHgap(playlist.size() / 8);
        }

        this.mainPage.setAlignment(Pos.CENTER);
        this.mainPage.setColumnHalignment(HPos.CENTER);
        this.mainPage.setRowValignment(VPos.CENTER);
        this.mainPage.setPadding(new Insets(5, 5, 5, 5));

        Task<Void> metadataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < playlist.size(); i++) {
                    File file = playlist.get(i);
                    int index = i;
                    QueueSongData data = musicPlayer.getQueueData(file);

                    Image img = null;
                    if (data.imageData != null) {
                        img = new Image(new ByteArrayInputStream(data.imageData), 100, 100, true, true);
                    } else {
                        FileInputStream inputStream = new FileInputStream("src/main/resources/assets/generic-album-cover.jpeg");
                        img = new Image(new ByteArrayInputStream(inputStream.readAllBytes()), 100, 100, true, true);
                    }

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

                    Platform.runLater(() -> {
                        vbox.setOnMouseClicked(event -> {
                            if (musicPlayer.isRunning()) {
                                musicPlayer.stop();
                            }
                            musicPlayer.setPlaylistPosition(index);
                            musicPlayer.changeSong(playlist.get(index));
                            if (onPlaylistLoadedCallback != null) {
                                onPlaylistLoadedCallback.accept(playlist);
                            }

                            setTitleCallback.accept(playlist.get(index).getName() + " ~ Another MP3 Player");
                            if (updateUiCallback != null) {
                                updateUiCallback.run();
                            }

                            musicPlayer.start();
                        });
                        mainPage.getChildren().add(vbox);
                    });

                    Thread.sleep(10);
                }
                return null;
            }
        };

        Thread thread = new Thread(metadataTask);
        thread.setDaemon(true);
        thread.start();
    }

    public void updateSongCover() {
        ByteArrayInputStream bis = new ByteArrayInputStream(musicPlayer.getSongAlbumImage());
        this.songCover.setImage(new Image(bis, 400, 400, true, true));
    }

    private void initWindowCenter(BorderPane layout) throws Exception {
        this.coverQueueContainer = new GridPane();
        this.coverQueueContainer.getStyleClass().add("cover-queue-container");
        this.coverQueueContainer.setVisible(false);
        this.coverQueueContainer.setMouseTransparent(true);
        this.coverQueueContainer.setManaged(false);
        this.coverQueueContainer.setTranslateY(10000);
        this.coverQueueContainer.setVgap(1);
        this.coverQueueContainer.setMaxWidth(Double.MAX_VALUE);
        this.coverQueueContainer.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints(), col2 = new ColumnConstraints();

        col1.setPercentWidth(70);
        col1.setHalignment(HPos.CENTER);

        col2.setPercentWidth(30);
        col2.setHalignment(HPos.RIGHT);

        this.coverQueueContainer.getColumnConstraints().addAll(col1, col2);

        setSongCover(layout);
        Animated animatedCover = new Animated(this.songCover, new AnimatedScale());

        initQueueView();

        Label upNextLabel = new Label("UP NEXT");
        upNextLabel.getStyleClass().add("queue-header-selected");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox queueHeader = new HBox(upNextLabel, spacer);
        queueHeader.setSpacing(20);
        queueHeader.setPadding(new Insets(10));
        queueHeader.setAlignment(Pos.CENTER_LEFT);
        queueHeader.getStyleClass().add("queue-header-container");

        VBox queueContainer = new VBox(queueHeader, this.queue);
        VBox.setVgrow(this.queue, Priority.ALWAYS);
        queueContainer.setPadding(new Insets(0, 25, 0, 0));

        this.coverQueueContainer.add(animatedCover, 0, 0);
        this.coverQueueContainer.add(queueContainer, 1, 0);
    }

    private void initQueueView() {
        this.queue = new ListView<>();
        this.queue.prefHeightProperty().bind(this.songCover.fitHeightProperty());
        this.queue.getStyleClass().add("queue");
        this.queue.setCellFactory(param -> new QueueCell());
    }

    public void setupQueueSelectionHandler(List<File> playlist, Consumer<String> setTitleCallback, Button playPauseButton) {
        this.queue.setOnMouseClicked(event -> {
            int index = this.queue.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < this.queue.getItems().size() && playlist != null) {
                if (this.musicPlayer.isRunning()) {
                    this.musicPlayer.stop();
                }

                this.musicPlayer.setPlaylistPosition(index);
                this.musicPlayer.changeSong(playlist.get(index));

                setTitleCallback.accept(playlist.get(index).getName() + " ~ Another MP3 Player");
                if (updateVolumeSliderCallback != null) updateVolumeSliderCallback.run();
                if (updateSongSliderCallback != null) updateSongSliderCallback.run();
                if (updateSongLabelsCallback != null) updateSongLabelsCallback.run();

                this.musicPlayer.start();
                setImage(playPauseButton, "new-pause.png");
            }
        });
    }

    private void setSongCover(BorderPane layout) throws Exception {
        FileInputStream inputStream = new FileInputStream("src/main/resources/assets/generic-album-cover.jpeg");
        Image image = new Image(inputStream);
        this.songCover = new ImageView(image);
        this.songCover.setPreserveRatio(true);
        this.songCover.fitWidthProperty().bind(layout.widthProperty().multiply(0.7));
        this.songCover.fitHeightProperty().bind(layout.heightProperty().multiply(0.7));
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

    private void initHomePage() {
        this.mainPage = new FlowPane();
        this.mainPage.setAlignment(Pos.CENTER);
    }

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

    private void createHomeNowPlayingContainer() {
        this.homeNowPlayingContainer = new StackPane();
        this.homeNowPlayingContainer.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(this.mainPage);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("modern-scroll-pane");

        this.mainPage.prefHeightProperty().bind(scrollPane.heightProperty().subtract(2));

        this.homeNowPlayingContainer.getChildren().add(scrollPane);
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
