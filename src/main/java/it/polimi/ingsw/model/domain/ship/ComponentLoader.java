package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public class ComponentLoader {
    private static String components_file;
    
    //TODO: check the JsonNode import and what to use to handle it
    //private ObjectMapper mapper;

    public ComponentLoader() {}
    public void loadComponentPool() {}
    public Component createComponentFromJson(String id, ComponentType type/*, JsonNode node*/) {return null;}

    //TODO: check the JsonNode import and what to use to handle it
    public Map<Direction, ConnectorType> parseConnections(/*JsonNode node*/) {return null;}
}
