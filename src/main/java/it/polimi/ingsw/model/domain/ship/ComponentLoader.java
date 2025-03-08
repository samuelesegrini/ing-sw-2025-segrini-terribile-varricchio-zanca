package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public class ComponentLoader {
    private static String components_file;
    private ObjectMapper mapper;

    public ComponentLoader() {}
    public void loadComponentPool() {}
    public Component createComponentFromJson(String id, ComponentType type, JsonNode node) {return null;}
    public Map<Direction, ConnectorType> parseConnections(JsonNode node) {return null;}
}
