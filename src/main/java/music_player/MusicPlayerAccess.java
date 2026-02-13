package music_player;

import queue.QueueSongData;

import java.io.File;

public interface MusicPlayerAccess {
    boolean hasClip();
    boolean isRunning();
    boolean atEnd();
    boolean hasPlaylist();
    void start();
    void stop();
    void volumeChange(double value);
    void setSongPosition(int position);
    int getClipLength();
    int getClipCurrentValue();
    String getSongTitle();
    String getSongArtist();
    byte[] getSongAlbumImage();
    File loadNextSong();
    File loadPreviousSong();
    File initPlaylist();
    void changeSong(File file);
    void setPlaylistPosition(int position);
    QueueSongData getQueueData(File file);
}
