import domain.Song;

import javax.swing.*;

public class AnotherMp3PlayerApplication {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
                try {
                    new MP3PlayerGUI().setVisible(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
		});
	}

}
