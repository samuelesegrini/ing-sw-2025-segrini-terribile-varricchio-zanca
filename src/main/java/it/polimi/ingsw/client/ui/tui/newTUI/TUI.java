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

    public TUI(ClientController controller) {
        this.controller = controller;
        this.clientState = controller.getClientState();
        this.scanner = new Scanner(System.in);
    }


    @Override
    public void start() {
        printer.printHeader();
    };

    @Override
    public void shutdown() {};

    @Override
    public boolean isRunning() {return false;}

    @Override
    public void showError(String title, String message) {};

    @Override
    public void showInfo(String title, String message) {};


    public void inputListener(){
        String input;
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for server: " + e.getMessage());
                }
            }
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
}
