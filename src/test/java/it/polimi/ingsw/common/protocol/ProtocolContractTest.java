package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.GameView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts the three promises the protocol makes about itself.
 *
 * <p><b>Every message is accounted for.</b> {@link Messages} is compared against the sealed
 * hierarchies, so a record added without a sample fails the build rather than becoming a
 * message nobody ever tried to send. The same hole the adventure card factory grew through
 * in M3, closed before it opens.
 *
 * <p><b>Every message can cross an RMI connection.</b> RMI marshals with Java serialization,
 * so a message carrying something unserializable does not fail at compile time, or in a unit
 * test, or on a socket — it fails the first time somebody plays over RMI. The structural
 * check walks the record components of the whole hierarchy and refuses anything Java
 * serialization cannot carry; the round trip then proves it on real instances.
 *
 * <p><b>Nothing on the wire is an {@link Optional}.</b> Which is the specific case of the
 * above worth naming, because {@code Optional} is deliberately not serializable and is
 * otherwise the obvious thing to reach for. A value that may be absent is {@code null} and
 * is read through an {@code …IfAny()} accessor.
 *
 * <p>Components involved: {@link Command}, {@link Event}, {@link GameView}.
 */
class ProtocolContractTest {

    /** Types Java serialization carries that are not themselves records or enums. */
    private static final Set<Class<?>> CARRIED = Set.of(
            String.class, Integer.class, Long.class, Boolean.class, Double.class,
            List.class, Set.class, Map.class);

    // ------------------------------------------------------------------ coverage

    @Test
    @DisplayName("every command in the protocol has a worked example")
    void everyCommandIsSampled() {
        assertEquals(Set.of(), missingFrom(Command.class, Messages.commands()),
                "these commands have never been sent, not even in a test");
    }

    @Test
    @DisplayName("every event in the protocol has a worked example")
    void everyEventIsSampled() {
        assertEquals(Set.of(), missingFrom(Event.class, Messages.events()),
                "these events have never been received, not even in a test");
    }

    @Test
    @DisplayName("every choice and every prompt has a worked example")
    void everyFlightMessageIsSampled() {
        assertEquals(Set.of(), missingFrom(PlayerChoice.class, Messages.choices()));
        assertEquals(Set.of(), missingFrom(PlayerPrompt.class, Messages.prompts()));
    }

    @Test
    @DisplayName("the hierarchy walker finds the messages it is supposed to be checking")
    void theWalkerActuallyWalks() {
        // Without this, a walker that returned nothing would make every check above pass
        // by finding no messages to complain about. The four families are named here so
        // that deleting one is a failure rather than a quietly smaller protocol.
        assertEquals(Set.of("LobbyCommand", "BuildingCommand", "PreparationCommand", "FlightCommand"),
                familiesOf(Command.class));
        assertEquals(Set.of("LobbyEvent", "GameEvent", "FlightEvent"), familiesOf(Event.class));

        assertEquals(leavesOf(Command.class).size(), Messages.commands().size());
        assertEquals(leavesOf(Event.class).size(), Messages.events().size());
        assertTrue(leavesOf(Command.class).size() >= 20, "the protocol got unexpectedly small");
    }

    // ------------------------------------------------------------------ structure

    @Test
    @DisplayName("no message carries anything RMI could not marshal")
    void everyMessageIsMarshallable() {
        Set<String> offences = new TreeSet<>();
        Stream.of(Command.class, Event.class, PlayerPrompt.class, PlayerChoice.class)
                .flatMap(root -> leavesOf(root).stream())
                .forEach(message -> checkComponents(message, offences));

        assertEquals(Set.of(), offences,
                "Java serialization cannot carry these, so RMI would fail on them at run time");
    }

    @Test
    @DisplayName("a message is a record, so that it cannot grow behaviour")
    void everyMessageIsARecord() {
        Set<String> classes = Stream.of(Command.class, Event.class)
                .flatMap(root -> leavesOf(root).stream())
                .filter(leaf -> !leaf.isRecord())
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(Set.of(), classes, "a message is data, not an object with opinions");
    }

    // ------------------------------------------------------------------ round trip

    @Test
    @DisplayName("every command survives being marshalled and read back")
    void commandsSurviveTheWire() {
        Messages.commands().forEach(ProtocolContractTest::assertRoundTrips);
    }

    @Test
    @DisplayName("every event survives being marshalled and read back")
    void eventsSurviveTheWire() {
        Messages.events().forEach(ProtocolContractTest::assertRoundTrips);
    }

    @Test
    @DisplayName("every prompt and every answer survives being marshalled and read back")
    void flightMessagesSurviveTheWire() {
        Messages.prompts().forEach(ProtocolContractTest::assertRoundTrips);
        Messages.choices().forEach(ProtocolContractTest::assertRoundTrips);
    }

    @Test
    @DisplayName("a whole game, nested views and all, comes back the same")
    void theWholePictureSurvivesTheWire() {
        assertRoundTrips(new GameEvent.StateChanged(Messages.state()));
    }

    // ------------------------------------------------------------------ absence

    @Test
    @DisplayName("an absent value reads back as an empty Optional, not as a null nobody expected")
    void absenceIsReadableAgain() {
        // The other half of rule 2.3. Banning Optional from the wire is only safe if the
        // null it is replaced by never escapes into code that would dereference it, which
        // is what the …IfAny() accessors are for.
        BuildingView empty = new BuildingView(0, List.of(), null, null, List.of(), null, 0, 0,
                Set.of(), List.of());
        assertEquals(Optional.empty(), empty.handIfAny());
        assertEquals(Optional.empty(), empty.unweldedIfAny());

        FlightView between = new FlightView(24, Map.of(), List.of(), null, 0);
        assertEquals(Optional.empty(), between.cardIfAny());

        GameView lobby = new GameView("game-1", GameLevel.LEVEL_II, GamePhase.LOBBY,
                PlayerColor.RED, List.of(), null, null, null, null);
        assertEquals(Optional.empty(), lobby.buildingIfAny());
        assertEquals(Optional.empty(), lobby.flightIfAny());
        assertEquals(Optional.empty(), lobby.pendingIfAny());
        assertEquals(Optional.empty(), lobby.scoresIfAny());

        assertEquals(Optional.empty(),
                new BuildingCommand.FinishBuilding(null).startSpaceIfAny());
        assertEquals(Optional.of(3),
                new BuildingCommand.FinishBuilding(3).startSpaceIfAny());
        assertEquals(Optional.empty(),
                new PreparationCommand.BoardCrew(new Position(2, 3), null).alienIfAny());
        assertEquals(Optional.of(AlienColor.PURPLE),
                new PreparationCommand.BoardCrew(new Position(2, 3), AlienColor.PURPLE).alienIfAny());
    }

    @Test
    @DisplayName("the whole picture keeps its nested views across the wire")
    void nestedViewsSurviveIntact() {
        GameView sent = Messages.state();
        GameView back = ((GameEvent.StateChanged) roundTrip(new GameEvent.StateChanged(sent))).state();

        assertEquals(sent, back);
        assertEquals(2, back.players().size());
        assertTrue(back.buildingIfAny().isPresent());
        assertTrue(back.flightIfAny().isPresent());
        assertTrue(back.pendingIfAny().isPresent());
        assertEquals(sent.players().get(0).ship().cells(), back.players().get(0).ship().cells());
    }

    // ------------------------------------------------------------------ machinery

    private static Set<String> familiesOf(Class<?> root) {
        return Stream.of(root.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> missingFrom(Class<?> root, List<?> samples) {
        Set<Class<?>> sampled = samples.stream()
                .map(Object::getClass)
                .collect(Collectors.toSet());
        return leavesOf(root).stream()
                .filter(leaf -> !sampled.contains(leaf))
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Walks a sealed hierarchy down to the records that actually get sent. */
    private static Set<Class<?>> leavesOf(Class<?> root) {
        Set<Class<?>> leaves = new LinkedHashSet<>();
        List<Class<?>> pending = new ArrayList<>(List.of(root));
        while (!pending.isEmpty()) {
            Class<?> next = pending.remove(0);
            Class<?>[] permitted = next.getPermittedSubclasses();
            if (permitted == null || permitted.length == 0) {
                leaves.add(next);
            } else {
                pending.addAll(List.of(permitted));
            }
        }
        return leaves;
    }

    private static void checkComponents(Class<?> message, Set<String> offences) {
        if (!message.isRecord()) {
            return;
        }
        for (RecordComponent component : message.getRecordComponents()) {
            String where = message.getSimpleName() + "." + component.getName();
            checkType(component.getGenericType(), where, offences);
        }
    }

    private static void checkType(Type type, String where, Set<String> offences) {
        if (type instanceof ParameterizedType parameterized) {
            checkType(parameterized.getRawType(), where, offences);
            for (Type argument : parameterized.getActualTypeArguments()) {
                checkType(argument, where, offences);
            }
            return;
        }
        if (!(type instanceof Class<?> raw)) {
            offences.add(where + " is generic, and what it will hold is not known here");
            return;
        }
        if (raw == Optional.class) {
            offences.add(where + " is an Optional; use null and an …IfAny() accessor");
            return;
        }
        if (raw.isPrimitive() || raw.isEnum() || CARRIED.contains(raw)) {
            return;
        }
        if (Serializable.class.isAssignableFrom(raw)) {
            checkComponents(raw, offences);
            return;
        }
        offences.add(where + " is a " + raw.getSimpleName() + ", which is not Serializable");
    }

    private static void assertRoundTrips(Object message) {
        Object read = roundTrip(message);
        assertEquals(message, read,
                message.getClass().getSimpleName() + " came back different from how it went out");
        assertTrue(message.getClass() == read.getClass(),
                "a message must come back as the same type it went out as");
    }

    private static Object roundTrip(Object message) {
        Object read;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(message);
            }
            try (ObjectInputStream in =
                         new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                read = in.readObject();
            }
        } catch (IOException | ClassNotFoundException problem) {
            throw new AssertionError(message.getClass().getSimpleName()
                    + " does not survive the wire: " + problem, problem);
        }
        return read;
    }
}
