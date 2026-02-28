package services;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Song;
import repos.SongsRepo;

public class SongService {

    private final SongsRepo songsRepo;

    public SongService() {
        this.songsRepo = new SongsRepo();
    }

    public void deletedFiles() throws SQLException {
        int removed = songsRepo.pruneOrphans();
        if (removed > 0) {
            System.out.println("Pruned " + removed + " songs");
        }
    }

    public Optional<Song> getSongByPath(String path) throws SQLException {
        return songsRepo.findByPath(path);
    }

    public List<Song> getAllSongs() throws SQLException {
        return songsRepo.findAll();
    }

    public List<String> findAllSongPaths() throws SQLException {
        List<Song> songs = songsRepo.findAll();
        List<String> paths = new ArrayList<>(songs.size());
        for (Song song : songs) {
            if (song.getFilePath() != null && !song.getFilePath().isBlank()) {
                paths.add(song.getFilePath());
            }
        }
        return paths;
    }

    public List<File> getExistingSongFilesFromDb() throws SQLException {
        List<Song> songs = songsRepo.findAll();
        List<File> files = new ArrayList<>(songs.size());
        for (Song song : songs) {
            if (
                song.getFilePath() == null || song.getFilePath().isBlank()
            ) continue;
            File file = new File(song.getFilePath());
            if (file.exists() && file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    public List<Song> search(String query) throws SQLException {
        return songsRepo.search(query);
    }

    public Song insertSong(Song song) throws SQLException {
        return songsRepo.insert(song);
    }

    public void insertSongs(List<Song> songs) throws SQLException {
        songsRepo.insertBatch(songs);
    }

    public List<String> findAllArtists() throws SQLException {
        return songsRepo.findAllArtists();
    }

    public List<Song> getSongsData(List<File> files) throws SQLException {
        if (files == null || files.isEmpty()) return List.of();

        List<Song> songs = new ArrayList<>();
        for (File file : files) {
            if (file == null || !file.exists() || !file.isFile()) continue;
            songs.add(getSongData(file));
        }

        if (!songs.isEmpty()) {
            insertSongs(songs);
        }
        return songs;
    }

    public void scanAndStore(List<File> files) throws SQLException {
        getSongsData(files);
    }

    public Song getSongData(File file) {
        String title = file.getName();
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        long durationMs = 0;

        try {
            Mp3File mp3File = new Mp3File(file);
            durationMs = mp3File.getLengthInSeconds() * 1000L;

            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                if (
                    id3v2Tag.getTitle() != null &&
                    !id3v2Tag.getTitle().isBlank()
                ) {
                    title = id3v2Tag.getTitle();
                }
                if (
                    id3v2Tag.getArtist() != null &&
                    !id3v2Tag.getArtist().isBlank()
                ) {
                    artist = id3v2Tag.getArtist();
                }
                if (
                    id3v2Tag.getAlbum() != null &&
                    !id3v2Tag.getAlbum().isBlank()
                ) {
                    album = id3v2Tag.getAlbum();
                }
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                if (
                    id3v1Tag.getTitle() != null &&
                    !id3v1Tag.getTitle().isBlank()
                ) {
                    title = id3v1Tag.getTitle();
                }
                if (
                    id3v1Tag.getArtist() != null &&
                    !id3v1Tag.getArtist().isBlank()
                ) {
                    artist = id3v1Tag.getArtist();
                }
                if (
                    id3v1Tag.getAlbum() != null &&
                    !id3v1Tag.getAlbum().isBlank()
                ) {
                    album = id3v1Tag.getAlbum();
                }
            }
        } catch (Exception e) {
            System.err.println(
                "Failed to extract metadata for " +
                    file.getPath() +
                    ": " +
                    e.getMessage()
            );
        }

        return new Song(file.getPath(), title, artist, album, durationMs);
    }

    public List<File> mergeAndStore(List<File> selectedFiles)
        throws SQLException {
        List<File> merged = new ArrayList<>();
        java.util.Set<String> seenPaths = new java.util.HashSet<>();

        List<File> existing = getExistingSongFilesFromDb();
        if (existing != null && !existing.isEmpty()) {
            for (File file : existing) {
                if (file == null || !file.exists() || !file.isFile()) continue;
                String path = file.getPath();
                if (seenPaths.add(path)) {
                    merged.add(file);
                }
            }
        }

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                if (file == null || !file.exists() || !file.isFile()) continue;
                String path = file.getPath();
                if (seenPaths.add(path)) {
                    merged.add(file);
                }
            }
            scanAndStore(selectedFiles);
        }

        return merged;
    }
}
