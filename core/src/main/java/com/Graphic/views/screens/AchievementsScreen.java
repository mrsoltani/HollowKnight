package com.Graphic.views.screens;

import com.Graphic.Main;
import com.Graphic.controllers.MainMenuController;
import com.Graphic.managers.AchievementManager;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.models.Achievement;
import com.Graphic.models.enums.GameViewScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

import static com.Graphic.utils.Constants.SettingsMenu.BACK_Y;

public class AchievementsScreen extends BaseMenuScreen {

    private static final Color TITLE_COLOR  = new Color(1f, 0.85f, 0.35f, 1f);
    private static final float ICON_SIZE    = 96f;
    private static final float ROW_HEIGHT   = 120f;


    private static final float ROW_START_Y  = 820f;

    private static final float ICON_X       = 260f;
    private static final float TEXT_X       = 400f;
    private static final float LOCKED_ALPHA = 0.4f;

    private ShaderProgram grayscaleShader;
    private boolean grayscaleShaderOk;

    @Override
    protected void onShow() {
        grayscaleShader = new ShaderProgram(
            Gdx.files.internal("shaders/blur.vert"),
            Gdx.files.internal("shaders/grayscale.frag")
        );
        grayscaleShaderOk = grayscaleShader.isCompiled();
        if (!grayscaleShaderOk) {
            Gdx.app.error("AchievementsScreen", "Grayscale shader failed: " + grayscaleShader.getLog());
        }
    }


    @Override protected String getTitle()     { return LocalizationManager.get("achievements.title"); }
    @Override protected int    getItemCount() { return 1; }

    @Override
    protected float getItemY(int index) { return BACK_Y; }

    @Override
    protected float getRowStartX(int index) {
        return centeredStartX(LocalizationManager.get("menu.back"), FontManager.getMenu());
    }

    @Override
    protected float getRowEndX(int index) {
        return centeredEndX(LocalizationManager.get("menu.back"), FontManager.getMenu());
    }

    @Override
    protected void renderItems() {
        Array<Achievement> achievements = AchievementManager.getAll();
        float y = ROW_START_Y;

        for (Achievement achievement : achievements) {
            drawAchievementRow(achievement, y);
            y -= ROW_HEIGHT;
        }

        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
        drawCentered(LocalizationManager.get("menu.back"), BACK_Y, FontManager.getMenu());
    }

    private void drawAchievementRow(Achievement achievement, float y) {
        boolean unlocked = achievement.isUnlocked();


        float iconAlpha = unlocked ? 1f : LOCKED_ALPHA;

        if (!unlocked && grayscaleShaderOk) batch.setShader(grayscaleShader);
        batch.setColor(1f, 1f, 1f, iconAlpha);
        batch.draw(achievement.getIcon(), ICON_X, y - ICON_SIZE, ICON_SIZE, ICON_SIZE);
        batch.setShader(null);


        FontManager.getMenu().setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, 1f);
        FontManager.getMenu().draw(batch, LocalizationManager.get(achievement.getTitleKey()), TEXT_X, y - 20f);

        FontManager.getMenuSmall().setColor(1f, 1f, 1f, 1f);
        FontManager.getMenuSmall().draw(batch, LocalizationManager.get(achievement.getDescriptionKey()), TEXT_X, y - 60f);


        batch.setColor(1f, 1f, 1f, 1f);
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
        FontManager.getMenuSmall().setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void selectCurrent() { goBack(); }

    @Override
    protected void goBack() {
        MainMenuController.returnToMainMenu();
    }

    @Override
    protected void onDispose() {
        if (grayscaleShader != null) grayscaleShader.dispose();
    }
}
