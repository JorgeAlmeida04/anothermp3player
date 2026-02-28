package model;

public class LyricsLine {

    private final long timeMs;
    private final String text;

    public LyricsLine(long timeMs, String text) {
        this.timeMs = Math.max(0, timeMs);
        this.text = text != null ? text : "";
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getText() {
        return text;
    }

    public boolean hasTimestamp() {
        return timeMs > 0;
    }

    @Override
    public String toString() {
        return (
            "LyricsLine{" + "timeMs=" + timeMs + ", text='" + text + '\'' + '}'
        );
    }
}
