module it.polimi.ingsw2025segriniterribilevarricchiozanca {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    // Export packages needed by Jackson for reflection
    exports it.polimi.ingsw.server.model.domain.general.config to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.server.model.domain.general.loader to com.fasterxml.jackson.databind;
    
    // Open packages for Jackson to use reflection
    opens it.polimi.ingsw.server.model.domain.general.config to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.server.model.domain.general.loader to com.fasterxml.jackson.databind;

    // Open DTO packages to JavaFX for TableView and property bindings
    opens it.polimi.ingsw.common.dto to javafx.base, javafx.controls;
    opens it.polimi.ingsw.common.model to javafx.base;
    opens it.polimi.ingsw.server.model.enums to javafx.base;
    
    exports it.polimi.ingsw.client to javafx.graphics;
    exports it.polimi.ingsw.client.core to javafx.graphics;
    opens it.polimi.ingsw.client.view to javafx.fxml;
}