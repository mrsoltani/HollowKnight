package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class SettingsMenuController {
    public static void switchToAudioSettings(){
        Main.getInstance().setScreen(GameViewScreen.AudioSettings);
    }

    public static void switchToVideoSettings(){
        Main.getInstance().setScreen(GameViewScreen.VideoSettings);
    }

    public static void switchToKeyboardSettings(){
        Main.getInstance().setScreen(GameViewScreen.KeyboardSettings);
    }

    public static void switchToLanguageSettings(){
        Main.getInstance().setScreen(GameViewScreen.LanguageSettings);
    }

    public static void back(){
        if (Main.getInstance().isInGame()) {
            Main.getInstance().setScreen(GameViewScreen.PauseMenu);
        } else {
            Main.getInstance().setScreen(GameViewScreen.MainMenu);
        }
    }
    public static void returnToSettings(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
}
