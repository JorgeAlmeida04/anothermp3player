import domain.Song;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class MusicPlayer extends PlaybackListener {

    private Song currentSong;

    //Usage of JLayer library to create an AdvancedPlayer obj which will handle playing the music
    private AdvancedPlayer advancedPlayer;

    //Boolean flag to trigger the pause of the song
    private boolean isPaused;

    public MusicPlayer(){

    }

    public void loadSong(Song song) throws Exception{
        currentSong = song;

        //Play the current song if not null
        if(currentSong != null){
            playCurrentSong();
        }
    }

    public void pauseSong(){
        if(advancedPlayer != null){
            //Update the pause flag
            isPaused = true;

            //Stop the player
            stopSong();
        }
    }

    public void stopSong(){
        if(advancedPlayer != null){
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }

    public void playCurrentSong() throws Exception{
        try{
            //Read MP3 Audio Data
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            //Create a new advanced player
            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            //Start Music
            startMusicThread();

        }catch (Exception e) {
            throw new Exception(e);
        }
    }

    //Thread that will handle playing the music
    private void startMusicThread() throws Exception{
        new  Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //Play music
                    advancedPlayer.play();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        //This method gets called in the beginning of the song
        System.out.println("Playback Started");
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        //This method gets called when the song finishes or if the player gets closed
        System.out.println("Playback Finished");
    }
}
