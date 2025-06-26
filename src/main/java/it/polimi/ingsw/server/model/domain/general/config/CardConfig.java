package it.polimi.ingsw.server.model.domain.general.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Record representing the configuration of a card
 */
public record CardConfig(
    String id,
    String type,
    String description,
    Map<String, Object> properties
) implements Serializable {} 