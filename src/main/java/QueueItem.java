import javafx.scene.image.Image;


import java.io.File;

public class QueueItem {
    File file;
    String title;
    String artist;
    String duration;
    Image image;

    public QueueItem(File file, String title, String artist, String duration, Image image) {
        this.file = file;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.image = image;
    }
}

