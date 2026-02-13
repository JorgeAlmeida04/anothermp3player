package queue;

public class QueueSongData {
    public String title;
    public String artist;
    public String duration;
    public byte[] imageData;

    public QueueSongData(String title, String artist, String duration, byte[] imageData) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.imageData = imageData;
    }
}


