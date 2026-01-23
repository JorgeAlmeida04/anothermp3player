package domain;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

public class Song {

    private String songTitle;
    private String songArtist;
    private String songLength;
    private String filePath;
    private Image songCover;
    private Mp3File mp3File;
    private double frameRatePerMilliseconds;

    public Song(String filePath) throws Exception{
        this.filePath = filePath;
        try{
            mp3File = new Mp3File(filePath);
            frameRatePerMilliseconds = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds();
            songLength = convertToSongLengthFormat();

            //Extraction of the cover art
            songCover = extractCoverArt(mp3File);

            //Usage of the jaudiotagger library to create an audio file obj to read mp3 file's information
            AudioFile audioFile = AudioFileIO.read(new File(filePath));

            //Read through the meta data of the audio file
            Tag tag = audioFile.getTag();
            if(tag != null){
                songTitle = tag.getFirst(FieldKey.TITLE);
                songArtist = tag.getFirst(FieldKey.ARTIST);
            }else{
                songTitle = "N/A";
                songArtist = "N/A";
            }

        }catch (Exception e){
            throw new Exception(e);
        }
    }

    private Image extractCoverArt(Mp3File mp3File) throws Exception{
        try{
            if(mp3File.hasId3v2Tag()){
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                byte[] imageData = id3v2Tag.getAlbumImage();

                if(imageData != null){
                    //Convert byte array to BufferedImage
                    return ImageIO.read(new ByteArrayInputStream(imageData));
                }
                return ImageIO.read(new File("src/main/java/assets/record.png"));
            }
            return ImageIO.read(new File("src/main/java/assets/record.png"));
        } catch (Exception ex){
            throw new Exception(ex);
        }
    }

    private String convertToSongLengthFormat(){
        long minutes = mp3File.getLengthInSeconds() / 60;
        long seconds = mp3File.getLengthInSeconds() % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public Image getSongCover() {
        return songCover;
    }

    public double getFrameRatePerMilliseconds(){
        return frameRatePerMilliseconds;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public String getSongLength() {
        return songLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public Mp3File getMp3File(){
        return mp3File;
    }
}
