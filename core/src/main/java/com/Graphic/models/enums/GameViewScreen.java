package com.Graphic.models.enums;
import com.Graphic.models.GameScreen;
import com.Graphic.views.screens.*;
import com.badlogic.gdx.Screen;

public enum GameViewScreen {
    MainMenu(new MainMenuScreen()),
    SettingsMenu(new SettingsScreen()),
    GuideMenu(new GuideMenuScreen()),
    AchievementsMenu(new AchievementsScreen()),
    StartGameMenu(new StartGameMenuScreen()),
    GameScreen(new GameScreen()),
    InventoryScreen(new InventoryScreen()),
    PauseMenu(new PauseScreen()),
    AudioSettings(new AudioSettingsScreen()),
    VideoSettings(new VideoSettingsScreen()),
    KeyboardSettings(new KeyboardSettingsScreen()),
    LanguageSettings(new LanguageSettingsScreen()),
    AbilitiesScreen(new GuideAbilitiesScreen()),
    CheatsScreen(new GuideCheatCodesScreen());

    private final Screen screen;
    GameViewScreen(Screen screen) {
        this.screen=screen;
    }
    public Screen getScreen(){
        return this.screen;
    }
}
