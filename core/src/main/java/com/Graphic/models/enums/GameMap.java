package com.Graphic.models.enums;

public enum GameMap {
    CROSSROADS_01("maps/ForgottenCrossroads01.tmx", "CROSSROADS"),
    CROSSROADS_02("maps/ForgottenCrossroads02.tmx", "CROSSROADS"),
    CROSSROADS_03("maps/ForgottenCrossroads03.tmx", "CROSSROADS"),
    CROSSROADS_04("maps/ForgottenCrossroads04.tmx", "CROSSROADS"),
    TRANSITION("maps/Transition.tmx", "TRANSITION"),
    CRYSTAL_PEAK_01("maps/CrystalPeak01.tmx", "CRYSTAL_PEAK"),
    CRYSTAL_PEAK_02("maps/CrystalPeak02.tmx", "CRYSTAL_PEAK"),
    CRYSTAL_PEAK_03("maps/CrystalPeak03.tmx", "CRYSTAL_PEAK"),
    CRYSTAL_PEAK_04("maps/CrystalPeak04.tmx", "CRYSTAL_PEAK"),
    CRYSTAL_PEAK_05("maps/CrystalPeak05.tmx", "CRYSTAL_PEAK"),
    CRYSTAL_PEAK("maps/crystal peak/Map.tmx", "CRYSTAL_PEAK");

    private final String filePath;
    private final String areaTag;

    GameMap(String filePath, String areaTag) {
        this.filePath = filePath;
        this.areaTag = areaTag;
    }

    public String getFilePath() { return filePath; }
    public String getAreaTag()  { return areaTag; }


    public static GameMap fromString(String name) {
        for (GameMap map : values()) {
            if (map.name().equalsIgnoreCase(name) || map.filePath.contains(name)) {
                return map;
            }
        }
        return null;
    }
}
