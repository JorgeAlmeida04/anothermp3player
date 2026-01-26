import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.Observable;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

public class MusicPlayerModel extends Observable {
    private Clip clip;
    private AudioInputStream audioStream;
    private AudioInputStream decodeStream;
    private AudioFormat audioFormat;
    private AudioFormat decodeFormat;
    private List<File> playlist;
    private int playlistPosition;

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
    public void volumeChange(double decibels){
        if(hasClip()){
            FloatControl gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();

            if (decibels < min) {
                decibels = min;
            } else if (decibels > max) {
                decibels = max;
            }
            
            gainControl.setValue((float) decibels);
        }
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

    //Sets the playlist to the new list
    public void setPlaylist(List<File> playlist){
        this.playlist = playlist;
        this.playlistPosition = 0;
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
}
