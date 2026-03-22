import controllers.MainController;
import containers.*;
import db.DataBaseManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import util.ImageCache;

/**
 * Main application class for the MP3 Player.
 * This class handles the UI bootstrapping and assembly.
 * Logic and Event orchestration are delegated to MainController.
 */
public class MP3PlayerGUIJavaFX extends Application {

    private static final double DEFAULT_UPDATE_DURATION = 0.25; // Reduced from 0.1 to lower CPU usage (4Hz instead of 10Hz)

    private MusicPlayerModel musicPlayer;
    private MainController controller;

    // UI Containers
    private BottomContainer bottomContainer;
    private CenterContainer centerContainer;
    private TopContainer topContainer;

    private Stage window;
    private Scene scene;
    private BorderPane layout;
    private Timeline uiUpdateTimeline;
    private boolean isOpen = false;

    @Override
    public void init() throws Exception {
        DataBaseManager.getInstance().initialize();
        this.musicPlayer = new MusicPlayerModel();
        this.controller = new MainController(this.musicPlayer);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.window = stage;
        this.layout = new BorderPane();

        window.setTitle("No Song Selected ~ Another MP3 Player");
        window.getIcons().add(ImageCache.getImage("amp3p.png"));
        window.setOnCloseRequest(e -> shutdown());

        initContainers();
        
        // Let the controller take over orchestration
        controller.initialize(window, bottomContainer, centerContainer, topContainer);

        setupUpdateTimer();
        assembleLayout();

        scene = new Scene(this.layout, 1150, 797);
        scene.getStylesheets().add("Default_Theme.css");
        window.setScene(scene);
        window.show();

        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")){
            CustomCaption.setImmersiveDarkMode(this.window, true);
        }

        initNowPlayingView();
    }

    private void initContainers() throws Exception {
        centerContainer = new CenterContainer((MusicPlayerAccess) musicPlayer);
        centerContainer.initialize(layout);

        topContainer = new TopContainer(
            (MusicPlayerAccess) musicPlayer, window,
            file -> musicPlayer.changeSong(file),
            list -> controller.onPlaylistLoaded(list),
            () -> controller.loadNextSong(),
            () -> centerContainer.loadQueueView(musicPlayer.getPlaylist())
        );
        topContainer.initialize();

        bottomContainer = new BottomContainer(
            (MusicPlayerAccess) musicPlayer,
            this::toggleView,
            () -> controller.loadNextSong()
        );
        bottomContainer.initialize(centerContainer.getNowPlayingWrapper());

        centerContainer.setupHomePageLoadButton(this::multipleFileSelectionAndFill);
    }

    private void setupUpdateTimer() {
        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATE_DURATION), e -> musicPlayer.announceChanges());
        this.uiUpdateTimeline = new Timeline(updater);
        this.uiUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        this.uiUpdateTimeline.play();
    }

    private void assembleLayout() {
        this.layout.setBottom(this.bottomContainer.getBottomLayout());
        this.bottomContainer.getBottomLayout().setVisible(false);
        this.layout.setTop(this.topContainer.getMenuBar());
        this.layout.setCenter(this.centerContainer.getHomeNowPlayingContainer());
    }

    private void initNowPlayingView() {
        this.centerContainer.getNowPlayingWrapper().setVisible(false);
        this.centerContainer.getNowPlayingWrapper().setMouseTransparent(true);
        Platform.runLater(() -> this.centerContainer.getNowPlayingWrapper().setTranslateY(2000));
    }

    private void multipleFileSelectionAndFill() {
        List<File> selectedFiles = topContainer.multipleFileSelection();
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            controller.onPlaylistLoaded(selectedFiles);
        }
    }

    private void toggleView(Region viewToAnimate) {
        boolean opening = !isOpen;
        if (opening) {
            StackPane container = centerContainer.getHomeNowPlayingContainer();
            if (!container.getChildren().contains(viewToAnimate)) container.getChildren().add(viewToAnimate);
            viewToAnimate.setVisible(true);
            viewToAnimate.setMouseTransparent(false);
            viewToAnimate.setManaged(true);
        }

        double endValue = opening ? 0 : this.layout.getHeight();
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150), 
            new KeyValue(viewToAnimate.translateYProperty(), endValue, Interpolator.EASE_BOTH)));

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

    private void shutdown() {
        if (uiUpdateTimeline != null) uiUpdateTimeline.stop();
        if (centerContainer != null) centerContainer.shutdown();
        if (musicPlayer != null) musicPlayer.stop();
        try { DataBaseManager.getInstance().shutdown(); } catch (Exception e) {}
        window.close();
    }

    @Override
    public void stop() throws Exception {
        shutdown();
        super.stop();
    }
}
