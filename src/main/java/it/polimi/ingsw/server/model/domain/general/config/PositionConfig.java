package it.polimi.ingsw.server.model.domain.general.config;

import java.io.Serializable;

public record PositionConfig(int x, int y) implements Serializable {
    private static final long serialVersionUID = 1L;
}

