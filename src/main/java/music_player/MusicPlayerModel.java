package music_player;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.Observable;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import queue.QueueSongData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

public class MusicPlayerModel extends Observable implements MusicPlayerAccess {
    private Clip clip;
    private AudioInputStream audioStream;
    private AudioInputStream decodeStream;
    private AudioFormat audioFormat;
    private AudioFormat decodeFormat;
    private List<File> playlist;
    private int playlistPosition;
    private String currentTitle;
    private String currentArtist;
    private byte[] currentAlbumImage;

    public MusicPlayerModel() {
        this.clip = null;
        this.audioStream = null;
        this.audioFormat = null;
        this.decodeFormat = null;
        this.decodeStream = null;
        this.playlist = null;
    }

    //Change the song loaded onto the clip
    public void changeSong(File mp3){
        try{
            extractMetadata(mp3);
            this.audioStream = getAudioInputStream(mp3);
            this.audioFormat = audioStream.getFormat();

            // If the format is already PCM_SIGNED and compatible, use it directly
            // Otherwise, try to convert (legacy logic for WAVs that might be different)
            if (this.audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED &&
                this.audioFormat.getSampleSizeInBits() == 16 &&
                !this.audioFormat.isBigEndian()) {
                 this.decodeStream = this.audioStream;
            } else {
                this.decodeFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        audioFormat.getSampleRate(),
                        16,
                        audioFormat.getChannels(),
                        audioFormat.getChannels() * 2,
                        audioFormat.getSampleRate(),
                        false
                );
                this.decodeStream = AudioSystem.getAudioInputStream(decodeFormat, audioStream);
            }
            
            this.clip = AudioSystem.getClip();
            this.clip.open(decodeStream);
            this.clip.setFramePosition(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            announceChanges();
        }
    }

    private void extractMetadata(File file){
        try{
            Mp3File mp3File = new Mp3File(file);

            //Reset Defaults
            this.currentTitle = file.getName();
            this.currentArtist = "Unknown Artist";
            this.currentAlbumImage = null;

            if(mp3File.hasId3v2Tag()){
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                this.currentTitle = id3v2Tag.getTitle();
                this.currentArtist = id3v2Tag.getArtist();
                this.currentAlbumImage = id3v2Tag.getAlbumImage();
            }else if(mp3File.hasId3v1Tag()){
                //ID3v1 does not support images
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                this.currentTitle = id3v1Tag.getTitle();
                this.currentArtist = id3v1Tag.getArtist();
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
        FileInputStream fis = new FileInputStream(file);
        Bitstream bitstream = new Bitstream(fis);
        Decoder decoder = new Decoder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Header h = bitstream.readFrame();
        if (h == null) {
            return null;
        }

        int sampleRate = h.frequency();
        int channels = (h.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;

        int frames = 0;
        while (h != null) {
            SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            short[] buffer = sb.getBuffer();
            int len = sb.getBufferLength();
            for (int i = 0; i < len; i++) {
                short s = buffer[i];
                out.write(s & 0xff);
                out.write((s >> 8) & 0xff);
            }
            bitstream.closeFrame();
            frames++;
            h = bitstream.readFrame();
        }
        bitstream.close();

        byte[] audioData = out.toByteArray();
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        return new AudioInputStream(new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize());
    }

    //Starts the playlist
    public File initPlaylist(){
        if(this.playlist != null && !this.playlist.isEmpty()){
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Loads the next song in the playlist
    public File loadNextSong(){
        if(this.playlist != null && !this.playlist.isEmpty()){
            this.playlistPosition++;
            if(this.playlistPosition >= this.playlist.size()){
                this.playlistPosition = 0;
            }
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Loads the previous song on the playlist
    public File loadPreviousSong(){
        if(this.playlist != null && !this.playlist.isEmpty()){
            this.playlistPosition--;
            if(this.playlistPosition < 0){
                this.playlistPosition = this.playlist.size() - 1;
            }
            File song = this.playlist.get(this.playlistPosition);
            changeSong(song);
            return song;
        }
        return null;
    }

    //Starts a song from its current position
    public void start(){
        if(hasClip() && !this.clip.isRunning()){
            this.clip.start();
        }
    }

    //Pauses the clip
    public void stop(){
        if(hasClip() && this.clip.isRunning()){
            this.clip.stop();
        }
    }

    //Changes the clips loudness
    public void volumeChange(double sliderValue) {
        if (hasClip()) {
            FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);

            //Convert Slider (0-100) to a Percentage (0.0 to 1.0)
            float volumePercentage = (float) sliderValue / 100.0f;

            //Handle 0 separately (Math.log10(0) is -Infinity)
            if (volumePercentage <= 0.0001f) {
                // Mute completely
                gainControl.setValue(gainControl.getMinimum());
                return;
            }

            //Convert Percentage to Decibels (Logarithmic Scale)
            // Formula: 20 * log10(amplitude)
            float dB = getDB(volumePercentage, gainControl);

            gainControl.setValue(dB);
        }
    }

    private static float getDB(float volumePercentage, FloatControl gainControl) {
        float dB = (float) (Math.log10(volumePercentage) * 20.0);

        // Clamp the value just in case (optional but safe)
        // Usually min is -80.0dB and max is 6.0dB
        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();

        // Ensure we don't go below the hardware minimum
        if (dB < min) dB = min;

        // Ensure we don't exceed the hardware maximum (usually +6dB, but 0dB is safer for "100%")
        if (dB > max) dB = max;

        return dB;
    }

    //Rewinds the clip to the start
    public void rewindToStart(){
        if(hasClip()){
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(0);
            if(prevRun){
                this.clip.start();
            }
        }
    }

    //Sets the clip's position to the new value
    public void setSongPosition(int position){
        if(hasClip()){
            boolean prevRun = this.clip.isRunning();
            this.clip.stop();
            this.clip.setFramePosition(position);
            if(prevRun){
                this.clip.start();
            }
        }
    }

    public String getSongTitle(){
        return  this.currentTitle;
    }

    public String getSongArtist(){
        return  this.currentArtist;
    }

    public byte[]  getSongAlbumImage(){
        return  this.currentAlbumImage;
    }

    //Extracts the songs metadata for the queue
    public String[] getSongMetadata(File file){
        String title = file.getName();
        String artist = "Unknown Artist";
        try{
            Mp3File mp3File = new Mp3File(file);
            if(mp3File.hasId3v2Tag()){
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                if (id3v2Tag.getTitle() != null) title = id3v2Tag.getTitle();
                if (id3v2Tag.getArtist() != null) artist = id3v2Tag.getArtist();
            } else if (mp3File.hasId3v1Tag()){
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                if (id3v1Tag.getTitle() != null) title = id3v1Tag.getTitle();
                if (id3v1Tag.getArtist() != null) artist = id3v1Tag.getArtist();
            }
        }catch(Exception e){
            //Fallback to defaults
            e.printStackTrace();
        }
        return new String[]{title, artist};
    }

    //Sets the playlist to the new list
    public void setPlaylist(List<File> playlist){
        this.playlist = playlist;
        this.playlistPosition = 0;
    }

    //Changes the playlist song (triggered by the user)
    public void setPlaylistPosition(int position){
        this.playlistPosition = position;
    }

    //Gets the playlist
    public List<File> getPlaylist(){
        return this.playlist;
    }

    //Gets the playlist position
    public int  getPlaylistPosition(){
        return this.playlistPosition;
    }

    //Gets the minimum decibel volume of the clip
    public double getMinVolume(){
        FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        return gainControl.getMinimum();
    }

    //Gets the maximum decibel volume of the clip
    public double getMaxVolume(){
        FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        return gainControl.getMaximum();
    }

    //Gets the total length of the current clip
    //Implies that there is a current song stored in this.clip
    public int getClipLength(){
        return this.clip.getFrameLength();
    }

    //Gets the current position of the song
    //Implies that there is a current song stores in this.clip
    public int getClipCurrentValue(){
        return this.clip.getFramePosition();
    }

    //Checks if the song is at the end
    public boolean atEnd(){
        return this.clip.getFramePosition() >= this.clip.getFrameLength() - 100;
    }

    //Returns the state of the music player
    public boolean isRunning(){
        return hasClip() && (this.clip.isRunning() || atEnd());
    }

    //Checks if the music player has a current song
    public boolean hasClip(){
        return this.clip != null;
    }

    //Checks whether there is a current playlist
    public boolean hasPlaylist(){
        return this.playlist != null;
    }

    public void announceChanges(){
        setChanged();
        notifyObservers();
    }

    public QueueSongData getQueueData(File file) {
        String title = file.getName();
        String artist = "Unknown Artist";
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
                imageData = id3v2Tag.getAlbumImage();
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                if (id3v1Tag.getTitle() != null) title = id3v1Tag.getTitle();
                if (id3v1Tag.getArtist() != null) artist = id3v1Tag.getArtist();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new QueueSongData(title, artist, duration, imageData);
    }
}
