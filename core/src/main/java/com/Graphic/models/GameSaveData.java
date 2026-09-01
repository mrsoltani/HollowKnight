package com.Graphic.models;

import com.Graphic.models.enums.GameArea;
import com.Graphic.models.charms.CharmId;

public class GameSaveData {
    public int slotId;
    public float timePlayed;
    public int enemiesKilled;
    public int deaths;
    public boolean charmAcquired1; // Sharp Shadow
    public boolean charmAcquired2; // Void Heart
    public boolean wallBroken;
    public boolean gameBeaten;

    public GameArea lastArea;

    public CharmId equippedCharm1;
    public CharmId equippedCharm2;
    public CharmId equippedCharm3;

    public GameSaveData(int slotId) {
        this.slotId = slotId;
        this.timePlayed = 0f;
        this.enemiesKilled = 0;
        this.deaths = 0;
        this.charmAcquired1 = false;
        this.charmAcquired2 = false;
        this.wallBroken = false;
        this.gameBeaten = false;
        this.lastArea = GameArea.NONE;

        this.equippedCharm1 = CharmId.NONE;
        this.equippedCharm2 = CharmId.NONE;
        this.equippedCharm3 = CharmId.NONE;
    }

    public GameSaveData(int slotId, float timePlayed, int enemiesKilled, int deaths,
                        boolean charmAcquired1, boolean charmAcquired2, boolean wallBroken, boolean gameBeaten,
                        String areaString,
                        String equippedCharm1Str, String equippedCharm2Str, String equippedCharm3Str) {
        this.slotId = slotId;
        this.timePlayed = timePlayed;
        this.enemiesKilled = enemiesKilled;
        this.deaths = deaths;
        this.charmAcquired1 = charmAcquired1;
        this.charmAcquired2 = charmAcquired2;
        this.wallBroken = wallBroken;
        this.gameBeaten = gameBeaten;

        try {
            this.lastArea = GameArea.valueOf(areaString);
        } catch (Exception e) {
            this.lastArea = GameArea.NONE;
        }

        this.equippedCharm1 = parseCharmId(equippedCharm1Str);
        this.equippedCharm2 = parseCharmId(equippedCharm2Str);
        this.equippedCharm3 = parseCharmId(equippedCharm3Str);
    }

    private static CharmId parseCharmId(String value) {
        try {
            return CharmId.valueOf(value);
        } catch (Exception e) {
            return CharmId.NONE;
        }
    }
}
