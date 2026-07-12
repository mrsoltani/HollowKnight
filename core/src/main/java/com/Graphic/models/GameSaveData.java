package com.Graphic.models;

import com.Graphic.models.enums.GameArea;
import com.Graphic.models.charms.CharmId;

public class GameSaveData {
    public int slotId;
    public float timePlayed;
    public int enemiesKilled;
    public int deaths;
    public boolean charmAcquired;
    public boolean wallBroken;
    public boolean gameBeaten;

    public GameArea lastArea;

    // Up to 3 equipped charm slots (NONE = empty slot)
    public CharmId equippedCharm1;
    public CharmId equippedCharm2;
    public CharmId equippedCharm3;

    // Constructor for a brand new game
    public GameSaveData(int slotId) {
        this.slotId = slotId;
        this.timePlayed = 0f;
        this.enemiesKilled = 0;
        this.deaths = 0;
        this.charmAcquired = false;
        this.wallBroken = false;
        this.gameBeaten = false;
        this.lastArea = GameArea.NONE; // Empty slot

        this.equippedCharm1 = CharmId.NONE;
        this.equippedCharm2 = CharmId.NONE;
        this.equippedCharm3 = CharmId.NONE;
    }

    // Constructor for loading from the DB
    public GameSaveData(int slotId, float timePlayed, int enemiesKilled, int deaths,
                        boolean charmAcquired, boolean wallBroken, boolean gameBeaten, String areaString,
                        String equippedCharm1Str, String equippedCharm2Str, String equippedCharm3Str) {
        this.slotId = slotId;
        this.timePlayed = timePlayed;
        this.enemiesKilled = enemiesKilled;
        this.deaths = deaths;
        this.charmAcquired = charmAcquired;
        this.wallBroken = wallBroken;
        this.gameBeaten = gameBeaten;

        try {
            this.lastArea = GameArea.valueOf(areaString);
        } catch (Exception e) {
            this.lastArea = GameArea.NONE; // Fallback
        }

        this.equippedCharm1 = parseCharmId(equippedCharm1Str);
        this.equippedCharm2 = parseCharmId(equippedCharm2Str);
        this.equippedCharm3 = parseCharmId(equippedCharm3Str);
    }

    private static CharmId parseCharmId(String value) {
        try {
            return CharmId.valueOf(value);
        } catch (Exception e) {
            return CharmId.NONE; // Fallback for null/unknown values
        }
    }
}
