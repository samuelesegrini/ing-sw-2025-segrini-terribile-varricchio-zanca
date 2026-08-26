package it.polimi.ingsw.client.view.gui;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The pictures, loaded once.
 *
 * <p>A ship is up to thirty-five tiles and the heap is a hundred and fifty-six, every one a
 * separate file. Reading them from disk each time the board redraws — which is every time
 * anybody anywhere does anything — is the difference between a window that keeps up and one
 * that does not.
 *
 * <p>Only ever touched from the JavaFX thread, so a plain map is enough and a synchronised one
 * would be pretending otherwise.
 */
final class TileImages {

    private final Artwork artwork;
    private final Map<String, Image> loaded = new HashMap<>();

    TileImages(Artwork artwork) {
        this.artwork = artwork;
    }

    /**
     * Returns the picture of a component.
     *
     * @param tileId the component's id
     * @return the image, or empty when nothing is filed under that name
     */
    Optional<Image> ofTile(String tileId) {
        return artwork.ofTile(tileId).map(this::load);
    }

    /**
     * Returns the picture of an adventure card.
     *
     * @param cardId the card's id
     * @return the image, or empty when nothing is filed under that name
     */
    Optional<Image> ofCard(String cardId) {
        return artwork.ofCard(cardId).map(this::load);
    }

    /**
     * Returns a picture by resource path, for the boards that are not looked up by name.
     *
     * @param path where it lives
     * @return the image
     */
    Image ofPath(String path) {
        return load(path);
    }

    private Image load(String path) {
        return loaded.computeIfAbsent(path, where ->
                new Image(TileImages.class.getResourceAsStream(where)));
    }
}
