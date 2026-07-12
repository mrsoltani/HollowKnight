package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class AudioSettingsController {
    public static void changeMasterVolume(float value){
    }

    public static void toggleMusic(boolean musicOn){
    }

    public static void toggleSFX(boolean sfxOn){
    }

    public static void resetDefaults(){
    }

    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
}
