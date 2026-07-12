package com.Graphic.managers;


import com.Graphic.models.AchievementId;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class PreferencesAchievementStore implements AchievementStore {

    private static final String PREFS_NAME = "achievements";
    private final Preferences prefs;

    public PreferencesAchievementStore() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
    }

    @Override
    public boolean isUnlocked(AchievementId id) {
        return prefs.getBoolean(id.name(), false);
    }

    @Override
    public void setUnlocked(AchievementId id, boolean unlocked) {
        prefs.putBoolean(id.name(), unlocked);
        prefs.flush();
    }
}
