package com.Graphic.managers;

import com.Graphic.utils.GameAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class InputManager {
    private static final String PREFS_NAME = "game_keybindings";
    private static Preferences prefs;

    public static void init() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);


        for (GameAction action : GameAction.values()) {
            if (!prefs.contains(action.name())) {
                prefs.putInteger(action.name(), action.getDefaultKeyCode());
            }
        }
        prefs.flush();
    }




    public static int getKeyCode(GameAction action) {
        return prefs.getInteger(action.name());
    }


    public static String getKeyName(GameAction action) {
        return Input.Keys.toString(prefs.getInteger(action.name()));
    }




    public static void updateKeybinding(GameAction action, int newKeyCode) {
        prefs.putInteger(action.name(), newKeyCode);
        prefs.flush();
    }


    public static void resetToDefaults() {
        for (GameAction action : GameAction.values()) {
            prefs.putInteger(action.name(), action.getDefaultKeyCode());
        }
        prefs.flush();
    }
}
