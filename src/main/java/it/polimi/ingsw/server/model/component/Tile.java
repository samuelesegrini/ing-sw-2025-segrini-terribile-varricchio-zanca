package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Rotation;
import java.util.Map;
import java.util.Set;

/**
 * A printed piece of cardboard: either a drawable tile or a starting cabin.
 *
 * <p>Everything here is immutable and shared. One catalogue serves every game on the
 * server, and two players holding "the same" tile hold the same object.
 *
 * <p>The orientation queries below encode a fact verified against all 152 tile images:
 * the artwork is consistent, so a placed piece's facing is its kind plus its rotation
 * rather than something the data has to carry per tile.
 */
public sealed interface Tile permits ComponentTile, StartingCabinTile {

    /**
     * Returns the identifier of this piece, taken from its artwork file name.
     *
     * @return the identifier, unique across the catalogue
     */
    String id();

    /**
     * Returns what this piece does once welded to a ship.
     *
     * @return the kind of component
     */
    ComponentKind kind();

    /**
     * Returns what each of the four printed sides carries.
     *
     * @return an immutable map holding all four directions
     */
    Map<Direction, Connector> connectors();

    /**
     * Returns the number of slots or charges printed on this piece.
     *
     * @return the capacity, or {@code 0} for kinds that hold nothing
     */
    int capacity();

    /**
     * Returns the connector that faces the given direction once the piece is turned.
     *
     * @param side     the direction to look at on the placed piece
     * @param rotation how far the piece is turned from its printed orientation
     * @return the connector the neighbouring cell sees
     */
    default Connector connectorFacing(Direction side, Rotation rotation) {
        return connectors().get(side.rotatedBy(rotation.inverse()));
    }

    /**
     * Returns where this engine's exhaust points once the piece is turned.
     *
     * <p>Every engine is printed exhausting {@link Direction#SOUTH}, which is why the
     * rule that engines must fire toward the stern reduces to a rotation check.
     *
     * @param rotation how far the piece is turned
     * @return the direction the exhaust points
     * @throws IllegalStateException if this piece is not an engine
     */
    default Direction exhaustDirection(Rotation rotation) {
        if (!kind().isEngine()) {
            throw new IllegalStateException(id() + " is not an engine");
        }
        return Direction.SOUTH.rotatedBy(rotation);
    }

    /**
     * Returns where this cannon's muzzle points once the piece is turned.
     *
     * <p>Every cannon is printed facing {@link Direction#NORTH}, so a cannon counting
     * full firepower is one left unrotated.
     *
     * @param rotation how far the piece is turned
     * @return the direction the muzzle points
     * @throws IllegalStateException if this piece is not a cannon
     */
    default Direction muzzleDirection(Rotation rotation) {
        if (!kind().isCannon()) {
            throw new IllegalStateException(id() + " is not a cannon");
        }
        return Direction.NORTH.rotatedBy(rotation);
    }

    /**
     * Returns the two sides this shield protects once the piece is turned.
     *
     * <p>Every shield is printed covering north and east. Manual p.7 notes that where a
     * shield sits on the ship makes no difference; only which way it faces does.
     *
     * @param rotation how far the piece is turned
     * @return the two protected directions
     * @throws IllegalStateException if this piece is not a shield
     */
    default Set<Direction> shieldedSides(Rotation rotation) {
        if (kind() != ComponentKind.SHIELD) {
            throw new IllegalStateException(id() + " is not a shield");
        }
        return Set.of(Direction.NORTH.rotatedBy(rotation), Direction.EAST.rotatedBy(rotation));
    }
}
