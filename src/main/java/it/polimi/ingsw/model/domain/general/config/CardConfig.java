package it.polimi.ingsw.model.domain.general.config;

import java.util.Map;

/**
 * Record representing the configuration of a card
 */
public record CardConfig(
    String id,
    String type,
    String description,
    Map<String, Object> properties
) {} 