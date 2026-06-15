package com.Graphic.models.enums;
import com.Graphic.views.screens.*;
import com.badlogic.gdx.Screen;

public enum GameViewScreen {
    MainMenu(new MainMenuScreen()),
    SettingsMenu(new SettingsScreen()),
    GuideMenu(new GuideScreen()),
    AchievementsMenu(new AchievementsScreen()),
    StartGameMenu(new GameStartScreen()),
    GameScreen(new GameScreen());
    private final Screen screen;
    GameViewScreen(Screen screen) {
        this.screen=screen;
    }
    public Screen getScreen(){
        return this.screen;
    }
}
