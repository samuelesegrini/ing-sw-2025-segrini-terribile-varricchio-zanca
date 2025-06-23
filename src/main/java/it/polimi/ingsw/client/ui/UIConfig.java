package it.polimi.ingsw.client.ui;

import java.util.HashMap;
import java.util.Map;
/**
 * Configuration for UI initialization.
 */
public class UIConfig {
    private final Map<String, Object> properties;

    public UIConfig() {
        this.properties = new HashMap<>();
    }

    public UIConfig withProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public <T> T getProperty(String key, Class<T> type) {
        return type.cast(properties.get(key));
    }

    public static UIConfig getDefault() {
        return new UIConfig()
                .withProperty("theme", "default")
                .withProperty("fontSize", 12)
                .withProperty("resolution", "1920x1080");
    }
}
