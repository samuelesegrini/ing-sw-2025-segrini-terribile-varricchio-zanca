package it.polimi.ingsw.client.ui.tui.newTUI;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.UI;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.Scanner;

public class TUI implements UI {
    private final Scanner scanner;
    private final Printer printer = new Printer();
    private final ClientController controller;
    private final ClientState clientState;

    private final Object lock = new Object();
    private volatile boolean running = false;

    public TUI(ClientController controller) {
        this.controller = controller;
        this.clientState = controller.getClientState();
        this.scanner = new Scanner(System.in);
    }


    @Override
    public void start() {
        running = true;
        printer.printHeader();
        
        // Start input listener in a separate thread
        Thread inputThread = new Thread(this::inputListener, "TUI-InputListener");
        inputThread.setDaemon(false); // Keep JVM alive
        inputThread.start();
        
        // Show initial connection prompt
        printer.printConnectionPrompt();
        
        // Wake up the input listener
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void shutdown() {
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    };

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void showError(String title, String message) {};

    @Override
    public void showInfo(String title, String message) {};


    public void inputListener(){
        String input;
        while (running) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    if (!running) break;
                    System.err.println("Interrupted while waiting for server: " + e.getMessage());
                }
            }
            if (!running) break;
            
            System.out.print("> ");
            input = scanner.nextLine();
            elaborateInput(input);
        }
    }

    private void elaborateInput(String input) {
        switch(clientState.getCurrentView()){
            case CONNECTION:
                elaborateConnectionCommand(input);
                break;
            case LOGIN:
                elaborateLoginCommand(input);
                break;
            case LOBBY:
                elaborateLobbyCommand(input);
                break;
            case GAME_LOBBY:
                elaborateGameLobbyCommand(input);
                break;
            case GAME:
                elaborateGameCommand(input);
                break;
            default:
                System.err.println("Stato client non supportato ");
        }
    }

    private void elaborateConnectionCommand(String input) {
        // TODO: Implement connection command handling
        System.out.println("Connection command: " + input);
    }

    private void elaborateLoginCommand(String input) {
        // TODO: Implement login command handling
        System.out.println("Login command: " + input);
    }

    private void elaborateLobbyCommand(String input) {
        // TODO: Implement lobby command handling
        System.out.println("Lobby command: " + input);
    }

    private void elaborateGameLobbyCommand(String input) {
        // TODO: Implement game lobby command handling
        System.out.println("Game lobby command: " + input);
    }

    private void elaborateGameCommand(String input) {
        // TODO: Implement game command handling
        System.out.println("Game command: " + input);
    }
}
