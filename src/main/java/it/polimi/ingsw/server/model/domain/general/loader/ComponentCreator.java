package it.polimi.ingsw.server.model.domain.general.loader;

import it.polimi.ingsw.server.model.domain.general.config.ComponentConfig;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

@FunctionalInterface
public interface ComponentCreator {
    /**
     * Creates a component from the given configuration
     * @param config The component configuration
     * @return The created component
     */
    Component create(ComponentConfig config);
} 