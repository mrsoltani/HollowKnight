package com.Graphic.views.screens;

import com.Graphic.controllers.MainMenuController;
import com.Graphic.controllers.SettingsMenuController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.graphics.Color;

import static com.Graphic.utils.Constants.SettingsMenu.*;

public class SettingsScreen extends BaseMenuScreen {

    private String[] items() {
        return new String[]{
            LocalizationManager.get("settings.audio"),
            LocalizationManager.get("settings.video"),
            LocalizationManager.get("settings.keyboard"),
            LocalizationManager.get("settings.language"),
            LocalizationManager.get("menu.back")
        };
    }

    @Override protected String getTitle()     { return LocalizationManager.get("settings.title"); }
    @Override protected int    getItemCount() { return 5; }

    @Override
    protected float getItemY(int index) {
        return index == 4 ? BACK_Y : FIRST_ITEM_Y - (index * ITEM_SPACING);
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
            case 0: SettingsMenuController.switchToAudioSettings();    break;
            case 1: SettingsMenuController.switchToVideoSettings();    break;
            case 2: SettingsMenuController.switchToKeyboardSettings(); break;
            case 3: SettingsMenuController.switchToLanguageSettings(); break;
            case 4: goBack();                                          break;
        }
    }

    @Override
    protected void goBack() {
        SettingsMenuController.back();
    }
}
