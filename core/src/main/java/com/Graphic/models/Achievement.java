package com.Graphic.models;

import com.badlogic.gdx.graphics.Texture;

public class Achievement {

    private final AchievementId id;
    private final String titleKey;
    private final String descriptionKey;
    private final Texture icon;
    private boolean unlocked;

    public Achievement(AchievementId id, String titleKey, String descriptionKey, Texture icon, boolean unlocked) {
        this.id = id;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.icon = icon;
        this.unlocked = unlocked;
    }

    public AchievementId getId()            { return id; }
    public String getTitleKey()             { return titleKey; }
    public String getDescriptionKey()       { return descriptionKey; }
    public Texture getIcon()                { return icon; }
    public boolean isUnlocked()             { return unlocked; }


    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}
