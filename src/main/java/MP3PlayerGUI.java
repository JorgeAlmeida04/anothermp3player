import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutionException;

public class MP3PlayerGUI extends JFrame {
    //Color Configuration
    public static final Color FRAME_COLOR = Color.BLACK;
    public static final Color TEXT_COLOR = Color.WHITE;

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

        addGuiComponents();
    }

    private void addGuiComponents() throws Exception{
        addToolbar();

        //Load record image
        JLabel songImage = new JLabel(loadImage("src/main/java/assets/record.png"));
        songImage.setBounds(0, 50, getWidth() - 20, 225);
        add(songImage);

        //Song title
        JLabel songTitle = new JLabel("Song Title");
        songTitle.setBounds(0, 285, getWidth()-10, 30);
        songTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        songTitle.setForeground(TEXT_COLOR);
        songTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitle);

        //Song Artist
        JLabel songArtist = new JLabel("Artist");
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

    private void addToolbar(){
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
        JPanel playbackButtons = new JPanel();
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
        playbackButtons.add(playButton);

        //Pause Button
        JButton pauseButton = new JButton((loadImage("src/main/java/assets/pause.png")));
        pauseButton.setBorderPainted(false);
        pauseButton.setBackground(null);
        pauseButton.setVisible(false);
        playbackButtons.add(pauseButton);

        //Next Button
        JButton nextButton = new JButton(loadImage("src/main/java/assets/next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setBackground(null);
        playbackButtons.add(nextButton);

        add(playbackButtons);
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
