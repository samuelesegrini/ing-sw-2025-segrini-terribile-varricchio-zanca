package it.polimi.ingsw2025segriniterribilevarricchiozanca;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Everything is fine in this project");
    }
}