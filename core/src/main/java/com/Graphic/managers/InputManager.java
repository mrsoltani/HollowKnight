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

        // Loop through all actions to see if defaults need initialization
        for (GameAction action : GameAction.values()) {
            if (!prefs.contains(action.name())) {
                prefs.putInteger(action.name(), action.getDefaultKeyCode());
            }
        }
        prefs.flush(); // Commit any initialization changes to disk
    }

    // --- Core Accessors ---

    /** Returns the underlying integer keycode (e.g., 62 for spacebar) */
    public static int getKeyCode(GameAction action) {
        return prefs.getInteger(action.name());
    }

    /** Returns the string representation of the key bound to this action (e.g., "SPACE", "Z") */
    public static String getKeyName(GameAction action) {
        return Input.Keys.toString(prefs.getInteger(action.name()));
    }

    // --- Dynamic Modifier ---

    /** Instantly updates the preference map and flushes changes to disk */
    public static void updateKeybinding(GameAction action, int newKeyCode) {
        prefs.putInteger(action.name(), newKeyCode);
        prefs.flush();
    }

    /** Resets all keys back to factory defaults */
    public static void resetToDefaults() {
        for (GameAction action : GameAction.values()) {
            prefs.putInteger(action.name(), action.getDefaultKeyCode());
        }
        prefs.flush();
    }
}
