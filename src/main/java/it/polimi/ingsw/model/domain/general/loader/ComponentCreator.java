package it.polimi.ingsw.model.domain.general.loader;

import it.polimi.ingsw.model.domain.general.config.ComponentConfig;
import it.polimi.ingsw.model.domain.ship.components.Component;

@FunctionalInterface
public interface ComponentCreator {
    /**
     * Creates a component from the given configuration
     * @param config The component configuration
     * @return The created component
     */
    Component create(ComponentConfig config);
} 