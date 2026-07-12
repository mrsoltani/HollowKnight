package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.managers.SaveManager;
import com.Graphic.models.enums.GameMap;
import com.Graphic.models.enums.GameViewScreen;
import com.Graphic.models.GameScreen;
import com.badlogic.gdx.Gdx;

public class MainMenuController {
    public static void switchToStartGameMenu(){
        Main.getInstance().setScreen(GameViewScreen.StartGameMenu);
    }

    public static void switchToSettingsMenu(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }

    public static void switchToAchievementsMenu(){
        Main.getInstance().setScreen(GameViewScreen.AchievementsMenu);
    }

    public static void switchToGuideMenu(){
        Main.getInstance().setScreen(GameViewScreen.GuideMenu);
    }

    public static void returnToMainMenu(){
        Main.getInstance().setScreen(GameViewScreen.MainMenu);
    }
    public static void quitGame(){
        Gdx.app.exit();
    }
}

