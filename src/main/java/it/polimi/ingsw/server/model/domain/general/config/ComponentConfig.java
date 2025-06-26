package it.polimi.ingsw.server.model.domain.general.config;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Record representing the configuration of a component
 */
public record ComponentConfig(
    String id,
    String type,
    List<String> connectors,
    Map<String, Object> properties
) implements Serializable {} 