package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.managers.CharmManager;
import com.Graphic.managers.SaveManager;
import com.Graphic.models.Charm;
import com.Graphic.models.enums.GameViewScreen;
import com.badlogic.gdx.Gdx;

public class PauseMenuController {
    public static void resume() {
        Gdx.app.log("NAV", "PauseMenuController.resume() called");
        Main.getInstance().setScreen(GameViewScreen.GameScreen);
    }

    public static void switchToSettings() {
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
    public static void switchToCheatCodes() {
        Main.getInstance().setScreen(GameViewScreen.CheatsScreen);
    }

    public static void saveAndQuit() {
        CharmManager.writeEquippedCharms(SaveManager.currentSave);
        SaveManager.saveCurrentGame();
        Main.getInstance().setInGame(false);
        Main.getInstance().setScreen(GameViewScreen.MainMenu);
    }
}
