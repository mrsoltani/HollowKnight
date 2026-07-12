package com.Graphic.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalizationManager {
    public enum Language { EN, FR }

    private static final String PREFS_NAME = "localization_settings";

    private static Preferences prefs;
    private static I18NBundle  bundle;
    private static Language    current = Language.EN;


    private static final List<Runnable> listeners = new ArrayList<>();

    public static void load() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);

        String saved = prefs.getString("language", Language.EN.name());
        Language lang;
        try {
            lang = Language.valueOf(saved);
        } catch (IllegalArgumentException e) {
            Gdx.app.error("LocalizationManager", "Unknown saved language '" + saved + "', defaulting to EN");
            lang = Language.EN;
        }

        setLanguage(lang);
    }

    public static void setLanguage(Language lang) {
        current = lang;
        Locale locale = lang == Language.EN ? Locale.ENGLISH : Locale.FRENCH;
        bundle = I18NBundle.createBundle(
            Gdx.files.internal("i18n/strings"), locale, "UTF-8"
        );

        if (prefs != null) {
            prefs.putString("language", lang.name());
            prefs.flush();
        }

        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public static String get(String key) {
        return bundle.get(key);
    }

    public static Language getCurrent() { return current; }

    public static void addListener(Runnable onLanguageChange) {
        listeners.add(onLanguageChange);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
