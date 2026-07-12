package com.Graphic.views.screens;

import com.Graphic.controllers.PauseMenuController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

import static com.Graphic.utils.Constants.V_HEIGHT;

public class PauseScreen extends BaseMenuScreen {

    private static final float ITEM_SPACING = 90f;
    private static final float START_Y      = V_HEIGHT * 0.58f;
    private boolean back=false;
    private String[] items() {
        return new String[]{
            LocalizationManager.get("pause.continue"),
            LocalizationManager.get("pause.settings"),
            LocalizationManager.get("pause.cheats"),
            LocalizationManager.get("pause.save"),
        };
    }

    @Override protected String getTitle()     { return LocalizationManager.get("pause.title"); }
    @Override protected int    getItemCount() { return items().length; }

    @Override
    protected float getItemY(int index) {
        return START_Y - (index * ITEM_SPACING);
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
    protected void renderItems() {
        String[] items = items();
        for (int i = 0; i < items.length; i++) {
            FontManager.getMenu().setColor(
                i == selectedIndex
                    ? new Color(1f, 1f, 1f, 1f)
                    : new Color(0.35f, 0.35f, 0.4f, 0.75f)
            );
            drawCentered(items[i], getItemY(i), FontManager.getMenu());
        }
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void selectCurrent() {
        switch (selectedIndex) {
            case 0: PauseMenuController.resume();          break;
            case 1: PauseMenuController.switchToSettings(); break;
            case 2: PauseMenuController.switchToCheatCodes();break;
            case 3: PauseMenuController.saveAndQuit();      break;
        }
    }

    @Override
    protected void goBack() {
        PauseMenuController.resume();
    }

    @Override
    protected void handleExtraInput(float delta){

    }
}
