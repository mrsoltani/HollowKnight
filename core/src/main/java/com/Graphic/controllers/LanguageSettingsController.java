package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class LanguageSettingsController {
    public static void changeLanguage(){
    }

    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
}
