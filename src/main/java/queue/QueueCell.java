package queue;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class QueueCell extends ListCell<QueueItem> {
    private HBox content;
    private ImageView imageView;
    private Label titleLabel;
    private Label artistLabel;
    private Label durationLabel;
    private VBox textContainer;

    public QueueCell() {
        super();
        imageView = new ImageView();
        imageView.setFitHeight(40);
        imageView.setFitWidth(40);
        imageView.setPreserveRatio(true);

        titleLabel = new Label();
        titleLabel.getStyleClass().add("queue-item-title");

        artistLabel = new Label();
        artistLabel.getStyleClass().add("queue-item-artist");

        textContainer = new VBox(titleLabel, artistLabel);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        durationLabel = new Label();
        durationLabel.getStyleClass().add("queue-item-duration");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(imageView, textContainer, spacer, durationLabel);
        content.setSpacing(10);
        content.setPadding(new Insets(0, 15, 0, 0));
        content.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(QueueItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            setStyle(""); // Reset style
        } else {
            if (item.image != null) {
                imageView.setImage(item.image);
            } else {
                imageView.setImage(null);
                // Could set a default placeholder here
            }
            titleLabel.setText(item.title);
            artistLabel.setText(item.artist);
            durationLabel.setText(item.duration);
            setGraphic(content);
            setText(null);
        }
    }
}
