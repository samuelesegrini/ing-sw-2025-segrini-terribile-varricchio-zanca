package it.polimi.ingsw.server.data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where the loader reads game data files from.
 *
 * <p>An indirection with exactly one production implementation, so that tests can feed
 * the loader deliberately broken data without writing files.
 */
@FunctionalInterface
public interface GameDataSource {

    /**
     * Opens a game data file by name.
     *
     * @param name the file name, for example {@code components.json}
     * @return a stream over the file's bytes; the caller closes it
     * @throws IOException if the file cannot be opened
     */
    InputStream open(String name) throws IOException;

    /**
     * Returns a source reading the files bundled in {@code /data} on the classpath.
     *
     * @return the production data source
     */
    static GameDataSource bundled() {
        return name -> {
            InputStream stream = GameDataSource.class.getResourceAsStream("/data/" + name);
            if (stream == null) {
                throw new IOException("no bundled game data file named " + name);
            }
            return stream;
        };
    }
}
