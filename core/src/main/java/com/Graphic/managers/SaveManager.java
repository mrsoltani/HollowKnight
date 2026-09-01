package com.Graphic.managers;

import com.Graphic.models.GameSaveData;
import com.badlogic.gdx.Gdx;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SaveManager {

    private static String dbUrl;


    public static GameSaveData currentSave;

    public static void init() {

        File saveDir = Gdx.files.local("saves").file();
        if (!saveDir.exists()) saveDir.mkdirs();
        System.out.println("Absolute Save Path: " + Gdx.files.local("saves/savedata.db").file().getAbsolutePath());
        dbUrl = "jdbc:sqlite:" + Gdx.files.local("saves/savedata.db").file().getAbsolutePath();
        createTableIfNotExists();
    }

    private static void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS saves (\n"
            + " slot_id INTEGER PRIMARY KEY,\n"
            + " time_played REAL,\n"
            + " enemies_killed INTEGER,\n"
            + " deaths INTEGER,\n"
            + " charm_acquired_1 INTEGER,\n"
            + " charm_acquired_2 INTEGER,\n"
            + " wall_broken INTEGER,\n"
            + " game_beaten INTEGER,\n"
            + " last_area TEXT,\n"
            + " equipped_charm_1 TEXT,\n"
            + " equipped_charm_2 TEXT,\n"
            + " equipped_charm_3 TEXT\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to create database table.", e);
        }

        migrateAddColumnIfMissing("equipped_charm_1","INTEGER");
        migrateAddColumnIfMissing("equipped_charm_2","INTEGER");
        migrateAddColumnIfMissing("equipped_charm_3","INTEGER");
        migrateAddColumnIfMissing("charm_acquired_1","INTEGER");
        migrateAddColumnIfMissing("charm_acquired_2","INTEGER");
    }


    private static void migrateAddColumnIfMissing(String columnName, String type) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE saves ADD COLUMN " + columnName + " " + type + ";");
        } catch (Exception e) {
            // column already exists — expected on every load after the first
        }
    }


    public static GameSaveData loadSlot(int slotId) {
        String sql = "SELECT * FROM saves WHERE slot_id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, slotId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new GameSaveData(
                    rs.getInt("slot_id"),
                    rs.getFloat("time_played"),
                    rs.getInt("enemies_killed"),
                    rs.getInt("deaths"),
                    rs.getInt("charm_acquired_1") == 1,
                    rs.getInt("charm_acquired_2") == 1,
                    rs.getInt("wall_broken") == 1,
                    rs.getInt("game_beaten") == 1,
                    rs.getString("last_area"),
                    rs.getString("equipped_charm_1"),
                    rs.getString("equipped_charm_2"),
                    rs.getString("equipped_charm_3")
                );
            }
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Error loading save slot " + slotId, e);
        }

        return new GameSaveData(slotId);
    }

    public static void saveCurrentGame() {
        if (currentSave == null) return;

        String sql = "INSERT INTO saves (slot_id, time_played, enemies_killed, deaths, charm_acquired_1, charm_acquired_2, wall_broken, game_beaten, last_area, " +
            "equipped_charm_1, equipped_charm_2, equipped_charm_3) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(slot_id) DO UPDATE SET " +
            "time_played=excluded.time_played, " +
            "enemies_killed=excluded.enemies_killed, " +
            "deaths=excluded.deaths, " +
            "charm_acquired_1=excluded.charm_acquired_1, " +
            "charm_acquired_2=excluded.charm_acquired_2, " +
            "wall_broken=excluded.wall_broken, " +
            "game_beaten=excluded.game_beaten, " +
            "last_area=excluded.last_area, " +
            "equipped_charm_1=excluded.equipped_charm_1, " +
            "equipped_charm_2=excluded.equipped_charm_2, " +
            "equipped_charm_3=excluded.equipped_charm_3;";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentSave.slotId);
            pstmt.setFloat(2, currentSave.timePlayed);
            pstmt.setInt(3, currentSave.enemiesKilled);
            pstmt.setInt(4, currentSave.deaths);
            pstmt.setInt(5, currentSave.charmAcquired1 ? 1 : 0);
            pstmt.setInt(6, currentSave.charmAcquired2 ? 1 : 0);
            pstmt.setInt(7, currentSave.wallBroken ? 1 : 0);
            pstmt.setInt(8, currentSave.gameBeaten ? 1 : 0);
            pstmt.setString(9, currentSave.lastArea.name());
            pstmt.setString(10, currentSave.equippedCharm1.name());
            pstmt.setString(11, currentSave.equippedCharm2.name());
            pstmt.setString(12, currentSave.equippedCharm3.name());

            pstmt.executeUpdate();
            Gdx.app.log("SaveManager", "Game saved successfully to slot " + currentSave.slotId);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Error saving game.", e);
        }
    }


    public static void deleteAndResetDatabase() {
        try {
            com.badlogic.gdx.files.FileHandle dbFile = Gdx.files.local("saves/savedata.db");
            if (dbFile.exists()) {
                dbFile.delete();
                Gdx.app.log("SaveManager", "Database deleted successfully.");
            }
            createTableIfNotExists();
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to clear the save database.", e);
        }
    }
}
