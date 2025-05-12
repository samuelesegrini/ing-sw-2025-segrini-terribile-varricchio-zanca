package it.polimi.ingsw.client.view;

import it.polimi.ingsw.common.dto.GameSettingsDTO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.Optional;

/**
 * Dialog for creating a new game with custom settings.
 * Provides fields to configure game name, player count, and difficulty level.
 */
public class GameSettingsDialog {
    
    /**
     * Shows the game settings dialog and returns the user's input.
     * 
     * @param owner The owner stage for this dialog
     * @return An Optional containing the GameSettingsDTO if confirmed, or empty if canceled
     */
    public Optional<GameSettingsDTO> showAndWait(Stage owner) {
        // Create the custom dialog
        Dialog<GameSettingsDTO> dialog = new Dialog<>();
        dialog.setTitle("Create New Game");
        dialog.setHeaderText("Enter Game Settings");
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        // Set the button types
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Create the grid and add components
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField gameName = new TextField();
        gameName.setPromptText("Galaxy Adventure");
        
        ComboBox<Integer> maxPlayers = new ComboBox<>();
        maxPlayers.getItems().addAll(2, 3, 4);
        maxPlayers.setValue(4);
        
        ComboBox<String> gameLevel = new ComboBox<>();
        gameLevel.getItems().addAll("TEST FLIGHT", "LEVEL II");
        gameLevel.setValue("LEVEL II");
        
        grid.add(new Label("Game Name:"), 0, 0);
        grid.add(gameName, 1, 0);
        grid.add(new Label("Max Players:"), 0, 1);
        grid.add(maxPlayers, 1, 1);
        grid.add(new Label("Game Level:"), 0, 2);
        grid.add(gameLevel, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the game name field by default
        gameName.requestFocus();
        
        // Convert the result to GameSettingsDTO when the create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                if (gameName.getText().trim().isEmpty()) {
                    // Show error for empty game name
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Settings");
                    alert.setHeaderText(null);
                    alert.setContentText("Game name cannot be empty.");
                    alert.showAndWait();
                    return null;
                }
                return new GameSettingsDTO(
                    gameName.getText().trim(),
                    maxPlayers.getValue(),
                    gameLevel.getValue().equals("TEST FLIGHT") ? GameLevel.TEST_FLIGHT : GameLevel.LEVEL_II
                );
            }
            return null;
        });
        
        // Show the dialog and return the result
        return dialog.showAndWait();
    }
} 