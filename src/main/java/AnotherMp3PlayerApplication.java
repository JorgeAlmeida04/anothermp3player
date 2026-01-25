import javax.swing.*;
import javafx.application.Application;

public class AnotherMp3PlayerApplication {
	public static void main(String[] args) {
        Application.launch(MP3PlayerGUIJavaFX.class, args);

		/* SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
                try {
                    new MP3PlayerGUI().setVisible(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
		}); */
	}
}
