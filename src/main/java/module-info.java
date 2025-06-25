module it.polimi.ingsw2025segriniterribilevarricchiozanca {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires org.jline;

    // --- RMI Requirements ---
    requires java.rmi;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires java.desktop;
    requires org.fusesource.jansi;

    // --- Exports and Opens for RMI ---
    exports it.polimi.ingsw.common.network.rmi to java.rmi;
    exports it.polimi.ingsw.common.message;
    exports it.polimi.ingsw.common.message.event;
    exports it.polimi.ingsw.common.message.request;
    exports it.polimi.ingsw.common.message.response;

    opens it.polimi.ingsw.common.network.rmi to java.rmi;
    opens it.polimi.ingsw.common.message to java.rmi;
    opens it.polimi.ingsw.common.message.event to java.rmi;
    opens it.polimi.ingsw.common.message.request to java.rmi;
    opens it.polimi.ingsw.common.message.response to java.rmi;

    // Jackson needs access to config classes for JSON deserialization
    opens it.polimi.ingsw.server.model.domain.general.config to com.fasterxml.jackson.databind;

    // --- Client packages ---
    exports it.polimi.ingsw.client.core;
    exports it.polimi.ingsw.client.controller;
    exports it.polimi.ingsw.client.network;
    exports it.polimi.ingsw.client.ui;
    exports it.polimi.ingsw.client.ui.core;
    exports it.polimi.ingsw.client.ui.gui;
    exports it.polimi.ingsw.client.ui.gui.views;
    exports it.polimi.ingsw.client.ui.tui;
    exports it.polimi.ingsw.client.ui.tui.views;

    // NOTE: it.polimi.ingsw.client package removed - no Java files in root client package
    opens it.polimi.ingsw.client.core to javafx.fxml;
    opens it.polimi.ingsw.client.ui.gui to javafx.fxml;
    opens it.polimi.ingsw.client.ui.gui.views to javafx.fxml;

    // --- Server packages for shared model classes ---
    exports it.polimi.ingsw.server.model.domain.ship;
    exports it.polimi.ingsw.server.model.enums;
    exports it.polimi.ingsw.server.model.enums.ship;

    // --- Common packages ---
    // NOTE: client.core.state package removed as part of Simple Direct Model Architecture
}