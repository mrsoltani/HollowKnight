package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class GuideMenuController {
    public static void switchToAbilities(){
        Main.getInstance().setScreen(GameViewScreen.AbilitiesScreen);
    }

    public static void switchToCheatCodes(){
        Main.getInstance().setScreen(GameViewScreen.CheatsScreen);
    }

    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.MainMenu);
    }


}
