package com.Graphic.models.enums;

public enum GameMap {
    CROSSROADS_01("maps/ForgottenCrossroads01.tmx", "CROSSROADS"),
    CROSSROADS_02("maps/ForgottenCrossroads02.tmx", "CROSSROADS"),
    CROSSROADS_03("maps/ForgottenCrossroads03.tmx", "CROSSROADS"),
    CROSSROADS_04("maps/ForgottenCrossroads04.tmx", "CROSSROADS");

    private final String filePath;
    private final String areaTag;

    GameMap(String filePath, String areaTag) {
        this.filePath = filePath;
        this.areaTag = areaTag;
    }

    public String getFilePath() { return filePath; }
    public String getAreaTag()  { return areaTag; }

    /** Resolves a case-insensitive string from Tiled properties into a valid Enum value */
    public static GameMap fromString(String name) {
        for (GameMap map : values()) {
            if (map.name().equalsIgnoreCase(name) || map.filePath.contains(name)) {
                return map;
            }
        }
        return null;
    }
}
