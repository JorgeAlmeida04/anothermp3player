package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LyricsTrack {

    private final boolean synced;
    private final List<LyricsLine> lines;
    private final String rawText;
    private final String sourcePath;

    private LyricsTrack(
        boolean synced,
        List<LyricsLine> lines,
        String rawText,
        String sourcePath
    ) {
        this.synced = synced;
        this.lines = Collections.unmodifiableList(
            lines != null ? new ArrayList<>(lines) : new ArrayList<>()
        );
        this.rawText = rawText != null ? rawText : "";
        this.sourcePath = sourcePath != null ? sourcePath : "";
    }

    public static LyricsTrack synced(
        List<LyricsLine> lines,
        String sourcePath
    ) {
        return new LyricsTrack(true, lines, "", sourcePath);
    }

    public static LyricsTrack unsynced(String rawText, String sourcePath) {
        return new LyricsTrack(false, List.of(), rawText, sourcePath);
    }

    public boolean isSynced() {
        return synced;
    }

    public List<LyricsLine> getLines() {
        return lines;
    }

    public String getRawText() {
        return rawText;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public boolean isEmpty() {
        return (synced && lines.isEmpty()) || (!synced && rawText.isBlank());
    }

    @Override
    public String toString() {
        return (
            "LyricsTrack{" +
            "synced=" +
            synced +
            ", lines=" +
            lines.size() +
            ", rawTextLength=" +
            rawText.length() +
            ", sourcePath='" +
            sourcePath +
            '\'' +
            '}'
        );
    }
}
