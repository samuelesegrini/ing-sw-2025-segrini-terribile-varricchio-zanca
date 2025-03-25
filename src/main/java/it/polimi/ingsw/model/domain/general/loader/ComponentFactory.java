package it.polimi.ingsw.model.domain.general.loader;

import it.polimi.ingsw.model.domain.general.config.ComponentConfig;
import it.polimi.ingsw.model.domain.ship.components.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class responsible for creating different types of components
 */
public class ComponentFactory {
    private final Map<String, ComponentCreator> creators;

    public ComponentFactory() {
        this.creators = new HashMap<>();
        initializeCreators();
    }

    /**
     * Registers a creator for a specific component type
     */
    public void registerCreator(String type, ComponentCreator creator) {
        creators.put(type, creator);
    }

    /**
     * Creates a component from the given configuration
     */
    public Component createComponent(ComponentConfig config) {
        ComponentCreator creator = creators.get(config.type());
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for component type: " + config.type());
        }
        return creator.create(config);
    }

    /**
     * Creates a cabin component
     */
    private Component createCabin(ComponentConfig config) {
        // Implementation will depend on your specific component types and requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Creates an engine component
     */
    private Component createEngine(ComponentConfig config) {
        // Implementation will depend on your specific component types and requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Configures the sides of an abstract component
     */
    private void configureSides(Component component, List<String> sides) {
        // Implementation will depend on your component side configuration requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void initializeCreators() {
        // Register creators for different component types
        registerCreator("CABIN", this::createCabin);
        registerCreator("ENGINE", this::createEngine);
        // Add more component type creators as needed
    }
} 