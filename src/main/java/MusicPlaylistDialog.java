import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class MusicPlaylistDialog extends JDialog {
    private MP3PlayerGUI mp3PlayerGUI;

    private ArrayList<String> songPaths;

    public MusicPlaylistDialog(MP3PlayerGUI mp3PlayerGUI){
        this.mp3PlayerGUI = mp3PlayerGUI;
        songPaths = new ArrayList<>();

        //Configure Dialog
        setTitle("Create Playlist");
        setSize(400, 400);
        setResizable(false);
        getContentPane().setBackground(MP3PlayerGUI.FRAME_COLOR);
        setLayout(null);
        setModal(true); //This property makes it so that the dialog has to be closed to give focus
        setLocationRelativeTo(mp3PlayerGUI);

        addDialogComponents();
    }

    private void addDialogComponents(){
        //Container to hold each song path
        JPanel songContainer = new JPanel();
        songContainer.setLayout(new BoxLayout(songContainer, BoxLayout.Y_AXIS));
        songContainer.setBounds((int)(getWidth() * 0.025), 10, (int)(getWidth() * 0.9), (int) (getWidth() * 0.75));
        add(songContainer);

        //add song button
        JButton addSongButton = new JButton("Add");
        addSongButton.setBounds(60, (int) (getHeight() * 0.80), 100, 25);
        addSongButton.setFont(new Font("Dialogue", Font.BOLD, 14));
        addSongButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Open file picker
                JFileChooser filePicker = new JFileChooser();
                filePicker.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));
                filePicker.setCurrentDirectory(new File("src/main/java/assets"));
                int result = filePicker.showOpenDialog(MusicPlaylistDialog.this);

                File selectedFile = filePicker.getSelectedFile();
                if(result == JFileChooser.APPROVE_OPTION && selectedFile != null){
                    JLabel filePathLabel = new JLabel(selectedFile.getPath());
                    filePathLabel.setFont(new Font("Dialog", Font.BOLD, 12));
                    filePathLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                    //Add to the list
                    songPaths.add(filePathLabel.getText());

                    //Add to container
                    songContainer.add(filePathLabel);

                    //Refresh dialogue to show newly added JLabel
                    songContainer.revalidate();
                }
            }
        });
        add(addSongButton);

        //Save playlist button
        JButton savePlaylistButton = new JButton("Save");
        savePlaylistButton.setBounds(215, (int) (getHeight() * 0.80), 100, 25);
        savePlaylistButton.setFont(new Font("Dialogue", Font.BOLD, 14));
        savePlaylistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        add(savePlaylistButton);
    }
}
