package com.Graphic.controllers;

import com.Graphic.Main;
import com.Graphic.models.enums.GameViewScreen;

public class CheatCodesScreenController {
    public static void back(){
        if(Main.getInstance().isInGame()){
            Main.getInstance().setScreen(GameViewScreen.PauseMenu);
        }
        else{
            Main.getInstance().setScreen(GameViewScreen.GuideMenu);
        }
    }
}
