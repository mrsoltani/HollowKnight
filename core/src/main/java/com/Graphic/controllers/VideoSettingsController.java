package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.managers.VideoManager;
import com.Graphic.models.enums.GameViewScreen;

public class VideoSettingsController {
    public static void changeBrightness(float sliderValue){
        float linerValue = sliderValue * 2f - 1f;
        float brightness = linerValue * linerValue * linerValue;
        VideoManager.setBrightness(brightness);
    }

    public static void setTheme(int direction){

    }

    public static void reset(){
        VideoManager.setBrightness(0);
    }


    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.SettingsMenu);
    }
}
