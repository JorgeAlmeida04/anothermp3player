import containers.*;
import music_player.MusicPlayerAccess;
import music_player.MusicPlayerModel;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.yetihafen.javafx.customcaption.CustomCaption;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MP3PlayerGUIJavaFX extends Application implements Observer {

    private static final double DEFAULT_UPDATE_DURATION = 0.1;

    private MusicPlayerModel musicPlayer;

    private BottomContainer bottomContainer;
    private CenterContainer centerContainer;
    private TopContainer topContainer;

    private List<File> playlist;
    private File songFile;

    private Stage window;
    private Scene scene;
    private BorderPane layout;

    private boolean isOpen = false;

    @Override
    public void init() {
        this.musicPlayer = new MusicPlayerModel();
        this.musicPlayer.addObserver(this);
    }

    @Override
    public void start(Stage stage) throws Exception {
        layout = new BorderPane();

        window = stage;
        window.setTitle("No Song Selected ~ Another MP3 Player");
        window.setOnCloseRequest(e -> {
            System.out.println("Closing");
            window.close();
        });

        initContainers();

        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATE_DURATION), e -> notifyGUI());
        Timeline t = new Timeline(updater);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        this.layout.setBottom(this.bottomContainer.getBottomLayout());
        this.bottomContainer.getBottomLayout().setVisible(false);
        this.layout.setTop(this.topContainer.getMenuBar());
        this.layout.setCenter(this.centerContainer.getHomeNowPlayingContainer());

        scene = new Scene(this.layout, 1260, 720);
        scene.getStylesheets().add("Default_Theme.css");

        window.setScene(scene);
        window.show();

        CustomCaption.setImmersiveDarkMode(this.window, true);

        this.centerContainer.getCoverQueueContainer().setVisible(false);
        this.centerContainer.getCoverQueueContainer().setMouseTransparent(true);
        Platform.runLater(() -> {
            this.centerContainer.getCoverQueueContainer().setTranslateY(2000);
        });
    }

    private void initContainers() throws Exception {
        centerContainer = new CenterContainer(
                (MusicPlayerAccess) musicPlayer,
            this::onPlaylistLoaded,
            this::onSongSelectedFromQueue,
            this::onPlayPauseToggle
        );
        centerContainer.initialize(layout);
        
        topContainer = new TopContainer(
                (MusicPlayerAccess) musicPlayer,
            window,
            this::onSongLoaded,
            this::onPlaylistLoaded,
            this::loadPlaylistSong,
            this::loadQueueView
        );
        topContainer.initialize();
        
        bottomContainer = new BottomContainer(
                (MusicPlayerAccess) musicPlayer,
            this::toggleView,
            this::loadPlaylistSong,
            this::updateSongLabels,
            this::onPrevSong
        );
        bottomContainer.initialize(centerContainer.getCoverQueueContainer());
        
        centerContainer.setUpdateCallbacks(
            this::updateVolumeSlider,
            this::updateSongSlider,
            this::updateSongLabels
        );
        centerContainer.setupHomePageLoadButton(this::multipleFileSelectionAndFill);
    }

    private void onSongLoaded(File songFile) {
        this.songFile = songFile;
        if (this.musicPlayer.hasClip() && this.musicPlayer.isRunning()) {
            this.musicPlayer.stop();
        }
        this.musicPlayer.changeSong(songFile);

        if (this.musicPlayer.hasClip()) {
            window.setTitle(songFile.getName() + "~ Another MP3 Player");
            updateSongLabels();
            updateVolumeSlider();
            updateSongSlider();
        }
    }

    private void onPlaylistLoaded(List<File> playlist) {
        this.playlist = playlist;
        this.musicPlayer.setPlaylist(playlist);
    }

    private void onSongSelectedFromQueue(File song) {
        window.setTitle(song.getName() + " ~ Another MP3 Player");
        updateVolumeSlider();
        updateSongSlider();
        updateSongLabels();
    }

    private void onPrevSong(File song) {
        window.setTitle(song.getName() + " ~ Another MP3 Player");
        updateVolumeSlider();
        updateSongSlider();
        updateSongLabels();
    }

    private void onPlayPauseToggle() {
        bottomContainer.updatePlayPauseButton();
    }

    private void loadPlaylistSong() {
        boolean wasRunning = false;
        File song = null;

        if (this.musicPlayer.hasClip() && this.musicPlayer.isRunning()) {
            this.musicPlayer.stop();
            wasRunning = true;
            song = this.musicPlayer.loadNextSong();
        } else {
            song = this.musicPlayer.initPlaylist();
        }

        if (song != null && this.musicPlayer.hasClip()) {
            window.setTitle(song.getName() + "~ Another MP3 Player");
            updateSongLabels();
            updateVolumeSlider();
            updateSongSlider();
            this.musicPlayer.start();

            if (wasRunning) {
                setImage(bottomContainer.getPlayPauseButton(), "new-pause.png");
                this.musicPlayer.start();
            }
        }
    }

    private void loadQueueView() {
        centerContainer.loadQueueView(playlist);
        centerContainer.setupQueueSelectionHandler(playlist, this::setWindowTitle, bottomContainer.getPlayPauseButton());
    }

    private void multipleFileSelectionAndFill() {
        List<File> selectedFiles = topContainer.multipleFileSelection();
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            this.playlist = selectedFiles;
            this.musicPlayer.setPlaylist(selectedFiles);
            centerContainer.getMainPage().getChildren().clear();
            centerContainer.fillTheHomePage(
                playlist,
                "",
                this::setWindowTitle,
                () -> {
                    updateVolumeSlider();
                    updateSongSlider();
                    updateSongLabels();
                    setImage(bottomContainer.getPlayPauseButton(), "new-pause.png");
                }
            );
        }
    }

    private void updateVolumeSlider() {
        if (bottomContainer != null) {
            bottomContainer.updateVolumeSlider();
        }
    }

    private void updateSongSlider() {
        if (bottomContainer != null) {
            bottomContainer.updateSongSlider();
        }
    }

    private void updateSongLabels() {
        if (bottomContainer != null) {
            bottomContainer.updateSongLabels();
        }
        if (centerContainer != null) {
            centerContainer.updateSongCover();
        }
    }

    private void setWindowTitle(String title) {
        window.setTitle(title);
    }

    private void notifyGUI() {
        this.musicPlayer.announceChanges();
    }

    private void toggleView(Region viewToAnimate) {
        boolean opening = !isOpen;

        if (opening) {
            StackPane container = centerContainer.getHomeNowPlayingContainer();
            if (!container.getChildren().contains(viewToAnimate)) {
                container.getChildren().add(viewToAnimate);
            }
            viewToAnimate.setVisible(true);
            viewToAnimate.setMouseTransparent(false);
            viewToAnimate.setManaged(true);
        }

        double endValue = opening ? 0 : this.layout.getHeight();

        Timeline timeline = new Timeline();

        KeyValue kv = new KeyValue(
                viewToAnimate.translateYProperty(),
                endValue,
                Interpolator.EASE_BOTH
        );

        KeyFrame kf = new KeyFrame(Duration.millis(150), kv);
        timeline.getKeyFrames().addAll(kf);

        timeline.setOnFinished(event -> {
            if (!opening) {
                viewToAnimate.setVisible(false);
                viewToAnimate.setMouseTransparent(true);
                viewToAnimate.setManaged(false);
                centerContainer.getHomeNowPlayingContainer().getChildren().remove(viewToAnimate);
            }
        });

        timeline.play();
        isOpen = opening;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()) {
            setImage(bottomContainer.getPlayPauseButton(), "new-pause.png");
        } else {
            setImage(bottomContainer.getPlayPauseButton(), "new-play.png");
        }
        if (this.musicPlayer.hasClip()) {
            this.bottomContainer.getBottomLayout().setVisible(true);
            if (this.musicPlayer.atEnd()) {
                if (this.musicPlayer.hasPlaylist()) {
                    loadPlaylistSong();
                } else {
                    this.bottomContainer.getBottomLayout().setVisible(false);
                }
            }
            if (!bottomContainer.isDragging()) {
                bottomContainer.getSongSlider().setValue(this.musicPlayer.getClipCurrentValue());
            }
        }
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
