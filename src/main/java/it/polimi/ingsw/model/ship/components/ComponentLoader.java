package it.polimi.ingsw.model.ship.components;

import it.polimi.ingsw.model.ship.Direction;

import java.util.Map;

public class ComponentLoader {
    private static String components_file;
    private ObjectMapper mapper;

    public ComponentLoader() {}
    public void loadComponentPool() {}
    public Component createComponentFromJson(String id, ComponentType type, JsonNode node) {return null;}
    public Map<Direction, ConnectorType> parseConnections(JsonNode node) {return null;}
}
