package com.Graphic.models;

import com.Graphic.models.enums.GameViewScreen;
import com.badlogic.gdx.Screen;

public class App {
    private static GameViewScreen currentScreen=GameViewScreen.MainMenu;

    public static void setCurrentScreen(GameViewScreen currentScreen) {
        App.currentScreen = currentScreen;
    }

    public static Screen getCurrentScreen() {
        return currentScreen.getScreen();
    }
}
