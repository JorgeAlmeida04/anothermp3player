import domain.Song;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
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

        musicPlayer = new MusicPlayer();
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
        JSlider playbackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        playbackSlider.setBounds(getWidth()/2 - 300/2, 365, 300, 40);
        playbackSlider.setBackground(null);
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
        playlistMenu.add(createPlaylist);

        JMenuItem loadPlaylist = new JMenuItem("Load Playlist");
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
        playbackButtons.add(nextButton);

        add(playbackButtons);
    }

    private void updateTitleAndArtist(Song song){
        songTitle.setText(song.getSongTitle());
        songArtist.setText(song.getSongArtist());
    }

    private void enablePause(){
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

    private void enablePlay(){
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
