package com.Graphic.views.screens;

import com.Graphic.controllers.SettingsMenuController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.graphics.Color;

import static com.Graphic.utils.Constants.SettingsMenu.*;

public class LanguageSettingsScreen extends BaseMenuScreen {

    private String[] items() {
        return new String[]{
            LocalizationManager.get("game.english"),
            LocalizationManager.get("game.french"),
            LocalizationManager.get("menu.back")
        };
    }

    @Override protected String getTitle()     { return LocalizationManager.get("game.language"); }
    @Override protected int    getItemCount() { return 3; }

    @Override
    protected float getItemY(int index) {
        return index == 2 ? BACK_Y : FIRST_ITEM_Y - (index * ITEM_SPACING);
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
            case 0:
                LocalizationManager.setLanguage(LocalizationManager.Language.EN);
                break;
            case 1:
                LocalizationManager.setLanguage(LocalizationManager.Language.FR);
                break;
            case 2:
                goBack();
                break;
        }
    }

    @Override
    protected void goBack() {
        SettingsMenuController.returnToSettings();
    }
}
