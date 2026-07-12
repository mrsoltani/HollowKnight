package com.Graphic.views.screens;

import com.Graphic.controllers.AudioSettingsController;
import com.Graphic.controllers.SettingsMenuController;
import com.Graphic.managers.AudioManager;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import static com.Graphic.utils.Constants.Menu.*;
import static com.Graphic.utils.Constants.SettingsMenu.*;

public class AudioSettingsScreen extends BaseMenuScreen {

    private Stage        stage;
    private Skin         skin;
    private TextureAtlas sliderAtlas;
    private Slider       masterSlider;

    private String[] items() {
        return new String[]{
            LocalizationManager.get("audio.music.volume"),
            LocalizationManager.get("audio.music.toggle"),
            LocalizationManager.get("audio.sfx.toggle"),
            LocalizationManager.get("audio.reset"),
            LocalizationManager.get("menu.back")
        };
    }

    @Override protected String getTitle()     { return LocalizationManager.get("audio.title"); }
    @Override protected int    getItemCount() { return 5; }

    @Override
    protected float getItemY(int index) {
        if (index == 4) return BACK_Y;
        if (index == 3) return BACK_Y + 110f;
        return FIRST_ITEM_Y - (index * ITEM_SPACING);
    }

    @Override
    protected float getRowStartX(int index) {
        if (index < 3) return LEFT_COL_X;
        return centeredStartX(items()[index], FontManager.getMenu());
    }

    @Override
    protected float getRowEndX(int index) {
        if (index < 3) return RIGHT_COL_END_X;
        return centeredEndX(items()[index], FontManager.getMenu());
    }

    @Override
    protected void onShow() {
        stage       = new Stage(viewport, batch);
        sliderAtlas = new TextureAtlas(Gdx.files.internal(PATH_SLIDER_ATLAS));
        skin        = new Skin(Gdx.files.internal(PATH_SLIDER_JSON), sliderAtlas);

        Gdx.input.setInputProcessor(stage);

        masterSlider = new Slider(0f, 1f, 0.01f, false, skin, SLIDER_STYLE);

        // Read initial value from AudioManager — single source of truth
        masterSlider.setValue(AudioManager.getMasterVolume());

        masterSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                AudioManager.setMasterVolume(masterSlider.getValue());
            }
        });
        stage.addActor(masterSlider);
    }

    @Override
    protected void renderItems() {
        String[] items = items();
        FontManager.getMenu().setColor(1f, 1f, 1f, 1f);

        for (int i = 0; i < items.length; i++) {
            float y = getItemY(i);

            if (i == 0) {
                FontManager.getMenu().draw(batch, items[i], LEFT_COL_X, y);
                masterSlider.setSize(SLIDER_WIDTH, masterSlider.getPrefHeight());
                layout.setText(FontManager.getMenu(), "A");
                masterSlider.setPosition(RIGHT_VALUE_X,
                    (y - layout.height / 2f) - masterSlider.getHeight() / 2f);

            } else if (i == 1 || i == 2) {
                FontManager.getMenu().draw(batch, items[i], LEFT_COL_X, y);

                // Fetch live state from AudioManager
                boolean state = (i == 1)
                    ? AudioManager.isMusicEnabled()
                    : AudioManager.isSFXEnabled();
                String stateLabel = state
                    ? LocalizationManager.get("common.on")
                    : LocalizationManager.get("common.off");
                layout.setText(FontManager.getMenu(), stateLabel);
                FontManager.getMenu().draw(batch, stateLabel,
                    RIGHT_COL_END_X - layout.width, y);

            } else {
                drawCentered(items[i], y, FontManager.getMenu());
            }
        }
    }

    @Override protected void renderStage() { stage.act(); stage.draw(); }

    @Override
    protected void handleExtraInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
            || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            if      (selectedIndex == 1) AudioManager.setMusicEnabled(!AudioManager.isMusicEnabled());
            else if (selectedIndex == 2) AudioManager.setSFXEnabled(!AudioManager.isSFXEnabled());
        }

        if (selectedIndex == 0) {
            float mod  = 0.35f * delta;
            float prev = masterSlider.getValue();
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
                masterSlider.setValue(prev - mod);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                masterSlider.setValue(prev + mod);
            if (masterSlider.getValue() != prev)
                AudioManager.setMasterVolume(masterSlider.getValue());
        }
    }

    @Override
    protected void handleItemClick(int index) {
        if      (index == 1) AudioManager.setMusicEnabled(!AudioManager.isMusicEnabled());
        else if (index == 2) AudioManager.setSFXEnabled(!AudioManager.isSFXEnabled());
        else                 selectCurrent();
    }

    @Override
    protected void selectCurrent() {
        switch (selectedIndex) {
            case 3: AudioManager.resetDefaults();
                masterSlider.setValue(AudioManager.getMasterVolume()); break;
            case 4: goBack(); break;
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
