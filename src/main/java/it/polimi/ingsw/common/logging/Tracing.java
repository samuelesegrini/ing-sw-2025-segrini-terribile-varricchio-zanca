package it.polimi.ingsw.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Whether this process says what it is doing, and where it says it.
 *
 * <p><b>Off by default, deliberately.</b> {@code slf4j} and {@code logback} were on the
 * classpath for a long time with nothing configuring them, which meant an unconfigured run
 * would have fallen back to Logback's own default — the root logger at {@code DEBUG}, straight
 * to the console. Adopting logging without configuring it makes production the noisy case,
 * which is exactly backwards, and the person it would have been noisy in front of is an
 * examiner.
 *
 * <p><b>The two sides cannot share an answer.</b> The server owns its terminal and can write
 * to it; the client is drawing a ship, a board and a set of prompts on that same terminal, and
 * a log line arriving mid-frame tears the picture apart. {@code stderr} is no escape — it is
 * the same terminal. So the server traces to the console and the client traces to a file,
 * always, and each entry point says which it is before anything has a chance to log.
 *
 * <p>The console pattern is the bare message. That is not laziness: the server's startup lines
 * are the program talking to whoever ran it, and they have to come out looking exactly as they
 * did when they were {@code System.out.println}. A file nobody is watching live is a different
 * matter, so the file pattern carries the time, the level and the logger.
 */
public final class Tracing {

    /** The command-line flag that turns the trace on, on either jar. */
    public static final String FLAG = "--debug";

    /** The command-line option that says where the client writes its log. */
    public static final String LOG_OPTION = "--log";

    /** The environment variable that does the same as {@link #FLAG}, for launch scripts. */
    public static final String DEBUG_VARIABLE = "GT_DEBUG";

    /** The environment variable that does the same as {@link #LOG_OPTION}. */
    public static final String LOG_VARIABLE = "GT_LOG";

    /** Where a client writes its log when nobody says otherwise. */
    public static final Path DEFAULT_CLIENT_LOG = Path.of("galaxy-trucker-client.log");

    /** Everything this project logs sits under one name, so one line raises all of it. */
    private static final String OURS = "it.polimi.ingsw";

    private static final String FILE_PATTERN =
            "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n";

    private Tracing() {
    }

    /**
     * Tells whether this process was asked to trace.
     *
     * <p>The environment is passed in rather than read, so that a test can answer the question
     * without the answer depending on the machine it runs on.
     *
     * @param args        the command line, in any order
     * @param environment where to look a variable up, usually {@code System::getenv}
     * @return {@code true} if the flag was given or the variable is set to anything but
     *         {@code 0}, {@code false} or an empty string
     */
    public static boolean wanted(String[] args, UnaryOperator<String> environment) {
        for (String argument : args) {
            if (FLAG.equalsIgnoreCase(argument)) {
                return true;
            }
        }
        return isTrue(environment.apply(DEBUG_VARIABLE));
    }

    /**
     * Returns where a client should write its log.
     *
     * @param args        the command line, in any order
     * @param environment where to look a variable up, usually {@code System::getenv}
     * @return the file named on the command line, or in the environment, or the default
     */
    public static Path logFile(String[] args, UnaryOperator<String> environment) {
        for (int at = 0; at < args.length - 1; at++) {
            if (LOG_OPTION.equalsIgnoreCase(args[at])) {
                return Path.of(args[at + 1]);
            }
        }
        String named = environment.apply(LOG_VARIABLE);
        return named == null || named.isBlank() ? DEFAULT_CLIENT_LOG : Path.of(named);
    }

    /**
     * Returns the command line with the tracing arguments taken out.
     *
     * <p>So that everything else parses exactly as it always did, whichever order the flag was
     * given in. The alternative — teaching {@code Startup} and the server's port reader about
     * a flag that is not theirs — would put the same option in two parsers.
     *
     * @param args the command line
     * @return what is left of it
     */
    public static String[] without(String[] args) {
        List<String> kept = new ArrayList<>(args.length);
        for (int at = 0; at < args.length; at++) {
            if (FLAG.equalsIgnoreCase(args[at])) {
                continue;
            }
            if (LOG_OPTION.equalsIgnoreCase(args[at])) {
                at++;
                continue;
            }
            kept.add(args[at]);
        }
        return kept.toArray(String[]::new);
    }

    /**
     * Leaves the trace on the console, where the shared configuration already puts it.
     *
     * <p>For the server, which owns its terminal.
     *
     * @param debug whether to trace at all
     */
    public static void toConsole(boolean debug) {
        ours().setLevel(debug ? Level.DEBUG : Level.INFO);
    }

    /**
     * Sends everything to a file and nothing to the console.
     *
     * <p>For the client, whose terminal belongs to the interface. Every appender the shared
     * configuration attached is detached first — not merely added to — because the point is
     * not that a file also gets the lines, it is that the terminal does not.
     *
     * @param file  where to write
     * @param debug whether to trace at all, or only report what goes wrong
     */
    public static void toFile(Path file, boolean debug) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();

        PatternLayoutEncoder layout = new PatternLayoutEncoder();
        layout.setContext(context);
        layout.setPattern(FILE_PATTERN);
        layout.start();

        FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> toTheFile = new FileAppender<>();
        toTheFile.setContext(context);
        toTheFile.setFile(file.toString());
        toTheFile.setAppend(true);
        toTheFile.setEncoder(layout);
        toTheFile.start();

        root.addAppender(toTheFile);
        ours().setLevel(debug ? Level.DEBUG : Level.INFO);
    }

    private static Logger ours() {
        return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(OURS);
    }

    private static boolean isTrue(String value) {
        if (value == null) {
            return false;
        }
        String said = value.strip().toLowerCase(Locale.ROOT);
        return !(said.isEmpty() || said.equals("0") || said.equals("false") || said.equals("no"));
    }

    /**
     * Returns the tracing arguments, for a usage line that has to list them.
     *
     * @return how to ask for a trace
     */
    public static String usage() {
        return "[" + FLAG + "] [" + LOG_OPTION + " <file>]";
    }

    static List<String> flags() {
        return Arrays.asList(FLAG, LOG_OPTION);
    }
}
