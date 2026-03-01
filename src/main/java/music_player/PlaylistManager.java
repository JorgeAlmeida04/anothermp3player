package music_player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * PlaylistManager handles the high-level logic for playlist navigation,
 * shuffling, and repeating. It keeps track of the original and active order.
 */
public class PlaylistManager {

    private List<File> activePlaylist;
    private List<File> originalPlaylist;
    private int playlistPosition = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private final Random random = new Random();

    public PlaylistManager() {}

    public void setPlaylist(List<File> playlist) {
        if (playlist == null) {
            this.activePlaylist = null;
            this.originalPlaylist = null;
            return;
        }
        this.originalPlaylist = new ArrayList<>(playlist);
        this.activePlaylist = new ArrayList<>(playlist);
        this.playlistPosition = 0;

        if (isShuffle && !this.activePlaylist.isEmpty()) {
            Collections.shuffle(this.activePlaylist, random);
        }
    }

    public void setShuffle(boolean shuffle) {
        if (this.isShuffle == shuffle || activePlaylist == null) return;
        this.isShuffle = shuffle;

        File currentSong = getCurrentSong();

        if (this.isShuffle) {
            this.originalPlaylist = new ArrayList<>(this.activePlaylist);
            Collections.shuffle(this.activePlaylist, random);
            // Maintain currently playing song index if possible
            if (currentSong != null) {
                int newIdx = this.activePlaylist.indexOf(currentSong);
                if (newIdx != -1 && newIdx != playlistPosition) {
                    Collections.swap(this.activePlaylist, playlistPosition, newIdx);
                }
            }
        } else {
            if (this.originalPlaylist != null) {
                this.activePlaylist = new ArrayList<>(this.originalPlaylist);
                if (currentSong != null) {
                    this.playlistPosition = this.activePlaylist.indexOf(currentSong);
                }
            }
        }
    }

    public void jumpToSong(File file) {
        if (this.activePlaylist == null || file == null) return;
        
        if (this.isShuffle) {
            List<File> pool = (this.originalPlaylist != null) ? this.originalPlaylist : this.activePlaylist;
            List<File> newList = new ArrayList<>(pool);
            if (newList.remove(file)) {
                Collections.shuffle(newList, random);
                newList.add(0, file);
                this.activePlaylist = newList;
                this.playlistPosition = 0;
            }
        } else {
            int newPos = this.activePlaylist.indexOf(file);
            if (newPos != -1) {
                this.playlistPosition = newPos;
            }
        }
    }

    public File next() {
        if (activePlaylist == null || activePlaylist.isEmpty()) return null;
        if (!isRepeat) {
            playlistPosition = (playlistPosition + 1) % activePlaylist.size();
        }
        return activePlaylist.get(playlistPosition);
    }

    public File previous() {
        if (activePlaylist == null || activePlaylist.isEmpty()) return null;
        playlistPosition = (playlistPosition - 1 + activePlaylist.size()) % activePlaylist.size();
        return activePlaylist.get(playlistPosition);
    }

    public File getCurrentSong() {
        if (activePlaylist != null && playlistPosition >= 0 && playlistPosition < activePlaylist.size()) {
            return activePlaylist.get(playlistPosition);
        }
        return null;
    }

    // Getters and Setters
    public List<File> getActivePlaylist() { return activePlaylist; }
    public int getPlaylistPosition() { return playlistPosition; }
    public void setPlaylistPosition(int pos) { this.playlistPosition = pos; }
    public boolean isShuffle() { return isShuffle; }
    public boolean isRepeat() { return isRepeat; }
    public void setRepeat(boolean repeat) { this.isRepeat = repeat; }
}
