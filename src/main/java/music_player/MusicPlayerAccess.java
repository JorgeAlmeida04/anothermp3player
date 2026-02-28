package music_player;

import java.io.File;
import java.util.List;
import queue.QueueSongData;

public interface MusicPlayerAccess {
    boolean hasClip();
    boolean isRunning();
    boolean atEnd();
    boolean hasPlaylist();
    void start();
    void stop();
    void volumeChange(double value);
    double getCurrentVolume();
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
    int getPlaylistPosition();
    List<File> getPlaylist();
    QueueSongData getQueueData(File file);
}
