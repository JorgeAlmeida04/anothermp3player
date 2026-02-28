package model;

import java.time.LocalDateTime;

public class Song {

    private int id;
    private String filePath;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private long durationMs;
    private Integer year;
    private LocalDateTime dateAdded;
    private LocalDateTime lastPlayed;

    public Song() {}

    public Song(String filePath, String title, String artist, String album, long durationMs) {
        this.filePath = filePath;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public long getDurationMs() { return durationMs; }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getYear() { return year; }

    public void setYear(Integer year) { this.year = year; }

    public LocalDateTime getDateAdded() { return dateAdded; }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastPlayed() { return lastPlayed; }

    public void setLastPlayed(LocalDateTime lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getFormattedDurationMs() {
        long totalSec = durationMs / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    public String toString() {
        return String.format(
                "%s - %s (%s)", artist, title, getFormattedDurationMs()
        );
    }
}
