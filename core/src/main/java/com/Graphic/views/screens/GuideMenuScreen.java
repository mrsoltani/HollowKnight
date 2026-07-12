package com.Graphic.views.screens;

import com.Graphic.controllers.GuideMenuController;
import com.Graphic.controllers.MainMenuController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.graphics.Color;

import static com.Graphic.utils.Constants.SettingsMenu.*;
import static com.Graphic.utils.Constants.V_HEIGHT;

public class GuideMenuScreen extends BaseMenuScreen {

    // ── Layout Tuning Configuration ──────────────────────────────────────
    private static final float CENTER_MENU_SPACING = 120f; // Generous space between the two items

    private String[] items() {
        return new String[]{
            LocalizationManager.get("guide.menu.abilities"),
            LocalizationManager.get("guide.menu.cheats"),
            LocalizationManager.get("guide.menu.back")
        };
    }

    @Override
    protected String getTitle() {
        return LocalizationManager.get("guide.title");
    }

    @Override
    protected int getItemCount() {
        return 3;
    }

    @Override
    protected float getItemY(int index) {
        if (index == 2) {
            return BACK_Y; // Keep BACK at its fixed designated screen position
        }

        // Start from perfect center height, offset index 0 up slightly and index 1 down
        float menuCenterY = V_HEIGHT / 2f + 30f;
        return menuCenterY - (index * CENTER_MENU_SPACING);
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
            // Highlight selection text using white vs unselected muted slate gray tint
            FontManager.getMenu().setColor(
                i == selectedIndex
                    ? new Color(1f, 1f, 1f, 1f)
                    : new Color(0.35f, 0.35f, 0.4f, 0.75f)
            );
            drawCentered(items[i], getItemY(i), FontManager.getMenu());
        }

        // Reset color layout safety defaults
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void selectCurrent() {
        switch (selectedIndex) {
            case 0: GuideMenuController.switchToAbilities();  break;
            case 1: GuideMenuController.switchToCheatCodes(); break;
            case 2: goBack();                                 break;
        }
    }

    @Override
    protected void goBack() {
        MainMenuController.returnToMainMenu();
    }
}
