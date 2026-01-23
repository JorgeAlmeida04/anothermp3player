import domain.Song;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.*;
import java.util.ArrayList;

public class MusicPlayer extends PlaybackListener {
    //Used to update isPaused more synchronously
    private static final Object playSignal = new Object();

    private MP3PlayerGUI mp3PlayerGUI;

    private Song currentSong;
    public Song getCurrentSong(){
        return currentSong;
    }

    private ArrayList<Song> playlist;

    //Keep track of the index of the playlist
    private int currentPlaylistIndex;

    //Usage of JLayer library to create an AdvancedPlayer obj which will handle playing the music
    private AdvancedPlayer advancedPlayer;

    //Boolean flag to trigger the pause of the song
    private boolean isPaused;

    //Boolean flag used to tell when the song has finished
    private boolean songFinished;

    private boolean pressedNext, pressedPrevious;

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
        playlist = null;

        //Stop song if possible
        if(!songFinished) {
            stopSong();
        }

        //Play the current song if not null
        if(currentSong != null){
            //Reset frame
            currentFrame = 0;

            //Reset current time in milli
            currentTimeInMill = 0;

            //Update GUI
            mp3PlayerGUI.setPlaybackSliderValue(0);

            playCurrentSong();
        }
    }

    public void loadPlaylist(File playlistFile) throws Exception{
        playlist = new ArrayList<>();

        //Store the paths from the text file into the playlist ArrayList
        try{
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //Read each line from the text file and store the text into the songPath variable
            String songPath;
            while((songPath = bufferedReader.readLine()) != null){
                //Create song object based on song path
                Song song = new Song(songPath);

                //Add to playlist array list
                playlist.add(song);
            }
        }catch (Exception e){
            throw new Exception(e);
        }

        if(!playlist.isEmpty()){
            //Reset playback slider
            mp3PlayerGUI.setPlaybackSliderValue(0);
            currentTimeInMill = 0;

            //Update current song to the first song in the playlist
            currentSong = playlist.getFirst();

            //Start from the beginning frame
            currentFrame = 0;

            //Update GUI
            mp3PlayerGUI.enablePause();
            mp3PlayerGUI.updateTitleAndArtist(currentSong);
            mp3PlayerGUI.updatePlaybackSlider(currentSong);

            //Start song
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

                while(!isPaused && !songFinished && !pressedNext && !pressedPrevious){
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

    public void nextSong() throws Exception{
        //No need to go to the next song if there is no playlist
        if(playlist == null) return;

        if(currentPlaylistIndex + 1 > playlist.size() - 1) return;

        pressedNext = true;

        //Stop song if possible
        if(!songFinished) {
            stopSong();
        }

        //Increase current playlist index
        currentPlaylistIndex++;

        //Update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //Reset frame
        currentFrame = 0;

        //Reset current time in milli
        currentTimeInMill = 0;

        //Update GUI
        mp3PlayerGUI.enablePause();
        mp3PlayerGUI.updateTitleAndArtist(currentSong);
        mp3PlayerGUI.updatePlaybackSlider(currentSong);

        //Play song
        playCurrentSong();
    }

    public void previousSong() throws Exception{
        //No need to go to the next song if there is no playlist
        if(playlist == null) return;

        pressedPrevious = true;

        //Stop song if possible
        if(!songFinished) {
            stopSong();
        }

        //Check if there is any previous songs (aka index > 0)
        if(currentPlaylistIndex > 0){
            //Decrement current playlist index
            currentPlaylistIndex--;
        }

        //Update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //Reset frame
        currentFrame = 0;

        //Reset current time in milli
        currentTimeInMill = 0;

        //Update GUI
        mp3PlayerGUI.enablePause();
        mp3PlayerGUI.updateTitleAndArtist(currentSong);
        mp3PlayerGUI.updatePlaybackSlider(currentSong);

        //Play song
        playCurrentSong();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        //This method gets called in the beginning of the song
        System.out.println("Playback Started");
        songFinished = false;
        pressedPrevious = false;
        pressedNext = false;
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        //This method gets called when the song finishes or if the player gets closed
        System.out.println("Playback Finished");
        try {
            if (isPaused) {
                currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMilliseconds());
            } else {
                //If user pressed next or prev we don't need to execute the rest of the code
                if(pressedPrevious || pressedNext) return;

                //When the song ends
                songFinished = true;

                if (playlist == null) {
                    //Update GUI
                    mp3PlayerGUI.enablePlay();
                } else {
                    //Last song in the playlist
                    if (currentPlaylistIndex == playlist.size() - 1) {
                        mp3PlayerGUI.enablePlay();
                    } else {
                        //Go to the next song on the playlist
                        nextSong();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
