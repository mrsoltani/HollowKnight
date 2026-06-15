package com.Graphic;

import com.Graphic.managers.FontManager;
import com.Graphic.models.App;
import com.Graphic.views.screens.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Main extends Game {
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        FontManager.load();
        setScreen(App.getCurrentScreen());
    }

    @Override
    public void dispose() {
        batch.dispose();
        FontManager.dispose();
    }
}
