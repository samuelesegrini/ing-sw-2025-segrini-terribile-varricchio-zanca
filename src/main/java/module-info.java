module it.polimi.ingsw2025segriniterribilevarricchiozanca {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    // Export packages needed by Jackson for reflection
    exports it.polimi.ingsw.model.domain.general.config to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.model.domain.general.loader to com.fasterxml.jackson.databind;
    
    // Open packages for Jackson to use reflection
    opens it.polimi.ingsw.model.domain.general.config to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.model.domain.general.loader to com.fasterxml.jackson.databind;
}