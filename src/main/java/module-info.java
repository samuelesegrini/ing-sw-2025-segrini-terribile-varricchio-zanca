// In src/main/java/module-info.java (or wherever your module descriptor is)

module it.polimi.ingsw2025segriniterribilevarricchiozanca {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;

    // --- RMI Requirements ---
    requires java.rmi;
    requires com.fasterxml.jackson.databind;

    // --- Exports and Opens for RMI ---
    exports it.polimi.ingsw.common.network.rmi to java.rmi;

    exports it.polimi.ingsw.common.message;
    exports it.polimi.ingsw.common.dto;

    opens it.polimi.ingsw.common.network.rmi to java.rmi;
    opens it.polimi.ingsw.common.message to java.rmi;
    opens it.polimi.ingsw.common.dto to java.rmi;

    opens it.polimi.ingsw.common.message.system to java.rmi;
    exports it.polimi.ingsw.common.message.system;

    opens it.polimi.ingsw.common.message.setup to java.rmi;
    exports it.polimi.ingsw.common.message.setup;
    opens it.polimi.ingsw.common.message.building to java.rmi;
    exports it.polimi.ingsw.common.message.building;


    // --- Your application's main packages that need to be opened for FXML ---
    opens it.polimi.ingsw.client.core to javafx.fxml;
    exports it.polimi.ingsw.client.core;

    opens it.polimi.ingsw.client.view to javafx.fxml;
    exports it.polimi.ingsw.client.view;

    opens it.polimi.ingsw.client.model to javafx.base;
    exports it.polimi.ingsw.client.model;
}