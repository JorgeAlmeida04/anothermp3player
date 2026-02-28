package queue;

public class QueueSongData {

    public String title;
    public String artist;
    public String album;
    public String duration;
    public byte[] imageData;

    public QueueSongData(
        String title,
        String artist,
        String album,
        String duration,
        byte[] imageData
    ) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.imageData = imageData;
    }
}
