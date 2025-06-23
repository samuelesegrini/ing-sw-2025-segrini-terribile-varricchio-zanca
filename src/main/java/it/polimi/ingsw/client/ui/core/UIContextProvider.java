package it.polimi.ingsw.client.ui.core;

public class UIContextProvider {
    private static volatile UIContext current;
    public static UIContext getCurrent() { return current; }
    public static void setCurrent(UIContext ctx) { current = ctx; }
} 