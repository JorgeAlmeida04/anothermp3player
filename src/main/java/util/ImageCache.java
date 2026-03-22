package util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Centralized image loading and caching utility.
 *
 * Eliminates the duplicated setImage() method that existed in
 * MP3PlayerGUIJavaFX, CenterContainer, and BottomContainer.
 *
 * Caches loaded images to avoid repeatedly reading the same
 * resource from disk (e.g., play/pause icons toggled every update cycle).
 *
 * Also caches album artwork to prevent repeated creation of Image objects
 * from byte arrays, reducing GC pressure.
 */
public final class ImageCache {

    private static final Map<String, Image> cache = new HashMap<>();
    
    // Album art cache: key = content hash + dimensions, value = soft reference to image
    // Using SoftReference allows GC to reclaim images under memory pressure
    private static final Map<AlbumArtKey, SoftReference<Image>> albumArtCache = new HashMap<>();
    
    /**
     * Key for album art cache entries based on image content hash and target dimensions.
     */
    private static final class AlbumArtKey {
        private final int contentHash;
        private final double requestedWidth;
        private final double requestedHeight;
        
        AlbumArtKey(byte[] imageData, double requestedWidth, double requestedHeight) {
            // Use Arrays.hashCode for stable content-based hash
            this.contentHash = Arrays.hashCode(imageData);
            this.requestedWidth = requestedWidth;
            this.requestedHeight = requestedHeight;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AlbumArtKey)) return false;
            AlbumArtKey that = (AlbumArtKey) o;
            return contentHash == that.contentHash &&
                   Double.compare(that.requestedWidth, requestedWidth) == 0 &&
                   Double.compare(that.requestedHeight, requestedHeight) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(contentHash, requestedWidth, requestedHeight);
        }
    }

    private ImageCache() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets an image from the /assets/ folder, using cache if available.
     *
     * @param fileName The image file name (e.g., "new-play.png")
     * @return The cached or newly loaded Image
     * @throws RuntimeException if the resource is not found
     */
    public static Image getImage(String fileName) {
        return cache.computeIfAbsent(fileName, key -> {
            String resourcePath = "/assets/" + key;
            InputStream inputStream = ImageCache.class.getResourceAsStream(
                resourcePath
            );
            if (inputStream == null) {
                throw new RuntimeException(
                    "Resource not found: " + resourcePath
                );
            }
            return new Image(inputStream);
        });
    }

    /**
     * Sets a cached icon image on a button.
     * Reuses the cached Image object but creates a new ImageView
     * (ImageView cannot be shared across scene graph nodes).
     *
     * @param button   The button to set the image on
     * @param fileName The name of the image file in /assets/
     */
    public static void setButtonImage(ButtonBase button, String fileName) {
        Image image = getImage(fileName);

        Object currentKey = button.getProperties().get("imageCache.iconKey");
        if (
            fileName.equals(currentKey) &&
            button.getGraphic() instanceof ImageView
        ) {
            ImageView currentView = (ImageView) button.getGraphic();
            if (currentView.getImage() == image) {
                return;
            }
        }

        ImageView imageView = new ImageView(image);
        button.setGraphic(imageView);
        button.getProperties().put("imageCache.iconKey", fileName);
    }
    
    /**
     * Gets a cached album art Image from byte array data.
     * Caches images by content hash and requested dimensions to avoid
     * recreating Image objects when switching between songs.
     *
     * @param imageData      The raw image bytes (e.g., from ID3 tag)
     * @param requestedWidth The desired width (0 for original)
     * @param requestedHeight The desired height (0 for original)
     * @param preserveRatio  Whether to preserve aspect ratio
     * @param smooth         Whether to use smooth scaling
     * @return The cached or newly created Image
     */
    public static Image getAlbumArtImage(byte[] imageData, double requestedWidth, double requestedHeight,
                                          boolean preserveRatio, boolean smooth) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        
        AlbumArtKey key = new AlbumArtKey(imageData, requestedWidth, requestedHeight);
        
        // Check cache
        SoftReference<Image> ref = albumArtCache.get(key);
        if (ref != null) {
            Image cached = ref.get();
            if (cached != null && !cached.isError()) {
                return cached;
            }
            // Image was GC'd or had error, remove stale entry
            albumArtCache.remove(key);
        }
        
        // Create new image
        Image image = new Image(new ByteArrayInputStream(imageData), 
                                requestedWidth, requestedHeight, preserveRatio, smooth);
        
        // Only cache if no error occurred during loading
        if (!image.isError()) {
            albumArtCache.put(key, new SoftReference<>(image));
        }
        
        return image;
    }
    
    /**
     * Clears all cached album art images.
     * Call this when memory pressure is high or when playlist changes significantly.
     */
    public static void clearAlbumArtCache() {
        albumArtCache.clear();
    }
    
    /**
     * Returns the current number of cached album art images.
     * Useful for debugging cache effectiveness.
     */
    public static int getAlbumArtCacheSize() {
        return albumArtCache.size();
    }
}
