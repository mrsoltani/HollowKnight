package com.Graphic.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontManager {
    private static BitmapFont titleFont;
    private static BitmapFont menuFont;
    private static BitmapFont bodyFont;

    public static void load() {
        FreeTypeFontGenerator cinzel =
            new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/Cinzel-VariableFont_wght.ttf"));

        FreeTypeFontGenerator perpetua =
            new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/Perpetua Regular.otf"));

        FreeTypeFontGenerator.FreeTypeFontParameter params =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Title font
        params.size = 72;
        params.color = Color.WHITE;
        params.shadowOffsetX = 3;
        params.shadowOffsetY = 3;
        params.shadowColor = new Color(0, 0, 0, 0.5f);
        titleFont = cinzel.generateFont(params);

        // Menu button font
        params.size = 36;
        params.shadowOffsetX = 2;
        params.shadowOffsetY = 2;
        menuFont = cinzel.generateFont(params);

        // Body text font
        params.size = 24;
        params.shadowOffsetX = 0;
        params.shadowOffsetY = 0;
        bodyFont = perpetua.generateFont(params);

        cinzel.dispose();
        perpetua.dispose();
    }

    public static BitmapFont getTitle() { return titleFont; }
    public static BitmapFont getMenu()  { return menuFont;  }
    public static BitmapFont getBody()  { return bodyFont;  }

    public static void dispose() {
        titleFont.dispose();
        menuFont.dispose();
        bodyFont.dispose();
    }
}
