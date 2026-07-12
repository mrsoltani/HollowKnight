package com.Graphic.views.screens;

import com.Graphic.controllers.CheatCodesScreenController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.controllers.SettingsMenuController;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import static com.Graphic.utils.Constants.SettingsMenu.*;
import static com.Graphic.utils.Constants.V_WIDTH;

public class GuideCheatCodesScreen extends BaseMenuScreen {
    private static final String[][] COMBOS = {
        {"Shift", "F1"}, {"Shift", "F2"}, {"Shift", "F3"},
        {"Shift", "F4"}, {"Shift", "F5"}, {"Shift", "F6"}
    };

    private static final float KEYCAP_H      = 42f;
    private static final float KEYCAP_RADIUS = 7f;
    private static final float KEY1_W        = 100f;
    private static final float KEY2_W        = 70f;
    private static final float KEY_GAP       = 14f;
    private static final float PLUS_W        = 20f;
    private static final float COMBO_BOX_W   = 160f;

    private static final float COL1_TEXT_X   = 440f;
    private static final float COL1_DESC_X   = 440f;
    private static final float COL1_KEY_X    = 1200f;
    private static final float ROW_SPACING   = 110f;
    private static final float GRID_START_Y  = 760f;


    private static final String[] NAME_KEYS = {
        "cheat.soul",
        "cheat.heal",
        "cheat.teleport",
        "cheat.noclip",
        "cheat.god",
        "cheat.onehit"
    };

    private static final String[] DESC_KEYS = {
        "cheat.soul.desc",
        "cheat.heal.desc",
        "cheat.teleport.desc",
        "cheat.noclip.desc",
        "cheat.god.desc",
        "cheat.onehit.desc"
    };


    private ShapeRenderer shapeRenderer;



    @Override protected String getTitle()     { return LocalizationManager.get("cheat.title"); }
    @Override protected int    getItemCount() { return 1; }

    @Override
    protected float getItemY(int index) {
        return BACK_Y;
    }

    @Override
    protected float getRowStartX(int index) {
        return centeredStartX(LocalizationManager.get("menu.back"), FontManager.getMenu());
    }

    @Override
    protected float getRowEndX(int index) {
        return centeredEndX(LocalizationManager.get("menu.back"), FontManager.getMenu());
    }



    @Override
    protected void onShow() {
        shapeRenderer = new ShapeRenderer();
    }



    @Override
    public void render(float delta) {
        if (delta > 0.1f) delta = 0.1f;

        handleInput(delta);
        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();


        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderBackground();
        renderHeader();
        batch.end();


        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < COMBOS.length; i++) renderComboBox(i);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);


        batch.begin();
        renderItems();
        drawPointers(
            getRowStartX(selectedIndex),
            getRowEndX(selectedIndex),
            currentPointerY
        );
        batch.end();
    }
    @Override
    protected void renderItems() {
        for (int i = 0; i < COMBOS.length; i++) {
            float rowY = rowY(i);


            FontManager.getMenuSmall().setColor(Color.WHITE);
            FontManager.getMenuSmall().draw(batch,
                LocalizationManager.get(NAME_KEYS[i]),
                COL1_TEXT_X, rowY);





            float boxCenterY = rowY - KEYCAP_H / 2f;

            String key1 = COMBOS[i][0];
            String key2 = COMBOS[i][1];


            layout.setText(FontManager.getMenuSmall(), key1);
            float ty1 = boxCenterY + layout.height / 2f;
            float tx1 = COL1_KEY_X + (KEY1_W - layout.width) / 2f;
            FontManager.getMenuSmall().setColor(Color.WHITE);
            FontManager.getMenuSmall().draw(batch, key1, tx1, ty1);


            layout.setText(FontManager.getMenuSmall(), "+");
            float tyPlus = boxCenterY + layout.height / 2f;
            float txPlus = COL1_KEY_X + KEY1_W + (KEY_GAP + PLUS_W) / 2f - layout.width / 2f;
            FontManager.getMenuSmall().setColor(Color.WHITE);
            FontManager.getMenuSmall().draw(batch, "+", txPlus, tyPlus);


            float key2X = COL1_KEY_X + KEY1_W + KEY_GAP + PLUS_W;
            layout.setText(FontManager.getMenuSmall(), key2);
            float ty2 = boxCenterY + layout.height / 2f;
            float tx2 = key2X + (KEY2_W - layout.width) / 2f;
            FontManager.getMenuSmall().setColor(Color.WHITE);
            FontManager.getMenuSmall().draw(batch, key2, tx2, ty2);
        }

        FontManager.getMenuSmall().setColor(Color.WHITE);


        FontManager.getMenu().setColor(Color.WHITE);
        drawCentered(LocalizationManager.get("menu.back"), getItemY(0), FontManager.getMenu());
    }


    private void renderComboBox(int index) {
        float ky = rowY(index) - KEYCAP_H;
        float kh = KEYCAP_H;
        float r  = KEYCAP_RADIUS;

        shapeRenderer.setColor(Color.WHITE);


        drawRoundedBox(COL1_KEY_X, ky, KEY1_W, kh, r);


        float key2X = COL1_KEY_X + KEY1_W + KEY_GAP + PLUS_W;
        drawRoundedBox(key2X, ky, KEY2_W, kh, r);
    }

    private void drawRoundedBox(float kx, float ky, float kw, float kh, float r) {
        shapeRenderer.line(kx + r, ky,      kx + kw - r, ky);
        shapeRenderer.line(kx + r, ky + kh, kx + kw - r, ky + kh);
        shapeRenderer.line(kx,     ky + r,  kx,           ky + kh - r);
        shapeRenderer.line(kx + kw, ky + r, kx + kw,      ky + kh - r);

        drawArcOutline(kx + r,      ky + r,      r, 180f, 270f, 12);
        drawArcOutline(kx + kw - r, ky + r,      r, 270f, 360f, 12);
        drawArcOutline(kx + r,      ky + kh - r, r,  90f, 180f, 12);
        drawArcOutline(kx + kw - r, ky + kh - r, r,   0f,  90f, 12);
    }

    private void drawArcOutline(float cx, float cy, float r,
                                float s, float e, int n) {
        float step = (e - s) / n;
        for (int i = 0; i < n; i++) {
            double a1 = Math.toRadians(s + i * step);
            double a2 = Math.toRadians(s + (i + 1) * step);
            shapeRenderer.line(
                cx + r * (float) Math.cos(a1), cy + r * (float) Math.sin(a1),
                cx + r * (float) Math.cos(a2), cy + r * (float) Math.sin(a2)
            );
        }
    }

    private float rowY(int index) {
        return GRID_START_Y - (index * ROW_SPACING);
    }



    @Override
    protected void handleExtraInput(float delta) {


        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goBack();
        }
    }

    @Override
    protected void selectCurrent() {
        goBack();
    }

    @Override
    protected void goBack() {
        CheatCodesScreenController.back();
    }
}
