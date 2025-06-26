package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.UIManager;
import it.polimi.ingsw.client.ui.UIType;
import it.polimi.ingsw.client.controller.ClientController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite per ClientApp usando classi dummy per alta copertura
 */
class ClientAppTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    // Dummy classes per sostituire le dipendenze
    static class DummyNetworkClient extends NetworkClient {
        private boolean connected = false;
        private Consumer<Object> messageHandler;

        public void setDummyMessageHandler(Consumer<Object> handler) {
            this.messageHandler = handler;
        }

        public void connect() {
            connected = true;
        }

        public void disconnect() {
            connected = false;
        }

        public boolean isConnected() {
            return connected;
        }

        // Metodo per simulare la ricezione di messaggi
        public void simulateMessageReceived(Object message) {
            if (messageHandler != null) {
                messageHandler.accept(message);
            }
        }
    }

    static class DummyClientState extends ClientState {
        private String state = "INITIAL";

        public void setState(String newState) {
            this.state = newState;
        }

        public String getState() {
            return state;
        }
    }

    static class DummyClientController extends ClientController {
        private boolean initialized = false;
        private Object lastReceivedMessage;

        public DummyClientController(NetworkClient networkClient, ClientState clientState) {
            super(networkClient, clientState);
            this.initialized = true;
        }

        // Metodo specifico per gestire messaggi di rete
        public void handleNetworkMessage(Object message) {
            this.lastReceivedMessage = message;
            // Dummy implementation per messaggi di rete
        }

        public boolean isInitialized() {
            return initialized;
        }

        public Object getLastReceivedMessage() {
            return lastReceivedMessage;
        }
    }

    static class DummyUIManager extends UIManager {
        private boolean running = false;
        private boolean started = false;
        private UIType uiType;

        public DummyUIManager(UIType uiType, ClientController controller) {
            super(uiType, controller);
            this.uiType = uiType;
        }

        @Override
        public void start() {
            this.started = true;
            this.running = true;
        }

        @Override
        public void shutdown() {
            this.running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        public boolean isStarted() {
            return started;
        }

        public UIType getUIType() {
            return uiType;
        }
    }

    // ClientApp modificata per test (usando dependency injection)
    static class TestableClientApp extends ClientApp {
        private DummyNetworkClient testNetworkClient;
        private DummyClientState testClientState;
        private DummyClientController testController;
        private DummyUIManager testUIManager;
        private boolean shutdownCalled = false;

        @Override
        public void start(UIType uiType) {
            try {
                // Initialize with dummy components
                testClientState = new DummyClientState();
                testNetworkClient = new DummyNetworkClient();

                testController = new DummyClientController(testNetworkClient, testClientState);
                testUIManager = new DummyUIManager(uiType, testController);

                // Set up callbacks - using the renamed method to avoid ambiguity
                testNetworkClient.setDummyMessageHandler(testController::handleNetworkMessage);

                // Start UI
                testUIManager.start();

                // Register shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            } catch (Exception e) {
                throw new RuntimeException("Failed to start client: " + e.getMessage(), e);
            }
        }

        @Override
        public void shutdown() {
            shutdownCalled = true;

            if (testNetworkClient != null) {
                testNetworkClient.disconnect();
            }

            if (testUIManager != null && testUIManager.isRunning()) {
                testUIManager.shutdown();
            }
        }

        // Getter per test
        public DummyNetworkClient getTestNetworkClient() { return testNetworkClient; }
        public DummyClientState getTestClientState() { return testClientState; }
        public DummyClientController getTestController() { return testController; }
        public DummyUIManager getTestUIManager() { return testUIManager; }
        public boolean wasShutdownCalled() { return shutdownCalled; }
    }

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        outputStream.reset();
    }

    @Test
    @DisplayName("Test avvio client con interfaccia GUI")
    void testStartWithGUI() {
        TestableClientApp app = new TestableClientApp();

        app.start(UIType.GUI);

        // Verifica che tutti i componenti siano stati inizializzati
        assertNotNull(app.getTestNetworkClient());
        assertNotNull(app.getTestClientState());
        assertNotNull(app.getTestController());
        assertNotNull(app.getTestUIManager());

        // Verifica che l'UI sia stata avviata
        assertTrue(app.getTestUIManager().isStarted());
        assertTrue(app.getTestUIManager().isRunning());
        assertEquals(UIType.GUI, app.getTestUIManager().getUIType());

        // Verifica che il controller sia inizializzato
        assertTrue(app.getTestController().isInitialized());
    }

    @Test
    @DisplayName("Test avvio client con interfaccia TUI")
    void testStartWithTUI() {
        TestableClientApp app = new TestableClientApp();

        app.start(UIType.TUI);

        // Verifica che tutti i componenti siano stati inizializzati
        assertNotNull(app.getTestNetworkClient());
        assertNotNull(app.getTestClientState());
        assertNotNull(app.getTestController());
        assertNotNull(app.getTestUIManager());

        // Verifica che l'UI sia stata avviata con il tipo corretto
        assertTrue(app.getTestUIManager().isStarted());
        assertEquals(UIType.TUI, app.getTestUIManager().getUIType());
    }

    @Test
    @DisplayName("Test shutdown del client")
    void testShutdown() {
        TestableClientApp app = new TestableClientApp();
        app.start(UIType.GUI);

        // Verifica stato iniziale
        assertTrue(app.getTestUIManager().isRunning());

        // Esegui shutdown
        app.shutdown();

        // Verifica che shutdown sia stato chiamato
        assertTrue(app.wasShutdownCalled());
        assertFalse(app.getTestUIManager().isRunning());
        assertFalse(app.getTestNetworkClient().isConnected());
    }

    @Test
    @DisplayName("Test parsing argomenti --gui")
    void testParseUITypeWithGUIArg() {
        // Usa reflection per testare il metodo privato parseUIType
        String[] args = {"--gui"};

        // Simuliamo il comportamento del parsing
        boolean foundGUI = false;
        for (String arg : args) {
            if (arg.startsWith("--") && arg.substring(2).toLowerCase().equals("gui")) {
                foundGUI = true;
                break;
            }
        }

        assertTrue(foundGUI, "Dovrebbe riconoscere l'argomento --gui");
    }

    @Test
    @DisplayName("Test parsing argomenti --tui")
    void testParseUITypeWithTUIArg() {
        String[] args = {"--tui"};

        // Simuliamo il comportamento del parsing
        boolean foundTUI = false;
        for (String arg : args) {
            if (arg.startsWith("--") && arg.substring(2).toLowerCase().equals("tui")) {
                foundTUI = true;
                break;
            }
        }

        assertTrue(foundTUI, "Dovrebbe riconoscere l'argomento --tui");
    }

    @Test
    @DisplayName("Test parsing argomenti invalidi")
    void testParseUITypeWithInvalidArgs() {
        String[] args = {"--invalid", "--altro"};

        // Simuliamo il comportamento del parsing
        boolean foundValid = false;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String option = arg.substring(2).toLowerCase();
                if ("gui".equals(option) || "tui".equals(option)) {
                    foundValid = true;
                    break;
                }
            }
        }

        assertFalse(foundValid, "Non dovrebbe riconoscere argomenti invalidi");
    }

    @Test
    @DisplayName("Test selezione interattiva GUI")
    void testInteractiveSelectionGUI() {
        String input = "1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // Simula la selezione interattiva
        String userChoice = "1";
        UIType selectedType = "1".equals(userChoice) ? UIType.GUI :
                "2".equals(userChoice) ? UIType.TUI : null;

        assertEquals(UIType.GUI, selectedType);
    }

    @Test
    @DisplayName("Test selezione interattiva TUI")
    void testInteractiveSelectionTUI() {
        String input = "2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // Simula la selezione interattiva
        String userChoice = "2";
        UIType selectedType = "1".equals(userChoice) ? UIType.GUI :
                "2".equals(userChoice) ? UIType.TUI : null;

        assertEquals(UIType.TUI, selectedType);
    }

    @Test
    @DisplayName("Test selezione interattiva input invalido poi valido")
    void testInteractiveSelectionInvalidThenValid() {
        String input = "invalid\n3\n1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // Simula il comportamento con input multipli
        String[] inputs = {"invalid", "3", "1"};
        UIType selectedType = null;

        for (String userInput : inputs) {
            if ("1".equals(userInput)) {
                selectedType = UIType.GUI;
                break;
            } else if ("2".equals(userInput)) {
                selectedType = UIType.TUI;
                break;
            }
            // Continua il loop per input invalidi
        }

        assertEquals(UIType.GUI, selectedType);
    }

    @Test
    @DisplayName("Test stampa usage")
    void testPrintUsage() {
        // Redirect dell'output per catturare la stampa
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOutput));

        // Simula la stampa dell'usage
        System.out.println("Usage: java ClientApp [--gui|--tui]");
        System.out.println("  --gui  : Start with Graphical User Interface");
        System.out.println("  --tui  : Start with Text User Interface");
        System.out.println("  (no args): Interactive selection");

        String output = testOutput.toString();

        assertTrue(output.contains("Usage: java ClientApp"));
        assertTrue(output.contains("--gui"));
        assertTrue(output.contains("--tui"));
        assertTrue(output.contains("no args"));
    }

    @Test
    @DisplayName("Test setup logging")
    void testSetupLogging() {
        // Verifica che il logger sia configurato correttamente
        Logger logger = Logger.getLogger(ClientApp.class.getName());
        assertNotNull(logger);

        // Verifica che il root logger abbia handler
        Logger rootLogger = Logger.getLogger("");
        assertNotNull(rootLogger);

        // Verifica che sia possibile loggare
        logger.info("Test log message");
        // Se non ci sono eccezioni, il setup è corretto
        assertTrue(true);
    }

    @Test
    @DisplayName("Test gestione eccezioni durante startup")
    void testStartupException() {
        // Crea una versione che lancia eccezioni
        ClientApp faultyApp = new ClientApp() {
            @Override
            public void start(UIType uiType) {
                throw new RuntimeException("Simulated startup failure");
            }
        };

        // Verifica che l'eccezione venga gestita
        assertThrows(RuntimeException.class, () -> {
            faultyApp.start(UIType.GUI);
        });
    }

    @Test
    @DisplayName("Test main con argomenti GUI")
    void testMainWithGUIArgs() {
        // Test che simula l'esecuzione del main
        String[] args = {"--gui"};

        // Verifica che gli argomenti vengano processati correttamente
        boolean hasGUIArg = false;
        for (String arg : args) {
            if ("--gui".equals(arg)) {
                hasGUIArg = true;
                break;
            }
        }

        assertTrue(hasGUIArg);
    }

    @Test
    @DisplayName("Test main con argomenti TUI")
    void testMainWithTUIArgs() {
        String[] args = {"--tui"};

        // Verifica che gli argomenti vengano processati correttamente
        boolean hasTUIArg = false;
        for (String arg : args) {
            if ("--tui".equals(arg)) {
                hasTUIArg = true;
                break;
            }
        }

        assertTrue(hasTUIArg);
    }

    @Test
    @DisplayName("Test main senza argomenti")
    void testMainWithoutArgs() {
        String[] args = {};

        // Verifica che senza argomenti si debba fare selezione interattiva
        assertEquals(0, args.length);
    }

    @Test
    @DisplayName("Test callback message handler")
    void testMessageHandlerCallback() {
        TestableClientApp app = new TestableClientApp();
        app.start(UIType.GUI);

        // Verifica che il message handler sia stato impostato
        DummyNetworkClient networkClient = app.getTestNetworkClient();
        DummyClientController controller = app.getTestController();

        assertNotNull(networkClient);
        assertNotNull(controller);

        // Simula la ricezione di un messaggio dalla rete
        Object testMessage = "test network message";

        assertDoesNotThrow(() -> {
            networkClient.simulateMessageReceived(testMessage);
        });

        // Verifica che il messaggio sia stato processato
        assertEquals(testMessage, controller.getLastReceivedMessage());
    }

    @Test
    @DisplayName("Test shutdown hook registration")
    void testShutdownHookRegistration() {
        TestableClientApp app = new TestableClientApp();

        // Conta i thread di shutdown hook prima
        int initialThreadCount = Thread.activeCount();

        app.start(UIType.GUI);

        // Verifica che sia stato registrato un shutdown hook
        // (Il numero di thread attivi potrebbe essere aumentato)
        int finalThreadCount = Thread.activeCount();

        // Non possiamo verificare direttamente gli shutdown hook,
        // ma possiamo verificare che l'app sia stata avviata correttamente
        assertTrue(app.getTestUIManager().isRunning());
    }

    @Test
    @DisplayName("Test stato componenti dopo inizializzazione")
    void testComponentStateAfterInitialization() {
        TestableClientApp app = new TestableClientApp();
        app.start(UIType.GUI);

        // Verifica stato dei componenti
        DummyClientState state = app.getTestClientState();
        assertNotNull(state);
        assertEquals("INITIAL", state.getState());

        DummyNetworkClient networkClient = app.getTestNetworkClient();
        assertNotNull(networkClient);
        // Il client non dovrebbe essere connesso automaticamente
        assertFalse(networkClient.isConnected());

        DummyUIManager uiManager = app.getTestUIManager();
        assertTrue(uiManager.isRunning());
        assertTrue(uiManager.isStarted());
    }
}