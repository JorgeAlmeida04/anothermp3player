import domain.Song;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

public class MP3PlayerGUI extends JFrame {
    //Color Configuration
    public static final Color FRAME_COLOR = Color.BLACK;
    public static final Color TEXT_COLOR = Color.WHITE;

    private MusicPlayer musicPlayer;

    //Allow us to use file explorer
    private JFileChooser jFileChooser;

    private JLabel songTitle, songArtist, songImage;
    private JPanel playbackButtons;
    private JSlider playbackSlider;

    public MP3PlayerGUI() throws Exception{
        //This will call the JFrame constructor to configure the header title to "Another MP3 Player"
        super("Another MP3 Player");
        //Set the width and height of the window
        setSize(400, 800);

        //End process when app is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Lauch the app at the center of the screen
        setLocationRelativeTo(null);

        //uncomment the next line to prevent the app from being resized
        //setResizable(false);

        //Set layout to null which allows us to control the (x,y) coordinates of the components
        // and also set the height and width
        setLayout(null);

        //Change the frame color
        getContentPane().setBackground(FRAME_COLOR);

        musicPlayer = new MusicPlayer(this);
        jFileChooser = new JFileChooser();

        //Set a default path for file explorer
        jFileChooser.setCurrentDirectory(new File("src/main/java/assets"));

        //Filter file picker to only show .mp3 files
        jFileChooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));

        addGuiComponents();
    }

    private void addGuiComponents() throws Exception{
        addToolbar();

        //Load record image
        songImage = new JLabel(loadImage("src/main/java/assets/record.png"));
        songImage.setBounds(0, 50, getWidth() - 20, 225);
        add(songImage);

        //Song title
        songTitle = new JLabel("Song Title");
        songTitle.setBounds(0, 285, getWidth()-10, 30);
        songTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        songTitle.setForeground(TEXT_COLOR);
        songTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitle);

        //Song Artist
        songArtist = new JLabel("Artist");
        songArtist.setBounds(0, 315, getWidth()-10, 30);
        songArtist.setFont(new Font("Dialog", Font.PLAIN, 24));
        songArtist.setForeground(TEXT_COLOR);
        songArtist.setHorizontalAlignment(SwingConstants.CENTER);
        add(songArtist);

        //Playback Slider
        playbackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        playbackSlider.setBounds(getWidth()/2 - 300/2, 365, 300, 40);
        playbackSlider.setBackground(null);
        playbackSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                //When the user is holding the tick we want to pause the song
                musicPlayer.pauseSong();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    //When the user drops the tick
                    JSlider source = (JSlider) e.getSource();

                    //Get the frame value from where the user wants to playback to
                    int frame = source.getValue();

                    //Update the current frame in the music player to this frame
                    musicPlayer.setCurrentFrame(frame);

                    //Update current time in milliseconds as well
                    musicPlayer.setCurrentTimeInMill((int) (frame / (1.8 * musicPlayer.getCurrentSong().getFrameRatePerMilliseconds())));

                    //Resume the song
                    musicPlayer.playCurrentSong();

                    //Toggle on pause button and toggle off play button
                    enablePause();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        add(playbackSlider);

        //Playback buttons
        addPlaybackButtons();

    }

    private void addToolbar() throws Exception{
        JToolBar toolbar = new JToolBar();
        toolbar.setBounds(0, 0, getWidth(), 20);

        //Prevent toolbar from being moved
        toolbar.setFloatable(false);

        //Add drop menu
        JMenuBar menuBar = new JMenuBar();
        toolbar.add(menuBar);

        //loading song menu option
        JMenu songMenu = new JMenu("Song");
        menuBar.add(songMenu);

        //Add the "load song" item in the song menu
        JMenuItem loadSong = new JMenuItem("Load Song");
        loadSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //The integer will let us know the user's selected option
                    int result = jFileChooser.showOpenDialog(MP3PlayerGUI.this);
                    File selectedFile = jFileChooser.getSelectedFile();

                    //Checks if the selectedFile is not empty and if the user choose the "open" button
                    if(selectedFile != null && result == JFileChooser.APPROVE_OPTION){

                        //Create a song obj based on selected file
                        Song song = new Song(selectedFile.getPath());

                        //Load song in music player

                        musicPlayer.loadSong(song);

                        //Update Song Title and Artist
                        updateTitleAndArtist(song);

                        //Update Playback Slider
                        updatePlaybackSlider(song);

                        //Toggle on pause button and toggle off play button
                        enablePause();
                    }
                }catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        songMenu.add(loadSong);

        //Add Playlist menu
        JMenu playlistMenu = new JMenu("Playlist");
        menuBar.add(playlistMenu);

        //Items for the playlist menu
        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        createPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Load music playlist dialogue
                new MusicPlaylistDialog(MP3PlayerGUI.this).setVisible(true);
            }
        });
        playlistMenu.add(createPlaylist);

        JMenuItem loadPlaylist = new JMenuItem("Load Playlist");
        loadPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser jFileChooser = new JFileChooser();
                    jFileChooser.setFileFilter(new FileNameExtensionFilter("Playlist", "txt"));
                    jFileChooser.setCurrentDirectory(new File("src/main/java/assets"));

                    int result = jFileChooser.showOpenDialog(MP3PlayerGUI.this);
                    File selectedFile = jFileChooser.getSelectedFile();

                    if(result == JFileChooser.APPROVE_OPTION){
                        //Stop music
                        musicPlayer.stopSong();

                        //Load Playlist
                        musicPlayer.loadPlaylist(selectedFile);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        playlistMenu.add(loadPlaylist);

        add(toolbar);
    }

    private void addPlaybackButtons() throws Exception{
        playbackButtons = new JPanel();
        playbackButtons.setBounds(0, 435, getWidth()-10, 80);
        playbackButtons.setBackground(null);

        //Previous button
        JButton prevButton = new JButton(loadImage("src/main/java/assets/previous.png"));
        prevButton.setBorderPainted(false);
        prevButton.setBackground(null);
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    //Go to the previous song
                    musicPlayer.previousSong();
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        });
        playbackButtons.add(prevButton);

        //Play Button
        JButton playButton = new JButton((loadImage("src/main/java/assets/play.png")));
        playButton.setBorderPainted(false);
        playButton.setBackground(null);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //Toggle off play button and toggle on pause button
                    enablePause();

                    //Play or resume the song
                    musicPlayer.playCurrentSong();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        playbackButtons.add(playButton);

        //Pause Button
        JButton pauseButton = new JButton((loadImage("src/main/java/assets/pause.png")));
        pauseButton.setBorderPainted(false);
        pauseButton.setBackground(null);
        pauseButton.setVisible(false);
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Toggle off pause button and toggle on play button
                enablePlay();

                //Pause song
                musicPlayer.pauseSong();
            }
        });
        playbackButtons.add(pauseButton);

        //Next Button
        JButton nextButton = new JButton(loadImage("src/main/java/assets/next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setBackground(null);
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //Go to the next song
                    musicPlayer.nextSong();
                }catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        playbackButtons.add(nextButton);

        add(playbackButtons);
    }

    //Method used to update the slider from the music player class
    public void setPlaybackSliderValue(int frame){
        playbackSlider.setValue(frame);
    }

    public void updateTitleAndArtist(Song song){
        songTitle.setText(song.getSongTitle());
        songArtist.setText(song.getSongArtist());
        //Set and scale the album cover
        songImage.setIcon(new ImageIcon(song.getSongCover().getScaledInstance(getWidth()/2, getHeight()/3, Image.SCALE_SMOOTH)));
    }

    public void updatePlaybackSlider(Song song){
        //Update max count for slider
        playbackSlider.setMaximum(song.getMp3File().getFrameCount());

        //Song length label
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        //Beginning will be 00:00
        JLabel labelBeginning = new JLabel(("00:00"));
        labelBeginning.setFont(new Font("Dialog", Font.BOLD, 18));
        labelBeginning.setForeground(TEXT_COLOR);

        //End will vary depending on the song
        JLabel labelEnd = new JLabel(song.getSongLength());
        labelEnd.setFont(new Font("Dialog", Font.BOLD, 18));
        labelEnd.setForeground(TEXT_COLOR);

        labelTable.put(0, labelBeginning);
        labelTable.put(song.getMp3File().getFrameCount(), labelEnd);

        playbackSlider.setLabelTable(labelTable);
        playbackSlider.setPaintLabels(true);
    }

    public void enablePause(){
        //Retrieve reference to play and pause buttons from playbackButtons panel
        JButton playBtn = (JButton) playbackButtons.getComponent(1);
        JButton pauseBtn = (JButton) playbackButtons.getComponent(2);

        //Turn off play button
        playBtn.setVisible(false);
        playBtn.setEnabled(false);

        //Turn on pause button
        pauseBtn.setVisible(true);
        pauseBtn.setEnabled(true);

    }

    public void enablePlay(){
        //Retrieve reference to play and pause buttons from playbackButtons panel
        JButton playBtn = (JButton) playbackButtons.getComponent(1);
        JButton pauseBtn = (JButton) playbackButtons.getComponent(2);

        //Turn on play button
        playBtn.setVisible(true);
        playBtn.setEnabled(true);

        //Turn off pause button
        pauseBtn.setVisible(false);
        pauseBtn.setEnabled(false);

    }

    private ImageIcon loadImage(String imagePath) throws Exception{
        try{
            //Read the image file from the given path
            BufferedImage image = ImageIO.read(new File(imagePath));

            //Returns an image icon so that the component can render the image
            return new ImageIcon(image);
        }catch(Exception e){
            throw new Exception(e);
        }
    }
}
