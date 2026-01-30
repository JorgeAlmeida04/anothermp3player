import eu.iamgio.animated.binding.Animated;
import eu.iamgio.animated.binding.AnimationSettings;
import eu.iamgio.animated.binding.presets.AnimatedScale;
import eu.iamgio.animated.binding.property.animation.AnimationProperty;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import net.yetihafen.javafx.customcaption.CustomCaption;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Function;

public class MP3PlayerGUIJavaFX extends Application implements Observer {

    private static final double DEFAULT_UPDATE_DURATION = 2.0;

    private MusicPlayerModel musicPlayer;

    //Global variables for the playback buttons
    private Button playPauseButton;
    private Button nextButton;
    private Button prevButton;

    //Song and volume sliders global variables
    private Slider volumeSlider;
    private ProgressBar volumeBar;
    private StackPane volumeSliderBar;

    //Sons slider variables
    private Slider songSlider;
    private ProgressBar songBar;
    private StackPane songSliderBar;

    //Bottom container (it will have the buttons container and the song slider)
    private GridPane bottomLayout;

    //Buttons container
    private GridPane playbackBox;

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
    private ImageView songCover;

    //Container for the song labels
    private VBox labelVBox;

    //Container for the center of the main stage
    private GridPane coverQueueContainer; //This grid will contain the album cover and the ListView os the queue
    private TableView<String> queueLyricsTable; //This table will have a view for the queue and the lyrics of the song(switchable)


    //Main Stage variables
    private Stage window;
    private Scene scene;
    private BorderPane layout;

    private void initBottomLayout() throws FileNotFoundException {
        this.bottomLayout = new GridPane();
        this.bottomLayout.getStyleClass().add("bottom-layout");
        this.bottomLayout.setHgap(1);
        this.bottomLayout.setMaxWidth(Double.MAX_VALUE);
        this.bottomLayout.setAlignment(Pos.CENTER);
        this.bottomLayout.setPadding(new Insets(5, 5, 5, 5));

        //Configure the constraints for the columns on the bottomLayout grid
        ColumnConstraints column1 = new ColumnConstraints();

        column1.setHgrow(Priority.ALWAYS);
        column1.setFillWidth(true);

        this.bottomLayout.getColumnConstraints().addAll(column1);

        //Init playback buttons container
        this.playbackBox = new GridPane();
        this.playbackBox.setVgap(2);
        this.bottomLayout.setMaxWidth(Double.MAX_VALUE);
        this.playbackBox.setAlignment(Pos.CENTER);
        this.playbackBox.setPadding(new Insets(5, 5, 5, 5));

        //Configure the constraints for the playbackBox grid
        ColumnConstraints col1 = new ColumnConstraints(), col2 = new ColumnConstraints(), col3 = new ColumnConstraints();

        col1.setPercentWidth(25);
        col1.setHalignment(HPos.LEFT); // Keep buttons to the left

        col2.setPercentWidth(50);
        col2.setHalignment(HPos.CENTER); // Center the text perfectly

        col3.setPercentWidth(25);
        col3.setHalignment(HPos.RIGHT); // Keep volume slider to the right

        playbackBox.getColumnConstraints().addAll(col1, col2, col3);

        //Config playback buttons
        addPlaybackButtons();

        //Config song slider
        initSongSlider();

        //Config volume slider
        initVolumeSlider();

        //Config song info
        initSongInfoVisualizer();

        this.playbackBox.add(labelVBox, 1, 0);
        this.playbackBox.add(volumeSliderBar, 2, 0);

        this.bottomLayout.add(songSliderBar, 0, 0);
        this.bottomLayout.add(playbackBox,0,1);

        this.bottomLayout.setOnMouseClicked(event -> {
            if(this.songCover.getScaleX() == 1 && this.songCover.getScaleY() == 1) {
                this.songCover.setScaleX(0.5);
                this.songCover.setScaleY(0.5);
            }else{
                this.songCover.setScaleX(1);
                this.songCover.setScaleY(1);
            }
        });

    }

    private void addPlaybackButtons() {
        //Creation of the playback buttons
        //and set up of the action handler
        playPauseButton = new Button();
        setImage(playPauseButton, "new-play.png");
        playPauseButton.setOnAction(e ->{
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
            if (this.musicPlayer.hasPlaylist()) {
                loadPlaylistSong();
            }
        });

        prevButton = new Button();
        setImage(prevButton, "prev.png");
        prevButton.setOnAction(e -> {
            if (this.musicPlayer.hasPlaylist()) {
                boolean wasRunning = this.musicPlayer.isRunning();
                this.musicPlayer.stop();
                File song = this.musicPlayer.loadPreviousSong();
                if (song != null) {
                    this.window.setTitle(song.getName() + " ~ Another MP3 Player");
                    updateVolumeSlider();
                    updateSongSlider();
                    updateSongLabels();
                    if (wasRunning) {
                        this.musicPlayer.start();
                        setImage(this.playPauseButton, "new-pause.png");
                    }
                }
            }
        });

        //Creation of the buttons specific containers
        FlowPane playbackButtons = new FlowPane();

        playbackButtons.setHgap(5);
        playbackButtons.setAlignment(Pos.CENTER);
        playbackButtons.setPadding(new Insets(5, 5, 5, 5));
        playbackButtons.getChildren().addAll(prevButton, playPauseButton, nextButton);

        playbackBox.add(playbackButtons, 0, 0);
    }

    private void addMenuBarItems() {
        //Creation of menu to load songs into the player
        songsMenu = new Menu("Songs");

        loadSong = new MenuItem("Load Song");
        loadSong.setOnAction(e -> {
            fileSelection();
            System.out.println(this.songFile.getName());
            if(this.songFile != null){
                loadSong();
                this.musicPlayer.setPlaylist(null);
                this.musicPlayer.start();
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
            multipleFileSelection();
            System.out.println("Loading playlist");
        });

        playlistMenu.getItems().addAll(createPlaylist, loadPlaylist);
        menuBar.getMenus().addAll(songsMenu, playlistMenu);
    }

    private void initSongInfoVisualizer() throws FileNotFoundException {
        labelVBox = new VBox();
        labelVBox.setSpacing(10);
        labelVBox.setPadding(new Insets(10, 10, 0, 10));
        labelVBox.setAlignment(Pos.CENTER);

        //Set up song title
        songTitle = new Label("Song Title");
        songTitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.REGULAR, 25));

        //Set up song artist
        songArtist = new Label("Song Artist");
        songArtist.setFont(Font.font("Comic Sans MS", FontWeight.SEMI_BOLD, FontPosture.REGULAR, 16));

        setSongCover();

        labelVBox.getChildren().addAll(songTitle, songArtist);
    }

    private void initVolumeSlider(){
        this.volumeBar = new ProgressBar(0);
        this.volumeBar.getStyleClass().add("volume-bar");
        this.volumeBar.setMaxWidth(Double.MAX_VALUE);

        this.volumeSlider = new Slider(0, 100, 10);
        this.volumeSlider.setOrientation(Orientation.HORIZONTAL);
        this.volumeSlider.setMaxHeight(80);
        this.volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.musicPlayer.volumeChange(newValue.doubleValue());
        });
        this.volumeSlider.getStyleClass().add("volume-slider");

        setupSliderPopup(this.volumeSlider, value -> String.format("%.0f%%", value));

        // Binds Progress = SliderValue / SliderMax
        this.volumeBar.progressProperty().bind(
                this.volumeSlider.valueProperty().divide(this.volumeSlider.maxProperty())
        );

        this.volumeSliderBar = new StackPane();
        this.volumeSliderBar.getChildren().addAll(this.volumeBar, this.volumeSlider);
        this.volumeSliderBar.setAlignment(Pos.CENTER);

    }

    private void initSongSlider(){
        this.songBar = new ProgressBar(0.0);
        this.songBar.getStyleClass().add("song-bar");
        this.songBar.setMaxWidth(Double.MAX_VALUE);

        this.songSlider = new Slider(0, 100, 0);
        this.songSlider.setShowTickMarks(false);
        this.songSlider.setShowTickLabels(false);
        this.songSlider.setMajorTickUnit(60 * 44231.5636364);
        this.songSlider.setOrientation(Orientation.HORIZONTAL);
        this.songSlider.getStyleClass().add("song-slider");

        setupSliderPopup(this.songSlider, hoverValue -> {
            if (this.songSlider.getMax() > 0) {
                double timeUnit = 44231.5636364; // Your specific unit
                int minutes = (int) (hoverValue / timeUnit) / 60;
                int seconds = (int) (hoverValue / timeUnit) % 60;
                return String.format("%d:%02d", minutes, seconds);
            }
            return "0:00";
        });

        this.songSlider.setOnMouseClicked(event -> {
            this.musicPlayer.setSongPosition(((int) this.songSlider.getValue()));
        });

        //Bind the progress bar to the song slider
        this.songBar.progressProperty().bind(
                this.songSlider.valueProperty().divide(this.songSlider.maxProperty())
        );

        this.songSliderBar = new StackPane();
        this.songSliderBar.getChildren().addAll(songBar, songSlider);
        this.songSliderBar.setAlignment(Pos.CENTER);
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

    //Set initial cover
    private void setSongCover() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("src/main/resources/assets/miku.jpg");
        Image image = new Image(inputStream);
        this.songCover = new ImageView(image);
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
    private void fileSelection(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a mp3 file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        this.songFile = fileChooser.showOpenDialog(window);
    }

    private void multipleFileSelection(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a mp3 file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(window);

        if(files != null){
            this.musicPlayer.setPlaylist(files);
            loadPlaylistSong();
        }

    }

    //Loads a new song. Updates the song slider and volume slider accordingly
    private void loadSong(){
        if(this.musicPlayer.hasClip() && this.musicPlayer.isRunning()){
            this.musicPlayer.stop();
        }
        this.musicPlayer.changeSong(this.songFile);

        if(this.musicPlayer.hasClip()) {
            this.window.setTitle(this.songFile.getName() + "~ Another MP3 Player");
            updateSongLabels();
            updateVolumeSlider();
            updateSongSlider();
        }
    }

    private void loadPlaylistSong(){
        boolean wasRunning = false;
        if(this.musicPlayer.hasClip() && this.musicPlayer.isRunning()){
            this.musicPlayer.stop();
            wasRunning = true;
        }
        File song = this.musicPlayer.loadNextSong();
        if(song != null && this.musicPlayer.hasClip()) {
            this.window.setTitle(song.getName() + "~ Another MP3 Player");
            updateSongLabels();
            updateVolumeSlider();
            updateSongSlider();
            this.musicPlayer.start();

            if (wasRunning) {
                setImage(this.playPauseButton, "new-pause.png");
                this.musicPlayer.start();
            }
        }
    }

    //Update song labels
    private void updateSongLabels(){
        this.songTitle.setText(this.musicPlayer.getSongTitle());
        this.songArtist.setText(this.musicPlayer.getSongArtist());
        ByteArrayInputStream bis =new  ByteArrayInputStream(this.musicPlayer.getSongAlbumImage());
        this.songCover.setImage(new Image(bis, 400, 400, true, true));
    }

    //Update volume slider
    private void updateVolumeSlider(){
        this.volumeSlider.setMin(0);
        this.volumeSlider.setMax(100);
        this.volumeSlider.setValue(10);
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
    public void start(Stage stage) throws Exception {
        menuBar = new MenuBar();

        //Top Bar Navigation
        addMenuBarItems();

        //Bottom container config
        initBottomLayout();

        //Starts a TimeLine that automatically updates the gui every second
        //This allows for the song slider to move the song's position
        KeyFrame updater = new KeyFrame(Duration.seconds(DEFAULT_UPDATE_DURATION), e -> notifyGUI());
        Timeline t = new Timeline(updater);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        window = stage;
        window.setTitle("No Song Selected ~ Another MP3 Player");
        window.setOnCloseRequest(e -> {
            System.out.println("Closing");
            window.close();
        });

        Animated animatedCover = new Animated(this.songCover, new AnimatedScale());

        layout = new BorderPane();
        layout.setBottom(this.bottomLayout);
        this.bottomLayout.setVisible(false);
        layout.setTop(this.menuBar);
        layout.setCenter(animatedCover);

        scene = new Scene(layout, 1260, 720);
        scene.getStylesheets().add("Default_Theme.css");

        window.setScene(scene);
        window.show();

        //CustomCaption.setCaptionColor(this.window, Color.BLACK);
        CustomCaption.setImmersiveDarkMode(this.window, true);
    }

    @Override
    public void update(Observable o, Object arg) {
        //Make sure play button is in sync
        if(this.musicPlayer.isRunning() && !this.musicPlayer.atEnd()){
            setImage(playPauseButton, "new-pause.png");
        }else{
            setImage(playPauseButton, "new-play.png");
        }
        if(this.musicPlayer.hasClip()){
            this.bottomLayout.setVisible(true);
            if(this.musicPlayer.atEnd()){
                if(this.musicPlayer.hasPlaylist()){
                    loadPlaylistSong();
                }else{
                    setImage(playPauseButton, "miku.jpg");
                    this.bottomLayout.setVisible(false);
                }
            }
            //Update slider based on current song position
            this.songSlider.setValue(this.musicPlayer.getClipCurrentValue());
        }
    }
}
