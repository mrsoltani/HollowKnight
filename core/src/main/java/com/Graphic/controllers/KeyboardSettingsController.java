package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class KeyboardSettingsController {
    public static void changeBrightness(float value){
    }

    public static void changeTheme(float value){
    }


    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
}
