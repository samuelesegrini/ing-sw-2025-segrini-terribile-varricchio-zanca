package it.polimi.ingsw.server.persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Where games are kept while the server is not running.
 *
 * <p>The one thing this has to get right is that a crash never leaves a half-written file where
 * a good one used to be. So nothing is ever written over: a snapshot goes to a temporary file
 * in the same directory, is forced to the disk, and is then moved into place in a single
 * operation the file system either does or does not do. A process killed at any point either
 * leaves the previous snapshot untouched or the new one complete — never a mixture.
 *
 * <p>A snapshot from a different build is refused outright rather than read as far as it goes.
 * Half a game loaded is worse than no game loaded: the players would be told they had resumed
 * and then find their ship was somebody else's.
 */
public final class SnapshotStore implements Snapshots {

    private static final String SUFFIX = ".snapshot";
    private static final String PARTIAL = ".writing";
    private static final String BROKEN = ".broken";

    private final Path directory;

    /**
     * Opens a store, creating the directory if it is not there.
     *
     * @param directory where to keep them
     * @throws UncheckedIOException if the directory cannot be made
     */
    public SnapshotStore(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException unusable) {
            throw new UncheckedIOException("cannot keep snapshots in " + directory, unusable);
        }
    }

    /**
     * Writes a snapshot, replacing any earlier one for the same game.
     *
     * <p>Atomic: the previous snapshot stays readable until this one is complete, and a crash
     * part-way through leaves it that way.
     *
     * @param snapshot what to keep
     * @throws UncheckedIOException if it cannot be written
     */
    @Override
    public void save(GameSnapshot snapshot) {
        Path finished = fileFor(snapshot.gameId());
        Path partial = directory.resolve(snapshot.gameId() + PARTIAL);
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(
                    Files.newOutputStream(partial, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                out.writeObject(snapshot);
                out.flush();
            }
            force(partial);
            move(partial, finished);
        } catch (IOException failed) {
            deleteQuietly(partial);
            throw new UncheckedIOException("could not keep " + snapshot.gameId(), failed);
        }
    }

    /**
     * Reads back one game.
     *
     * @param gameId which game
     * @return the snapshot, or empty when there is none or it is unreadable
     */
    public Optional<GameSnapshot> load(String gameId) {
        Path file = fileFor(gameId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object read = in.readObject();
            if (read instanceof GameSnapshot snapshot && snapshot.isReadable()) {
                return Optional.of(snapshot);
            }
            return Optional.empty();
        } catch (IOException | ClassNotFoundException | RuntimeException unreadable) {
            // A snapshot this build cannot understand is one it must not half-understand. The
            // file is left alone: somebody may want to look at it, and deleting evidence of a
            // format change is not this class's decision to make.
            return Optional.empty();
        }
    }

    /**
     * Reads back every game that was kept.
     *
     * @return the snapshots this build can read, in no particular order
     */
    @Override
    public List<GameSnapshot> loadAll() {
        List<GameSnapshot> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(file -> file.getFileName().toString().endsWith(SUFFIX))
                    .forEach(file -> {
                        String id = file.getFileName().toString();
                        load(id.substring(0, id.length() - SUFFIX.length())).ifPresent(found::add);
                    });
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read snapshots from " + directory, unreadable);
        }
        return List.copyOf(found);
    }

    /**
     * Forgets a game, because it has finished.
     *
     * @param gameId which game
     */
    @Override
    public void delete(String gameId) {
        deleteQuietly(fileFor(gameId));
        deleteQuietly(directory.resolve(gameId + PARTIAL));
    }

    /**
     * How many games are being kept.
     *
     * @return the count of readable and unreadable snapshots alike
     */
    public int count() {
        try (Stream<Path> files = Files.list(directory)) {
            return (int) files.filter(file -> file.getFileName().toString().endsWith(SUFFIX))
                    .count();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read snapshots from " + directory, unreadable);
        }
    }

    /**
     * Renames a snapshot aside so it is read once and then left alone.
     *
     * <p>Best effort. If the rename fails there is nothing useful to do about it: the server
     * is starting, this game was already not coming back, and refusing to start over a file
     * that could not be renamed would turn one lost game into no server at all.
     *
     * @param gameId which game
     */
    @Override
    public void setAside(String gameId) {
        try {
            move(fileFor(gameId), directory.resolve(gameId + SUFFIX + BROKEN));
        } catch (IOException | RuntimeException stuck) {
            System.err.println("could not set " + gameId + " aside: " + stuck.getMessage());
        }
    }

    private Path fileFor(String gameId) {
        return directory.resolve(gameId + SUFFIX);
    }

    /**
     * Pushes the bytes past the operating system's cache.
     *
     * <p>Without this the move can complete while the contents are still only promised, and a
     * machine that loses power keeps the new name over the old bytes.
     */
    private static void force(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    /**
     * Puts the finished file in place in one step.
     *
     * <p>Falls back to an ordinary replace where the file system will not promise atomicity,
     * which is the best that can be done there and still better than writing in place.
     */
    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException noPromise) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException stubborn) {
            // Nothing useful to do about it, and throwing would turn tidying up into a failure.
        }
    }
}
