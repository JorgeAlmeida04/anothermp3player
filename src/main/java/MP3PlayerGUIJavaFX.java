import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class MP3PlayerGUIJavaFX extends Application implements Observer {

    private static final double DEFAULT_UPDATE_DURATION = 2.0;

    private MusicPlayerModel musicPlayer;

    //Global variables for the playback buttons
    private Button playPauseButton;
    private Button nextButton;
    private Button prevButton;

    //Song and volume sliders global variables
    private Slider songSlider;
    private Slider volumeSlider;

    //Global variable of the bottom bar container
    private HBox playbackBox;

    //Uploaded song file
    private File songFile;

    //Global variables for the navigation menus
    private MenuBar menuBar;
    private Menu songsMenu;
    private Menu playlistMenu;

    //Items of the menus of the navigation menus
    private MenuItem loadSong;
    private MenuItem loadPlaylist;
    private MenuItem createPlaylist;

    //Song information to display
    private Label songTitle;
    private Label songArtist;
    private Label songCover;

    //Layout for the center
    private HBox centerHBox;

    //Main Stage variables
    private Stage window;
    private Scene scene;
    private BorderPane layout;

    private void addPlaybackButtons() {
        //Creation of the playback buttons
        //and set up of the action handler
        playPauseButton = new Button();
        setImage(playPauseButton, "new-play.png");
        playPauseButton.setOnAction(e ->{
                setImage(playPauseButton, "new-pause.png");
                System.out.println("Paused");
            }
        );

        nextButton = new Button("Next");
        nextButton.setOnAction(e -> System.out.println("Next"));

        prevButton = new Button("Prev");
        prevButton.setOnAction(e -> System.out.println("Prev"));

        playbackBox.getChildren().addAll(prevButton, playPauseButton, nextButton);
    }

    private void addMenuBarItems() {
        //Creation of menu to load songs into the player
        songsMenu = new Menu("Songs");

        loadSong = new MenuItem("Load Song");
        loadSong.setOnAction(e -> {
            songFile = fileSelection();
            if(songFile != null){
                loadSong(songFile);
                this.musicPlayer.setPlaylist(null);
            }
            System.out.println("Loading song");
        });

        songsMenu.getItems().add(loadSong);

        //Creation of the menu to create and load playlists
        playlistMenu = new Menu("Playlist");

        createPlaylist = new MenuItem("Create Playlist");
        createPlaylist.setOnAction(e -> {
            System.out.println("Creating playlist");
        });

        loadPlaylist = new MenuItem("Load Playlist");
        loadPlaylist.setOnAction(e -> {
            songFile = fileSelection();
            System.out.println("Loading playlist");
        });

        playlistMenu.getItems().addAll(createPlaylist, loadPlaylist);
        menuBar.getMenus().addAll(songsMenu, playlistMenu);
    }

    private void initSongInfoVisualizer(){
        centerHBox = new HBox();
        centerHBox.setSpacing(10);
        centerHBox.setPadding(new Insets(10, 10, 0, 10));
        centerHBox.setAlignment(Pos.CENTER);
        songTitle = new Label("Song Title");
        songArtist = new Label("Song Artist");
        songCover = new Label("Song Cover");

        centerHBox.getChildren().addAll(songTitle, songArtist, songCover);
    }

    private void initVolumeSlider(){
        this.volumeSlider = new Slider(0, 0, 0);
        this.volumeSlider.setOrientation(Orientation.VERTICAL);
        this.volumeSlider.setPadding(new Insets(10, 10, 0, 10));
        this.volumeSlider.setMaxHeight(80);
        this.volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.musicPlayer.volumeChange(newValue.doubleValue());
        });
    }

    private void initSongSlider(){
        this.songSlider = new Slider(0, 0, 0);
        this.songSlider.setShowTickMarks(true);
        this.songSlider.setShowTickLabels(false);
        this.songSlider.setMajorTickUnit(60 * 44231.5636364);

        Label label = new Label();
        Popup popup = new Popup();
        popup.getContent().add(label);
        this.songSlider.setOnMouseClicked(event -> {
            this.musicPlayer.setSongPosition(((int) this.songSlider.getValue()));
        });
        this.songSlider.setOnMouseMoved(event -> {
            NumberAxis axis = (NumberAxis) songSlider.lookup(".axis");
            Point2D location = axis.sceneToLocal(event.getSceneX(), event.getSceneY());
            double mouseX = location.getX();
            double value = axis.getValueForDisplay(mouseX).doubleValue();
            if(value >= this.songSlider.getMin() && value <= this.songSlider.getMax()){
                label.setText(String.format("%d:%02d", (int)(value/44231.5636364)/60, (int)(value%44231.5636364)%60));
            }else{
                label.setText("Load A Song To Start");
            }
            popup.setAnchorX(event.getSceneX() - 5);
            popup.setAnchorY(event.getSceneY() - 20);
        });
    }

    private void setImage(ButtonBase b, String fileName){
        String resourcePath = "/assets/" + fileName;
        var inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }

        Image image = new Image(inputStream);
        b.setGraphic(new ImageView(image));
    }

    //Loads the new song file to the system
    private File fileSelection(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a mp3 file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        return fileChooser.showOpenDialog(window);
    }

    //Loads a new song. Updates the song slider and volume slider accordingly
    private void loadSong(File file){
        if(this.musicPlayer.hasClip() && this.musicPlayer.isRunning()){
            this.musicPlayer.stop();
        }
        this.musicPlayer.changeSong(file);
        this.window.setTitle(file.getName() + "~ Another MP3 Player");

        updateVolumeSlider();
        updateSongSlider();
    }

    private void loadPlaylistSong(){
        boolean wasRunning = false;
        if(this.musicPlayer.hasClip() && this.musicPlayer.isRunning()){
            this.musicPlayer.stop();
            wasRunning = true;
        }
        File song = this.musicPlayer.loadNextSong();
        this.window.setTitle(song.getName() + "~ Another MP3 Player");

        updateVolumeSlider();
        updateSongSlider();

        if(wasRunning){
            setImage(this.playPauseButton, "new-pause.png");
            this.musicPlayer.start();
        }
    }

    //Update volume slider
    private void updateVolumeSlider(){
        int MIN_VOL = (int) this.musicPlayer.getMinVolume();
        int MAX_VOL = (int) this.musicPlayer.getMaxVolume();
        int half = (MAX_VOL - MIN_VOL) / 2;
        this.volumeSlider.setMax(MAX_VOL);
        this.volumeSlider.setMin(half);
        this.volumeSlider.setValue((double) (MAX_VOL + half) / 2);
    }

    //Update song slider
    private void updateSongSlider(){
        this.songSlider.setMax(this.musicPlayer.getClipLength());
        this.songSlider.setMin(0);
        this.songSlider.setValue(0);
    }

    //Notifies the GUI to update
    private void notifyGUI(){
        this.musicPlayer.announceChanges();
    }

    @Override
    public void init(){
        this.musicPlayer = new MusicPlayerModel();
        this.musicPlayer.addObserver(this);
    }

    @Override
    public void start(Stage stage) throws IOException {
        playbackBox = new HBox(10);
        menuBar = new MenuBar();

        //Top Bar Navigation
        addMenuBarItems();

        //Playback Buttons
        addPlaybackButtons();

        //Song info visualizer
        initSongInfoVisualizer();

        //Init volume slider
        initVolumeSlider();

        //Init song slider
        initSongSlider();

        //Starts a TimeLine that automatically updates the gui every second
        //This allows for the song slider to move the song's position
        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATE_DURATION), e -> notifyGUI());
        Timeline t = new Timeline(updater);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        window = stage;
        window.setTitle("Another MP3 Player");
        window.setOnCloseRequest(e -> {
            System.out.println("Closing");
            window.close();
        });

        layout = new BorderPane();
        layout.setBottom(this.playbackBox);
        layout.setTop(this.menuBar);
        layout.setCenter(this.centerHBox);
        layout.setRight(this.volumeSlider);
        layout.setLeft(this.songSlider);

        scene = new Scene(layout, 1260, 720);

        window.setScene(scene);
        window.show();
    }

    @Override
    public void update(Observable o, Object arg) {

    }
}
