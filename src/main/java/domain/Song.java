package domain;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

public class Song {

    private String songTitle;
    private String songArtist;
    private String songLength;
    private String filePath;

    public Song(String filePath) throws Exception{
        this.filePath = filePath;
        try{
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
}
