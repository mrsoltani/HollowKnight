package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class AbilitiesScreenController {
    public static void back(){
        Main.getInstance().setScreen(GameViewScreen.GuideMenu);
    }
}
