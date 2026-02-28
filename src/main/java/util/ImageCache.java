package util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
 */
public final class ImageCache {

    private static final Map<String, Image> cache = new HashMap<>();

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
}
