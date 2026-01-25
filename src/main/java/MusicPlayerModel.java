import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import java.util.Observable;

public class MusicPlayerModel extends Observable {
    private Clip clip;
    private AudioInputStream audioStream;
    private AudioInputStream decodeStream;
    private AudioFormat audioFormat;
    private AudioFormat decodeFormat;
    private List<File> playlist;
    private int playlistPosition;

    public MusicPlayerModel() {}

    //Change the song loaded onto the clip
    public void changeSong(File mp3){
        try{
            this.audioStream = AudioSystem.getAudioInputStream(mp3);
            this.audioFormat = audioStream.getFormat();
            this.decodeFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    audioFormat.getSampleRate(),
                    16,
                    audioFormat.getChannels(),
                    audioFormat.getChannels()*2,
                    audioFormat.getSampleRate(),
                    false
            );
            this.decodeStream = AudioSystem.getAudioInputStream(decodeFormat, audioStream);
            this.clip = AudioSystem.getClip();
            this.clip.open(decodeStream);
            this.clip.setFramePosition(0);
        } catch(Exception e) {
            System.out.println("Failed to load audio");
        } finally {
            announceChanges();
        }
    }

    //Loads the next song in the playlist
    public File loadNextSong(){
        if(this.playlist != null){
            changeSong(this.playlist.get(this.playlistPosition++));
            File song = this.playlist.get(this.playlistPosition);
            this.playlistPosition++;
            //Verifies if we are at the end of the playlist
            if(this.playlistPosition >= this.playlist.size()){
                this.playlistPosition = 0;
            }
            return song;
        }
        return null;
    }

    //Loads the previous song on the playlist
    public File loadPreviousSong(){
        if(this.playlist != null){
            changeSong(this.playlist.get(this.playlistPosition--));
            File song = this.playlist.get(this.playlistPosition);
            this.playlistPosition--;
            //Verifies if we are at the start of the playlist
            if(this.playlistPosition < 0){
                this.playlistPosition = this.playlist.size() - 1;
            }
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
            if(decibels == (getMaxVolume() + getMinVolume()) / 2){
                gainControl.setValue((float) this.getMinVolume());
            }else{
                gainControl.setValue((float) decibels);
            }
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
        return hasClip() && this.clip.isRunning() || atEnd();
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
