package music_player;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import javax.sound.sampled.*;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import queue.QueueSongData;

/**
 * MusicPlayerModel is the core audio engine.
 * It manages the low-level javax.sound.sampled.Clip and handles decoding.
 * High-level playlist logic is delegated to PlaylistManager.
 */
public class MusicPlayerModel implements MusicPlayerAccess {

    public static final String EVENT_PLAYER_STATE = "playerState";
    public static final String EVENT_TRACK_CHANGED = "trackChanged";
    public static final String EVENT_PLAYBACK_STATE = "playbackState";
    public static final String EVENT_PLAYLIST_CHANGED = "playlistChanged";
    public static final String EVENT_PLAYLIST_POSITION_CHANGED = "playlistPositionChanged";
    public static final String EVENT_VOLUME_CHANGED = "volumeChanged";

    private Clip clip;
    private final PlaylistManager playlistManager = new PlaylistManager();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    // Current Track Metadata
    private String currentTitle;
    private String currentArtist;
    private byte[] currentAlbumImage;
    private double currentVolume = 50.0;

    public MusicPlayerModel() {}

    // --- Audio Engine Core ---

    @Override
    public void changeSong(File mp3) {
        try {
            closeClip();
            extractMetadata(mp3);

            AudioInputStream audioStream = getAudioInputStream(mp3);
            if (audioStream == null) return;

            AudioFormat format = audioStream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat decodeFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16,
                    format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false
                );
                audioStream = AudioSystem.getAudioInputStream(decodeFormat, audioStream);
            }

            this.clip = AudioSystem.getClip();
            this.clip.open(audioStream);
            audioStream.close();

            volumeChange(this.currentVolume);
            propertyChangeSupport.firePropertyChange(EVENT_TRACK_CHANGED, null, mp3);
            propertyChangeSupport.firePropertyChange(EVENT_PLAYER_STATE, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeClip() {
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
            this.clip = null;
        }
    }

    @Override
    public void start() {
        if (hasClip() && !this.clip.isRunning()) {
            this.clip.start();
            propertyChangeSupport.firePropertyChange(EVENT_PLAYBACK_STATE, false, true);
        }
    }

    @Override
    public void stop() {
        if (hasClip() && this.clip.isRunning()) {
            this.clip.stop();
            propertyChangeSupport.firePropertyChange(EVENT_PLAYBACK_STATE, true, false);
        }
    }

    @Override
    public void volumeChange(double sliderValue) {
        double old = this.currentVolume;
        this.currentVolume = sliderValue;
        if (hasClip()) {
            FloatControl gain = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
            float volumePercentage = (float) sliderValue / 100.0f;
            if (volumePercentage <= 0.0001f) {
                gain.setValue(gain.getMinimum());
            } else {
                float dB = (float) (Math.log10(volumePercentage) * 20.0);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            }
        }
        propertyChangeSupport.firePropertyChange(EVENT_VOLUME_CHANGED, old, currentVolume);
    }

    // --- Delegation to PlaylistManager ---

    @Override public File initPlaylist() { return loadAndFire(playlistManager.getCurrentSong()); }
    @Override public File loadNextSong() { return loadAndFire(playlistManager.next()); }
    @Override public File loadPreviousSong() { return loadAndFire(playlistManager.previous()); }

    private File loadAndFire(File song) {
        if (song != null) {
            int oldPos = playlistManager.getPlaylistPosition();
            changeSong(song);
            propertyChangeSupport.firePropertyChange(EVENT_PLAYLIST_POSITION_CHANGED, oldPos, playlistManager.getPlaylistPosition());
        }
        return song;
    }

    @Override public void setPlaylist(List<File> list) { 
        playlistManager.setPlaylist(list); 
        propertyChangeSupport.firePropertyChange(EVENT_PLAYLIST_CHANGED, null, list);
    }
    @Override public void setPlaylistPosition(int pos) { playlistManager.setPlaylistPosition(pos); }
    @Override public int getPlaylistPosition() { return playlistManager.getPlaylistPosition(); }
    @Override public List<File> getPlaylist() { return playlistManager.getActivePlaylist(); }
    
    @Override public void jumpToSong(File file) { 
        playlistManager.jumpToSong(file); 
        // Notify UI that the list order might have changed (Shuffle mode)
        propertyChangeSupport.firePropertyChange(EVENT_PLAYLIST_CHANGED, null, getPlaylist());
        // Notify UI that the selection index has moved
        propertyChangeSupport.firePropertyChange(EVENT_PLAYLIST_POSITION_CHANGED, -1, getPlaylistPosition());
    }

    @Override public boolean isShuffle() { return playlistManager.isShuffle(); }
    @Override public void setShuffle(boolean s) { 
        boolean old = playlistManager.isShuffle();
        playlistManager.setShuffle(s); 
        propertyChangeSupport.firePropertyChange("shuffle", old, s);
        propertyChangeSupport.firePropertyChange(EVENT_PLAYLIST_CHANGED, null, getPlaylist());
    }
    @Override public boolean isRepeat() { return playlistManager.isRepeat(); }
    @Override public void setRepeat(boolean r) { 
        boolean old = playlistManager.isRepeat();
        playlistManager.setRepeat(r); 
        propertyChangeSupport.firePropertyChange("repeat", old, r);
    }

    // --- Info & State ---

    @Override public boolean hasClip() { return clip != null; }
    @Override public boolean isRunning() { return hasClip() && (clip.isRunning() || atEnd()); }
    @Override public boolean atEnd() { return hasClip() && clip.getFramePosition() >= clip.getFrameLength() - 100; }
    @Override public boolean hasPlaylist() { return playlistManager.getActivePlaylist() != null; }
    @Override public String getSongTitle() { return currentTitle; }
    @Override public String getSongArtist() { return currentArtist; }
    @Override public byte[] getSongAlbumImage() { return currentAlbumImage; }
    @Override public double getCurrentVolume() { return currentVolume; }
    @Override public int getClipLength() { return hasClip() ? clip.getFrameLength() : 0; }
    @Override public int getClipCurrentValue() { return hasClip() ? clip.getFramePosition() : 0; }
    @Override public long getPlaybackPositionMs() { return hasClip() ? clip.getMicrosecondPosition() / 1000L : 0; }

    public void rewindToStart() { if (hasClip()) clip.setFramePosition(0); }
    public void setSongPosition(int pos) { if (hasClip()) clip.setFramePosition(pos); }

    public void announceChanges() { propertyChangeSupport.firePropertyChange(EVENT_PLAYER_STATE, null, null); }
    public void addPropertyChangeListener(PropertyChangeListener l) { propertyChangeSupport.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { propertyChangeSupport.removePropertyChangeListener(l); }

    // --- Metadata Utilities ---

    private void extractMetadata(File file) {
        this.currentTitle = file.getName();
        this.currentArtist = "Unknown Artist";
        this.currentAlbumImage = null;
        try {
            Mp3File mp3 = new Mp3File(file);
            if (mp3.hasId3v2Tag()) {
                ID3v2 tag = mp3.getId3v2Tag();
                if (tag.getTitle() != null) currentTitle = tag.getTitle();
                if (tag.getArtist() != null) currentArtist = tag.getArtist();
                currentAlbumImage = tag.getAlbumImage();
            }
        } catch (Exception e) {}
    }

    @Override
    public QueueSongData getQueueData(File file) {
        // Try to get metadata from database first (fast path)
        QueueSongData dbData = getQueueDataFromDatabase(file);
        if (dbData != null) {
            // Still need to get album art from file (not stored in DB)
            byte[] img = extractAlbumImage(file);
            return new QueueSongData(dbData.title, dbData.artist, dbData.album, dbData.duration, img);
        }
        
        // Fall back to parsing MP3 file (slow path)
        return getQueueDataFromFile(file);
    }
    
    /**
     * Attempts to retrieve metadata from the database.
     * Returns null if not found or on error.
     */
    private QueueSongData getQueueDataFromDatabase(File file) {
        try {
            services.SongService songService = new services.SongService();
            Optional<model.Song> opt = songService.getSongByPath(file.getAbsolutePath());
            if (opt.isPresent()) {
                model.Song song = opt.get();
                String title = song.getTitle() != null && !song.getTitle().isBlank() 
                    ? song.getTitle() : file.getName();
                String artist = song.getArtist() != null && !song.getArtist().isBlank() 
                    ? song.getArtist() : "Unknown Artist";
                String album = song.getAlbum() != null && !song.getAlbum().isBlank() 
                    ? song.getAlbum() : "Unknown Album";
                long durationMs = song.getDurationMs();
                String duration = String.format("%d:%02d", durationMs / 60000, (durationMs % 60000) / 1000);
                return new QueueSongData(title, artist, album, duration, null);
            }
        } catch (Exception e) {
            // Silently fall back to file parsing
        }
        return null;
    }
    
    /**
     * Extracts only the album image from the MP3 file.
     * This is faster than parsing all metadata when DB has the rest.
     */
    private byte[] extractAlbumImage(File file) {
        try {
            Mp3File mp3 = new Mp3File(file);
            if (mp3.hasId3v2Tag()) {
                return mp3.getId3v2Tag().getAlbumImage();
            }
        } catch (Exception e) {}
        return null;
    }
    
    /**
     * Full metadata extraction from MP3 file (fallback when DB doesn't have the song).
     */
    private QueueSongData getQueueDataFromFile(File file) {
        String title = file.getName();
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        String duration = "0:00";
        byte[] img = null;
        try {
            Mp3File mp3 = new Mp3File(file);
            long sec = mp3.getLengthInSeconds();
            duration = String.format("%d:%02d", sec / 60, sec % 60);
            if (mp3.hasId3v2Tag()) {
                ID3v2 tag = mp3.getId3v2Tag();
                title = (tag.getTitle() != null) ? tag.getTitle() : title;
                artist = (tag.getArtist() != null) ? tag.getArtist() : artist;
                album = (tag.getAlbum() != null) ? tag.getAlbum() : album;
                img = tag.getAlbumImage();
            }
        } catch (Exception e) {}
        return new QueueSongData(title, artist, album, duration, img);
    }

    private AudioInputStream getAudioInputStream(File file) throws Exception {
        try { return AudioSystem.getAudioInputStream(file); } 
        catch (UnsupportedAudioFileException e) { return decodeMp3(file); }
    }

    private AudioInputStream decodeMp3(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
            Header h = bitstream.readFrame();
            int sampleRate = h.frequency();
            int channels = (h.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
            while (h != null) {
                SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                short[] buffer = sb.getBuffer();
                for (int i = 0; i < sb.getBufferLength(); i++) {
                    short s = buffer[i];
                    out.write(s & 0xff);
                    out.write((s >> 8) & 0xff);
                }
                bitstream.closeFrame();
                h = bitstream.readFrame();
            }
            byte[] data = out.toByteArray();
            return new AudioInputStream(new ByteArrayInputStream(data), 
                new AudioFormat(sampleRate, 16, channels, true, false), 
                data.length / (channels * 2));
        }
    }
}
