package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.LyricsLine;
import model.LyricsTrack;

/**
 * Local-only lyrics service.
 *
 * Resolution strategy (no online APIs):
 * 1) Same directory + same basename as song:
 *    - .lrc (preferred)
 *    - .txt
 * 2) Returns empty if nothing is found.
 *
 * Parsing behavior:
 * - .lrc with timestamps => synced LyricsTrack
 * - .lrc without timestamps => unsynced LyricsTrack (raw text)
 * - .txt => unsynced LyricsTrack (raw text)
 */
public class LyricsService {

    // Matches [mm:ss], [mm:ss.xx], [mm:ss.xxx]
    private static final Pattern TIME_TAG_PATTERN = Pattern.compile(
        "\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]"
    );

    /**
     * Resolve and parse local lyrics for a song file.
     *
     * @param songFile mp3 (or audio) file
     * @return Optional of LyricsTrack when local sidecar exists and parses;
     *         Optional.empty() otherwise.
     */
    public Optional<LyricsTrack> resolveLyrics(File songFile) {
        if (songFile == null || !songFile.exists() || !songFile.isFile()) {
            return Optional.empty();
        }

        List<File> candidates = findSidecarCandidates(songFile);
        for (File candidate : candidates) {
            Optional<LyricsTrack> parsed = parseLyricsFile(candidate);
            if (parsed.isPresent() && !parsed.get().isEmpty()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    /**
     * Find sidecar candidates in deterministic order:
     * .lrc first, then .txt
     */
    private List<File> findSidecarCandidates(File songFile) {
        List<File> result = new ArrayList<>();
        String base = stripExtension(songFile.getName());
        File dir = songFile.getParentFile();
        if (dir == null || !dir.exists()) return result;

        File lrc = new File(dir, base + ".lrc");
        File txt = new File(dir, base + ".txt");

        if (lrc.exists() && lrc.isFile()) result.add(lrc);
        if (txt.exists() && txt.isFile()) result.add(txt);

        return result;
    }

    /**
     * Parse .lrc or .txt into LyricsTrack.
     */
    private Optional<LyricsTrack> parseLyricsFile(File file) {
        try {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".lrc")) {
                return Optional.of(parseLrc(file));
            }
            if (name.endsWith(".txt")) {
                String raw = readWholeFile(file);
                if (raw.isBlank()) return Optional.empty();
                return Optional.of(LyricsTrack.unsynced(raw, file.getAbsolutePath()));
            }
        } catch (Exception e) {
            System.err.println("Lyrics parse failed for " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Parse LRC:
     * - if timestamped lines found => synced track
     * - otherwise => unsynced raw text
     */
    private LyricsTrack parseLrc(File lrcFile) throws Exception {
        List<LyricsLine> lines = new ArrayList<>();
        StringBuilder rawFallback = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
            new FileReader(lrcFile, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                rawFallback.append(line).append(System.lineSeparator());

                Matcher matcher = TIME_TAG_PATTERN.matcher(line);
                List<Long> times = new ArrayList<>();

                int lastTagEnd = 0;
                while (matcher.find()) {
                    long t = parseTimeToMs(matcher.group(1), matcher.group(2), matcher.group(3));
                    times.add(t);
                    lastTagEnd = matcher.end();
                }

                // Text after last timestamp
                String text = lastTagEnd > 0 ? line.substring(lastTagEnd).trim() : line.trim();

                if (!times.isEmpty()) {
                    // one LRC line may have multiple timestamps
                    for (Long t : times) {
                        lines.add(new LyricsLine(t, text));
                    }
                }
            }
        }

        if (lines.isEmpty()) {
            String raw = rawFallback.toString().trim();
            return LyricsTrack.unsynced(raw, lrcFile.getAbsolutePath());
        }

        lines.sort(Comparator.comparingLong(LyricsLine::getTimeMs));
        return LyricsTrack.synced(lines, lrcFile.getAbsolutePath());
    }

    private long parseTimeToMs(String mm, String ss, String frac) {
        int minutes = Integer.parseInt(mm);
        int seconds = Integer.parseInt(ss);

        int millis = 0;
        if (frac != null && !frac.isBlank()) {
            // normalize fraction to milliseconds
            // "7" -> 700ms, "73" -> 730ms, "735" -> 735ms
            if (frac.length() == 1) millis = Integer.parseInt(frac) * 100;
            else if (frac.length() == 2) millis = Integer.parseInt(frac) * 10;
            else millis = Integer.parseInt(frac.substring(0, 3));
        }

        return minutes * 60_000L + seconds * 1_000L + millis;
    }

    private String readWholeFile(File file) throws Exception {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
    }

    private String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx <= 0 ? filename : filename.substring(0, idx);
    }
}
