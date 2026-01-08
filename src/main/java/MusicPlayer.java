import domain.Song;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class MusicPlayer extends PlaybackListener {
    //Used to update isPaused more synchronously
    private static final Object playSignal = new Object();

    private MP3PlayerGUI mp3PlayerGUI;

    private Song currentSong;
    public Song getCurrentSong(){
        return currentSong;
    }

    //Usage of JLayer library to create an AdvancedPlayer obj which will handle playing the music
    private AdvancedPlayer advancedPlayer;

    //Boolean flag to trigger the pause of the song
    private boolean isPaused;

    //Stores the last frame when the playback is finished (used for pausing and resuming)
    private int currentFrame;

    public void setCurrentFrame(int frame){
        currentFrame = frame;
    }

    //Track how many milliseconds has passed since playing the song (used for updating the slider)
    private int currentTimeInMill;

    public void setCurrentTimeInMill(int time){
        currentTimeInMill = time;
    }

    public MusicPlayer(MP3PlayerGUI mp3PlayerGUI){
        this.mp3PlayerGUI = mp3PlayerGUI;
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
        if(currentSong == null) return;

        try{
            //Read MP3 Audio Data
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            //Create a new advanced player
            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            //Start Music
            startMusicThread();

            //Start playback slider thread
            startPlaybackSliderThread();

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
                    if(isPaused){
                        synchronized (playSignal){
                            //Update flag
                            isPaused = false;

                            //Notify the other thread
                            playSignal.notify();
                        }

                        //Resume music
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);

                    }else {
                        //Play music
                        advancedPlayer.play();
                    }
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    //Create a thread that will handle updating the slider
    private void startPlaybackSliderThread() throws Exception{
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(isPaused){
                    try{
                        //Wait till it gets notified by other thread to continue
                        // makes sure that isPaused boolean flag updates to false before continuing
                        synchronized (playSignal){
                            playSignal.wait();
                        }
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }

                while(!isPaused){
                    try{
                        //Increment current time milliseconds
                        currentTimeInMill++;

                        //Calculate into frame value
                        int calculatedFrame = (int) ((double) currentTimeInMill * 1.8 * currentSong.getFrameRatePerMilliseconds());

                        //Update GUI
                        mp3PlayerGUI.setPlaybackSliderValue(calculatedFrame);

                        //Mimic 1 millisecond using thread.sleep
                        Thread.sleep(1);
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
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

        if(isPaused) {
            currentFrame += (int)  ((double) evt.getFrame() * currentSong.getFrameRatePerMilliseconds());
        }
    }
}
