package com.Graphic.managers;

import com.Graphic.views.atmosphere.MenuAtmosphere;
import com.Graphic.views.atmosphere.Theme;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
public class VideoManager {

    private static final String PREFS_NAME = "video_settings";

    private static Preferences prefs;

    private static float                brightness = 1.0f;
    private static Theme theme       = Theme.VOID;

    public static void init() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        brightness = prefs.getFloat("brightness", 1.0f);

        String savedTheme = prefs.getString("theme", Theme.VOID.name());
        try {
            theme = Theme.valueOf(savedTheme);
        } catch (IllegalArgumentException e) {
            Gdx.app.error("VideoManager", "Unknown saved theme '" + savedTheme + "', defaulting to VOID");
            theme = Theme.VOID;
        }

        applyTheme();
    }

    public static void setBrightness(float b) {
        brightness = b;
        save();
    }

    public static void setTheme(Theme t) {
        theme = t;
        applyTheme();
        save();
    }

    public static void resetDefaults() {
        setBrightness(1.0f);
        setTheme(Theme.VOID);
    }

    public static float                getBrightness() { return brightness; }
    public static Theme getTheme()       { return theme; }

    private static void applyTheme() {
        MenuAtmosphere.getInstance().applyTheme(theme, false);
    }

    private static void save() {
        prefs.putFloat("brightness", brightness);
        prefs.putString("theme", theme.name());
        prefs.flush();
    }
}
