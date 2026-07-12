package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.managers.CharmManager;
import com.Graphic.managers.SaveManager;
import com.Graphic.models.enums.GameViewScreen;
import com.Graphic.models.GameScreen;

public class StartGameController {
    public static void startGame(){
        CharmManager.loadCharms(SaveManager.currentSave);
        GameScreen s=(GameScreen) GameViewScreen.GameScreen.getScreen();
        Main.getInstance().setScreen(GameViewScreen.GameScreen);
        s.spawn();
    }
}
