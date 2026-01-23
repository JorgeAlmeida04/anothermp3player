import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
                try{
                    JFileChooser filePicker = new JFileChooser();
                    filePicker.setCurrentDirectory(new File("src/main/java/assets"));
                    int result = filePicker.showSaveDialog(MusicPlaylistDialog.this);

                    if(result == JFileChooser.APPROVE_OPTION){
                        //Use getSelectedFile() to get reference to the file that we are about to save
                        File selectedFile = filePicker.getSelectedFile();

                        //Convert to .txt file if not done so already
                        //Check to see if the file does not have the ".txt" file extension
                        if(!selectedFile.getName().substring(selectedFile.getName().length() - 4).equalsIgnoreCase(".txt")){
                            selectedFile = new File(selectedFile.getAbsoluteFile() + ".txt");
                        }

                        //Create the new file at the destinated directory
                        selectedFile.createNewFile();

                        //Write all the song paths into the file
                        FileWriter fw = new FileWriter(selectedFile);
                        BufferedWriter bufferedWriter = new BufferedWriter(fw);

                        //Iterate through the song paths list and write each string into the file
                        //Each song will be written in their own row
                        for(String songPath : songPaths){
                            bufferedWriter.write(songPath + "\n");
                        }
                        bufferedWriter.close();

                        //Display success dialog
                        JOptionPane.showMessageDialog(MusicPlaylistDialog.this, "Successfully Created Playlist!");

                        //Close dialogue
                        MusicPlaylistDialog.this.dispose();

                    }
                } catch(Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        });
        add(savePlaylistButton);
    }
}
