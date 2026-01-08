import java.util.Set;
import javax.swing.*;

public class MP3PlayerGUI {

    public MP3PlayerGUI() {
        //This will call the JFrame constructor to configure the header title to "Another MP3 Player"
        super("Another MP3 Player");
        //Set the width and height of the window
        setSize(720, 1260);

        //End process when app is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Lauch the app at the center of the screen
        setLocationRelativeTo(null);

        //uncomment the next line to prevent the app from being resized
        // setResizable(false);

        //Set layout to null which allows us to control the (x,y) coordinates of the components
        // and also set the height and width
        setLayout(null);
    }
}
