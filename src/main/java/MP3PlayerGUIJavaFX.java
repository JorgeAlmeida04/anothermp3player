import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class MP3PlayerGUIJavaFX extends Application implements Observer {

    private static final double DEFAULT_UPDATE_DURATION = 2.0;

    private MusicPlayerModel musicPlayer;

    //Global variables for the playback buttons
    private Button pauseButton;
    private Button playButton;
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

    //Main Stage variables
    private Stage window;
    private Scene scene;
    private BorderPane layout;

    private void addPlaybackButtons() {
        //Creation of the playback buttons
        //and set up of the action handler
        pauseButton = new Button();
        setImage(pauseButton, "new-pause.png");
        pauseButton.setOnAction(e ->{
                    System.out.println("Paused");
                    playButton.setVisible(true);
                    pauseButton.setVisible(false);
                }
        );

        playButton = new Button();
        setImage(playButton, "new-play.png");
        playButton.setOnAction(e  -> {
            System.out.println("Playing");
            playButton.setVisible(false);
            pauseButton.setVisible(true);
        });

        nextButton = new Button("Next");
        nextButton.setOnAction(e -> System.out.println("Next"));

        prevButton = new Button("Prev");
        prevButton.setOnAction(e -> System.out.println("Prev"));

        playbackBox.getChildren().addAll(prevButton, pauseButton, playButton, nextButton);
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
    public void loadSong(File file){
        if(this.musicPlayer.hasClip() && this.musicPlayer.isRunning()){
            this.musicPlayer.stop();
        }
        this.musicPlayer.changeSong(file);
        this.window.setTitle(file.getName() + "~ Another MP3 Player");
        int MIN_VOL = (int) this.musicPlayer.getMinVolume();
        int MAX_VOL = (int) this.musicPlayer.getMaxVolume();

        //Update volume slider
        int half = (MAX_VOL - MIN_VOL) / 2;
        this.volumeSlider.setMax(MAX_VOL);
        this.volumeSlider.setMin(half);
        this.volumeSlider.setValue((double) (MAX_VOL + half) / 2);

        //Update song slider
        this.songSlider.setMax(this.musicPlayer.getClipLength());
        this.songSlider.setMin(0);
        this.songSlider.setValue(0);
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

        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATE_DURATION), e -> System.out.println("Updating"));

        window = stage;
        window.setTitle("Another MP3 Player");
        window.setOnCloseRequest(e -> {
            System.out.println("Closing");
            window.close();
        });

        layout = new BorderPane();
        layout.setBottom(playbackBox);
        layout.setTop(menuBar);
        playButton.setVisible(false);

        scene = new Scene(layout, 1260, 720);

        window.setScene(scene);
        window.show();
    }

    @Override
    public void update(Observable o, Object arg) {

    }
}
