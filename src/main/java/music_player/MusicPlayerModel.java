package music_player;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Observable;
import javax.sound.sampled.*;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import queue.QueueSongData;

public class MusicPlayerModel extends Observable implements MusicPlayerAccess {

    private Clip clip;
    private List<File> playlist;
    private int playlistPosition;
    private String currentTitle;
    private String currentArtist;
    private byte[] currentAlbumImage;
    private double currentVolume = 10.0;

    public MusicPlayerModel() {
        this.clip = null;
        this.playlist = null;
    }

    //Change the song loaded onto the clip
    public void changeSong(File mp3) {
        try {
            // Close previous clip and streams to prevent resource leaks
            closeClip();

            extractMetadata(mp3);
            AudioInputStream audioStream = getAudioInputStream(mp3);
            AudioFormat audioFormat = audioStream.getFormat();

            AudioInputStream decodeStream;
            // If the format is already PCM_SIGNED and compatible, use it directly
            // Otherwise, try to convert (legacy logic for WAVs that might be different)
            if (
                audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED &&
                audioFormat.getSampleSizeInBits() == 16 &&
                !audioFormat.isBigEndian()
            ) {
                decodeStream = audioStream;
            } else {
                AudioFormat decodeFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    audioFormat.getSampleRate(),
                    16,
                    audioFormat.getChannels(),
                    audioFormat.getChannels() * 2,
                    audioFormat.getSampleRate(),
                    false
                );
                decodeStream = AudioSystem.getAudioInputStream(
                    decodeFormat,
                    audioStream
                );
            }

            this.clip = AudioSystem.getClip();
            this.clip.open(decodeStream);
            this.clip.setFramePosition(0);

            // Close streams after clip has been opened (data is buffered in clip)
            decodeStream.close();
            if (decodeStream != audioStream) {
                audioStream.close();
            }

            volumeChange(this.currentVolume);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            announceChanges();
        }
    }

    /**
     * Closes the current clip and releases its audio resources.
     * Must be called before loading a new song to prevent resource leaks.
     */
    private void closeClip() {
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
            this.clip = null;
        }
    }

    /**
     * Extracts ID3 metadata (title, artist, album image) from an MP3 file.
     * Falls back to filename/defaults if tags are missing.
     */
    private void extractMetadata(File file) {
        try {
            Mp3File mp3File = new Mp3File(file);

            //Reset Defaults
            this.currentTitle = file.getName();
            this.currentArtist = "Unknown Artist";
            this.currentAlbumImage = null;

            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                if (id3v2Tag.getTitle() != null) this.currentTitle =
                    id3v2Tag.getTitle();
                if (id3v2Tag.getArtist() != null) this.currentArtist =
                    id3v2Tag.getArtist();
                this.currentAlbumImage = id3v2Tag.getAlbumImage();
            } else if (mp3File.hasId3v1Tag()) {
                //ID3v1 does not support images
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                if (id3v1Tag.getTitle() != null) this.currentTitle =
                    id3v1Tag.getTitle();
                if (id3v1Tag.getArtist() != null) this.currentArtist =
                    id3v1Tag.getArtist();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioInputStream getAudioInputStream(File file) throws Exception {
        try {
            return AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            return decodeMp3(file);
        }
    }

    private AudioInputStream decodeMp3(File file) throws Exception {
        // Use try-with-resources to ensure FileInputStream is closed on error
        try (FileInputStream fis = new FileInputStream(file)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();
            // Pre-size the buffer to reduce array copies (~10MB initial capacity for typical MP3)
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);

            Header h = bitstream.readFrame();
            if (h == null) {
                bitstream.close();
                return null;
            }

            int sampleRate = h.frequency();
            int channels = (h.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;

            // Reusable byte buffer to avoid per-sample write calls
            byte[] frameBytes = new byte[0];

            while (h != null) {
                SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(
                    h,
                    bitstream
                );
                short[] buffer = sb.getBuffer();
                int len = sb.getBufferLength();

                // Ensure byte buffer is large enough
                int needed = len * 2;
                if (frameBytes.length < needed) {
                    frameBytes = new byte[needed];
                }

                // Convert shorts to little-endian bytes in bulk
                for (int i = 0; i < len; i++) {
                    short s = buffer[i];
                    frameBytes[i * 2] = (byte) (s & 0xff);
                    frameBytes[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                }
                out.write(frameBytes, 0, needed);

                bitstream.closeFrame();
                h = bitstream.readFrame();
            }
            bitstream.close();

            byte[] audioData = out.toByteArray();
            AudioFormat format = new AudioFormat(
                sampleRate,
                16,
                channels,
                true,
                false
            );
            return new AudioInputStream(
                new ByteArrayInputStream(audioData),
                format,
                audioData.length / format.getFrameSize()
            );
        }
    }

    //Starts the playlist
    public File initPlaylist() {
        if (this.playlist != null && !this.playlist.isEmpty()) {
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Loads the next song in the playlist
    public File loadNextSong() {
        if (this.playlist != null && !this.playlist.isEmpty()) {
            this.playlistPosition++;
            if (this.playlistPosition >= this.playlist.size()) {
                this.playlistPosition = 0;
            }
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Loads the previous song on the playlist
    public File loadPreviousSong() {
        if (this.playlist != null && !this.playlist.isEmpty()) {
            this.playlistPosition--;
            if (this.playlistPosition < 0) {
                this.playlistPosition = this.playlist.size() - 1;
            }
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Starts a song from its current position
    public void start() {
        if (hasClip() && !this.clip.isRunning()) {
            this.clip.start();
        }
    }

    //Pauses the clip
    public void stop() {
        if (hasClip() && this.clip.isRunning()) {
            this.clip.stop();
        }
    }

    //Changes the clips loudness
    public void volumeChange(double sliderValue) {
        this.currentVolume = sliderValue;
        if (hasClip()) {
            FloatControl gainControl = (FloatControl) this.clip.getControl(
                FloatControl.Type.MASTER_GAIN
            );

            //Convert Slider (0-100) to a Percentage (0.0 to 1.0)
            float volumePercentage = (float) sliderValue / 100.0f;

            //Handle 0 separately (Math.log10(0) is -Infinity)
            if (volumePercentage <= 0.0001f) {
                // Mute completely
                gainControl.setValue(gainControl.getMinimum());
                return;
            }

            //Convert Percentage to Decibels (Logarithmic Scale)
            float dB = calculateDB(volumePercentage, gainControl);
            gainControl.setValue(dB);
        }
    }

    /**
     * Converts a linear volume percentage to decibels, clamped to hardware limits.
     */
    private static float calculateDB(
        float volumePercentage,
        FloatControl gainControl
    ) {
        float dB = (float) (Math.log10(volumePercentage) * 20.0);

        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();

        return Math.max(min, Math.min(max, dB));
    }

    //Rewinds the clip to the start
    public void rewindToStart() {
        if (hasClip()) {
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(0);
            if (prevRun) {
                this.clip.start();
            }
        }
    }

    //Sets the clip's position to the new value
    public void setSongPosition(int position) {
        if (hasClip()) {
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(position);
            if (prevRun) {
                this.clip.start();
            }
        }
    }

    public String getSongTitle() {
        return this.currentTitle;
    }

    public String getSongArtist() {
        return this.currentArtist;
    }

    public byte[] getSongAlbumImage() {
        return this.currentAlbumImage;
    }

    //Sets the playlist to the new list
    public void setPlaylist(List<File> playlist) {
        this.playlist = playlist;
        this.playlistPosition = 0;
    }

    //Changes the playlist song (triggered by the user)
    public void setPlaylistPosition(int position) {
        this.playlistPosition = position;
    }

    //Gets the playlist
    public List<File> getPlaylist() {
        return this.playlist;
    }

    //Gets the playlist position
    public int getPlaylistPosition() {
        return this.playlistPosition;
    }

    //Gets the total length of the current clip
    public int getClipLength() {
        return this.clip.getFrameLength();
    }

    //Gets the current position of the song
    public int getClipCurrentValue() {
        return this.clip.getFramePosition();
    }

    //Checks if the song is at the end
    public boolean atEnd() {
        return this.clip.getFramePosition() >= this.clip.getFrameLength() - 100;
    }

    //Returns the state of the music player
    public boolean isRunning() {
        return hasClip() && (this.clip.isRunning() || atEnd());
    }

    //Checks if the music player has a current song
    public boolean hasClip() {
        return this.clip != null;
    }

    //Checks whether there is a current playlist
    public boolean hasPlaylist() {
        return this.playlist != null;
    }

    public double getCurrentVolume() {
        return this.currentVolume;
    }

    public void announceChanges() {
        setChanged();
        notifyObservers();
    }

    /**
     * Extracts metadata from an MP3 file for queue display.
     * Consolidates the previously duplicated getSongMetadata() and getQueueData() methods.
     */
    public QueueSongData getQueueData(File file) {
        String title = file.getName();
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        String duration = "0:00";
        byte[] imageData = null;

        try {
            Mp3File mp3File = new Mp3File(file);
            long seconds = mp3File.getLengthInSeconds();
            long min = seconds / 60;
            long sec = seconds % 60;
            duration = String.format("%d:%02d", min, sec);

            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                if (id3v2Tag.getTitle() != null) title = id3v2Tag.getTitle();
                if (id3v2Tag.getArtist() != null) artist = id3v2Tag.getArtist();
                if (id3v2Tag.getAlbum() != null) album = id3v2Tag.getAlbum();
                imageData = id3v2Tag.getAlbumImage();
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                if (id3v1Tag.getTitle() != null) title = id3v1Tag.getTitle();
                if (id3v1Tag.getArtist() != null) artist = id3v1Tag.getArtist();
                if (id3v1Tag.getAlbum() != null) album = id3v1Tag.getAlbum();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new QueueSongData(title, artist, album, duration, imageData);
    }
}
