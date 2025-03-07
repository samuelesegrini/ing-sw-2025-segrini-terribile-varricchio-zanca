package it.polimi.ingsw.model.ship.components;

import it.polimi.ingsw.model.ship.Direction;

public class ComponentLoader {
    private static String components_file;
    private ObjectMapper mapper;

    public ComponentLoader() {}
    public void loadComponentPool() {}
    public Component createComponentFromJson(String id, ComponentType type, JsonNode node) {return null;}
    public Map<Direction, ConnectionType> parseConnections(JsonNode node) {return null;}
}
