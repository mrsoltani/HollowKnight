package com.Graphic.views.screens;

import com.Graphic.controllers.MainMenuController;
import com.Graphic.managers.AudioManager;
import com.Graphic.managers.EventBus;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;

import static com.Graphic.utils.Constants.MainMenu.*;
import static com.Graphic.utils.Constants.V_HEIGHT;
import static com.Graphic.utils.Constants.V_WIDTH;
import static com.Graphic.utils.Constants.Menu.*;

public class MainMenuScreen extends BaseMenuScreen {

    private Texture logo;

    private String[] items() {
        return new String[]{
            LocalizationManager.get("menu.start"),
            LocalizationManager.get("menu.options"),
            LocalizationManager.get("menu.achievements"),
            LocalizationManager.get("menu.guide"),
            LocalizationManager.get("menu.quit")
        };
    }

    @Override protected String getTitle()     { return ""; }
    @Override protected int    getItemCount() { return 5; }

    @Override
    protected float getItemY(int index) {
        return FIRST_ITEM_Y - (index * ITEM_SPACING);
    }

    @Override
    protected float getRowStartX(int index) {
        return centeredStartX(items()[index], FontManager.getMenu());
    }

    @Override
    protected float getRowEndX(int index) {
        return centeredEndX(items()[index], FontManager.getMenu());
    }

    @Override
    protected void onShow() {
        logo = new Texture(Gdx.files.internal(PATH_LOGO));
        EventBus.emit(EventBus.Event.ENTER_MENU);
    }


    @Override
    protected void renderHeader() {
        float logoW = logo.getWidth();
        float logoH = logo.getHeight();
        batch.draw(logo, (V_WIDTH - logoW) / 2f, V_HEIGHT - logoH);
    }

    @Override
    protected void renderItems() {
        String[] items = items();
        for (int i = 0; i < items.length; i++) {
            FontManager.getMenu().setColor(
                i == selectedIndex
                    ? new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f)
                    : new com.badlogic.gdx.graphics.Color(0.35f, 0.35f, 0.4f, 0.75f)
            );
            drawCentered(items[i], getItemY(i), FontManager.getMenu());
        }
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void handleExtraInput(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            com.Graphic.views.atmosphere.MenuAtmosphere.getInstance()
                .applyTheme(com.Graphic.views.atmosphere.Theme.VOID, false);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            com.Graphic.views.atmosphere.MenuAtmosphere.getInstance()
                .applyTheme(com.Graphic.views.atmosphere.Theme.GREENPATH, false);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            com.Graphic.views.atmosphere.MenuAtmosphere.getInstance()
                .applyTheme(com.Graphic.views.atmosphere.Theme.CRYSTAL_PEAK, false);
    }

    @Override
    protected void selectCurrent() {
        switch (selectedIndex) {
            case 0: MainMenuController.switchToStartGameMenu();    break;
            case 1: MainMenuController.switchToSettingsMenu();     break;
            case 2: MainMenuController.switchToAchievementsMenu(); break;
            case 3: MainMenuController.switchToGuideMenu();        break;
            case 4: MainMenuController.quitGame();                 break;
        }
    }

    @Override protected void goBack() {  }

    @Override
    protected void onDispose() {
        if (logo != null) logo.dispose();
    }
}
