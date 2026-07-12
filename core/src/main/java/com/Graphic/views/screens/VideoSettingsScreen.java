package com.Graphic.views.screens;

import com.Graphic.Main;
import com.Graphic.controllers.SettingsMenuController;
import com.Graphic.controllers.VideoSettingsController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.managers.VideoManager;
import com.Graphic.views.atmosphere.MenuAtmosphere;
import com.Graphic.views.atmosphere.Theme;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;

import static com.Graphic.utils.Constants.Menu.*;
import static com.Graphic.utils.Constants.SettingsMenu.*;

public class VideoSettingsScreen extends BaseMenuScreen {

    private Stage        stage;
    private Skin         skin;
    private TextureAtlas sliderAtlas;
    private Slider       brightnessSlider;

    private static final Theme[] THEMES = {
        Theme.VOID,
        Theme.GREENPATH,
        Theme.CRYSTAL_PEAK
    };

    private String[] items() {
        return new String[]{
            LocalizationManager.get("video.brightness"),
            LocalizationManager.get("video.theme"),
            LocalizationManager.get("video.reset"),
            LocalizationManager.get("menu.back")
        };
    }

    private String themeLabel(Theme t) {
        switch (t) {
            case GREENPATH:    return LocalizationManager.get("video.theme.green");
            case CRYSTAL_PEAK: return LocalizationManager.get("video.theme.crystal");
            default:           return LocalizationManager.get("video.theme.void");
        }
    }

    private void cycleTheme(int direction) {
        Theme current = MenuAtmosphere.getInstance().getCurrentTheme();
        int idx = 0;
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i] == current) { idx = i; break; }
        }
        idx = (idx + direction + THEMES.length) % THEMES.length;
        MenuAtmosphere.getInstance().applyTheme(THEMES[idx], false);
    }

    @Override protected String getTitle()     { return LocalizationManager.get("video.title"); }
    @Override protected int    getItemCount() { return 4; }

    @Override
    protected float getItemY(int index) {
        if (index == 3) return BACK_Y;
        if (index == 2) return BACK_Y + 110f;
        return FIRST_ITEM_Y - (index * ITEM_SPACING);
    }

    @Override
    protected float getRowStartX(int index) {
        if (index < 2) return LEFT_COL_X;
        return centeredStartX(items()[index], FontManager.getMenu());
    }

    @Override
    protected float getRowEndX(int index) {
        if (index < 2) return RIGHT_COL_END_X;
        return centeredEndX(items()[index], FontManager.getMenu());
    }

    @Override
    protected void onShow() {
        stage            = new Stage(viewport, batch);
        sliderAtlas      = new TextureAtlas(Gdx.files.internal(PATH_SLIDER_ATLAS));
        skin             = new Skin(Gdx.files.internal(PATH_SLIDER_JSON), sliderAtlas);
        brightnessSlider = new Slider(0f, 1f, 0.01f, false, skin, SLIDER_STYLE);
        brightnessSlider.setValue((VideoManager.getBrightness()+1)/2);
        stage.addActor(brightnessSlider);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    protected void renderItems() {
        String[] items = items();
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);


        FontManager.getMenu().draw(batch, items[0], LEFT_COL_X, getItemY(0));
        brightnessSlider.setSize(SLIDER_WIDTH, brightnessSlider.getPrefHeight());
        layout.setText(FontManager.getMenu(), "A");
        brightnessSlider.setPosition(RIGHT_VALUE_X,
            (getItemY(0) - layout.height / 2f) - brightnessSlider.getHeight() / 2f);

        FontManager.getMenu().draw(batch, items[1], LEFT_COL_X, getItemY(1));
        String currentThemeLabel = themeLabel(MenuAtmosphere.getInstance().getCurrentTheme());
        layout.setText(FontManager.getMenu(), currentThemeLabel);
        FontManager.getMenu().draw(batch, currentThemeLabel,
            RIGHT_COL_END_X - layout.width, getItemY(1));


        drawCentered(items[2], getItemY(2), FontManager.getMenu());


        drawCentered(items[3], getItemY(3), FontManager.getMenu());
    }

    @Override protected void renderStage() { stage.act(); stage.draw(); }

    @Override
    protected void handleExtraInput(float delta) {

        if (selectedIndex == 1) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) cycleTheme(-1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) cycleTheme(+1);
        }


        if (selectedIndex == 0) {
            float mod = 2 * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
                brightnessSlider.setValue(brightnessSlider.getValue() - mod);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                brightnessSlider.setValue(brightnessSlider.getValue() + mod);
            VideoSettingsController.changeBrightness(brightnessSlider.getValue());
        }
    }
    @Override
    protected void handleItemClick(int index) {
        selectCurrent();
    }

    @Override
    protected void selectCurrent() {
        switch (selectedIndex) {
            case 1:
                cycleTheme(+1);
                break;
            case 2:
                brightnessSlider.setValue(0.5f);
                VideoSettingsController.reset();
                break;
            case 3:
                goBack();
                break;
        }
    }

    @Override protected void goBack() {
        SettingsMenuController.returnToSettings();
    }

    @Override
    protected void onDispose() {
        if (stage       != null) stage.dispose();
        if (skin        != null) skin.dispose();
        if (sliderAtlas != null) sliderAtlas.dispose();
    }
}
