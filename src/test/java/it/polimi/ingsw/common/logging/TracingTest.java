package it.polimi.ingsw.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.read.ListAppender;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.transport.AbstractChannel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Envelope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether this process says what it is doing, and where.
 *
 * <p>Two of these tests exist to protect things that are invisible when they work. A console
 * appender on the client's logger tree tears the ship the TUI is drawing, and would do it only
 * when somebody happened to log mid-frame. A trace built at {@code INFO} costs a projection's
 * {@code toString} on every message whether or not anybody reads it.
 *
 * <p>Components involved: {@link Tracing}, {@link Command}, {@link Envelope}.
 */
class TracingTest {

    /** Nothing in the environment, unless a test says otherwise. */
    private static final UnaryOperator<String> NOTHING_SET = name -> null;

    private static UnaryOperator<String> environment(Map<String, String> values) {
        return values::get;
    }

    @AfterEach
    void putTheConfigurationBack() throws Exception {
        // These tests reconfigure logging for the whole JVM, so the next test class must not
        // inherit a file appender or a raised level from this one.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator again = new JoranConfigurator();
        again.setContext(context);
        again.doConfigure(TracingTest.class.getResourceAsStream("/logback.xml"));
    }

    @Nested
    @DisplayName("asking for a trace")
    class Asking {

        @Test
        @DisplayName("is off unless somebody asks")
        void offByDefault() {
            assertFalse(Tracing.wanted(new String[] {"4321", "4322"}, NOTHING_SET));
        }

        @Test
        @DisplayName("the flag works before the other arguments and after them")
        void theFlagWorksInEitherPosition() {
            assertTrue(Tracing.wanted(new String[] {"--debug", "1234", "5678"}, NOTHING_SET));
            assertTrue(Tracing.wanted(new String[] {"1234", "5678", "--debug"}, NOTHING_SET));
        }

        @Test
        @DisplayName("and the environment says the same thing, for a launch script")
        void theVariableWorks() {
            assertTrue(Tracing.wanted(new String[] {},
                    environment(Map.of(Tracing.DEBUG_VARIABLE, "1"))));
            assertFalse(Tracing.wanted(new String[] {},
                    environment(Map.of(Tracing.DEBUG_VARIABLE, "0"))));
            assertFalse(Tracing.wanted(new String[] {},
                    environment(Map.of(Tracing.DEBUG_VARIABLE, "false"))));
        }

        @Test
        @DisplayName("the tracing arguments are taken out, so everything else parses as it did")
        void theRestIsLeftAlone() {
            assertArrayEquals(new String[] {"1234", "5678"},
                    Tracing.without(new String[] {"--debug", "1234", "5678"}));
            assertArrayEquals(new String[] {"1234", "5678"},
                    Tracing.without(new String[] {"1234", "--debug", "5678"}));
            assertArrayEquals(new String[] {"--tui", "--socket"},
                    Tracing.without(new String[] {"--tui", "--log", "x.log", "--socket"}));
        }

        @Test
        @DisplayName("the client's log file has a default, and can be moved")
        void theLogFileCanBeMoved() {
            assertEquals(Tracing.DEFAULT_CLIENT_LOG, Tracing.logFile(new String[] {}, NOTHING_SET));
            assertEquals(Path.of("/tmp/a.log"),
                    Tracing.logFile(new String[] {"--log", "/tmp/a.log"}, NOTHING_SET));
            assertEquals(Path.of("/tmp/b.log"), Tracing.logFile(new String[] {},
                    environment(Map.of(Tracing.LOG_VARIABLE, "/tmp/b.log"))));
        }
    }

    @Nested
    @DisplayName("where the client writes")
    class TheClientsTerminal {

        @Test
        @DisplayName("never to the console, because that is where the ship is drawn")
        void nothingIsAttachedToTheConsole(@TempDir Path directory) {
            Tracing.toFile(directory.resolve("client.log"), true);

            ch.qos.logback.classic.Logger root =
                    ((LoggerContext) LoggerFactory.getILoggerFactory())
                            .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext();) {
                Appender<ILoggingEvent> appender = it.next();
                // The one that rots first, and the one nobody notices until a log line lands
                // in the middle of a rendered board.
                assertFalse(appender instanceof ConsoleAppender,
                        "a console appender would tear the TUI's frames");
            }
        }

        @Test
        @DisplayName("and the file is the one it was told to use")
        void theFileIsTheOneAsked(@TempDir Path directory) {
            Path log = directory.resolve("somewhere.log");

            Tracing.toFile(log, true);
            LoggerFactory.getLogger("it.polimi.ingsw.probe").debug("something happened");

            assertTrue(java.nio.file.Files.exists(log));
        }
    }

    @Nested
    @DisplayName("what a message crossing a channel produces")
    class Messages {

        /** A channel with no wire in it, so a message can be handed straight to deliver. */
        private static final class Scripted extends AbstractChannel<Event, Command> {

            Scripted() {
                super(Command.class);
                listenWith(new ChannelListener<>() {
                    @Override
                    public void received(Command command) {
                        // Not what this suite is about.
                    }

                    @Override
                    public void closed(String reason) {
                        // Not what this suite is about.
                    }
                });
                start();
            }

            @Override
            protected void transmit(Envelope envelope) {
                // Nowhere to send it.
            }

            @Override
            protected void release() {
                // Nothing to let go of.
            }

            void arrive() {
                deliver(new Envelope.Message(new LobbyCommand.ListGames()));
            }

            void beat() {
                deliver(new Envelope.KeepAlive());
            }
        }

        private ListAppender<ILoggingEvent> listeningAt(Level level) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            ListAppender<ILoggingEvent> records = new ListAppender<>();
            records.setContext(context);
            records.start();
            ch.qos.logback.classic.Logger channel =
                    context.getLogger(AbstractChannel.class);
            channel.setLevel(level);
            channel.addAppender(records);
            return records;
        }

        @Test
        @DisplayName("exactly one line, at debug, naming what it was")
        void oneLinePerMessage() {
            ListAppender<ILoggingEvent> records = listeningAt(Level.DEBUG);

            new Scripted().arrive();

            List<ILoggingEvent> messages = records.list.stream()
                    .filter(record -> record.getFormattedMessage().contains("ListGames"))
                    .toList();
            assertEquals(1, messages.size(), "one message, one line");
            assertEquals(Level.DEBUG, messages.get(0).getLevel());
        }

        @Test
        @DisplayName("and nothing at all when the trace is off")
        void nothingAtInfo() {
            ListAppender<ILoggingEvent> records = listeningAt(Level.INFO);

            Scripted channel = new Scripted();
            channel.arrive();

            // Not merely quiet: the argument is never formatted either, which on a StateChanged
            // is a whole projection built for nobody.
            assertTrue(records.list.isEmpty(), "a trace nobody asked for still cost something");
        }

        @Test
        @DisplayName("a keep-alive is quieter than a message, or it drowns everything else")
        void keepAlivesAreQuieter() {
            ListAppender<ILoggingEvent> records = listeningAt(Level.DEBUG);

            new Scripted().beat();

            assertTrue(records.list.stream().noneMatch(
                            record -> record.getFormattedMessage().contains("keep-alive")),
                    "a heartbeat every two seconds belongs at trace, not debug");
        }
    }
}
