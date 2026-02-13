package containers;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import music_player.MusicPlayerAccess;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class TopContainer {

    private MenuBar menuBar;
    private Menu songsMenu;
    private Menu playlistMenu;

    private MenuItem loadSong;
    private MenuItem loadPlaylist;
    private MenuItem createPlaylist;

    private final MusicPlayerAccess musicPlayer;
    private final Stage window;
    private final Consumer<File> onSongLoaded;
    private final Consumer<List<File>> onPlaylistLoaded;
    private final Runnable loadPlaylistSongCallback;
    private final Runnable loadQueueViewCallback;

    public TopContainer(MusicPlayerAccess musicPlayer,
                        Stage window,
                        Consumer<File> onSongLoaded,
                        Consumer<List<File>> onPlaylistLoaded,
                        Runnable loadPlaylistSongCallback,
                        Runnable loadQueueViewCallback) {
        this.musicPlayer = musicPlayer;
        this.window = window;
        this.onSongLoaded = onSongLoaded;
        this.onPlaylistLoaded = onPlaylistLoaded;
        this.loadPlaylistSongCallback = loadPlaylistSongCallback;
        this.loadQueueViewCallback = loadQueueViewCallback;
    }

    public void initialize() {
        menuBar = new MenuBar();
        addMenuBarItems();
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public List<File> multipleFileSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select mp3 files");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        return fileChooser.showOpenMultipleDialog(window);
    }

    private void addMenuBarItems() {
        songsMenu = new Menu("Songs");

        loadSong = new MenuItem("Load Song");
        loadSong.setOnAction(e -> {
            File songFile = fileSelection();
            if (songFile != null) {
                if (onSongLoaded != null) {
                    onSongLoaded.accept(songFile);
                }
                this.musicPlayer.setPlaylistPosition(0);
                this.musicPlayer.start();
            }
        });

        songsMenu.getItems().add(loadSong);

        playlistMenu = new Menu("Playlist");

        createPlaylist = new MenuItem("Create Playlist");
        createPlaylist.setOnAction(e -> {
            System.out.println("Creating playlist");
        });

        loadPlaylist = new MenuItem("Load Playlist");
        loadPlaylist.setOnAction(e -> {
            List<File> playlist = multipleFileSelection();
            if (playlist != null && !playlist.isEmpty()) {
                if (onPlaylistLoaded != null) {
                    onPlaylistLoaded.accept(playlist);
                }
                if (loadPlaylistSongCallback != null) {
                    loadPlaylistSongCallback.run();
                }
                if (loadQueueViewCallback != null) {
                    loadQueueViewCallback.run();
                }
            }
        });

        playlistMenu.getItems().addAll(createPlaylist, loadPlaylist);
        menuBar.getMenus().addAll(songsMenu, playlistMenu);
    }

    private File fileSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a mp3 file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Music"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 File", "*.mp3"),
                new FileChooser.ExtensionFilter("All File", "*.*")
        );
        return fileChooser.showOpenDialog(window);
    }
}
