package com.Graphic.views.screens;

import com.Graphic.managers.FontManager;
import com.Graphic.managers.InputManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.utils.GameAction;
import com.Graphic.controllers.SettingsMenuController;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import static com.Graphic.utils.Constants.SettingsMenu.*;
import static com.Graphic.utils.Constants.V_WIDTH;

public class KeyboardSettingsScreen extends BaseMenuScreen {

    // ── Grid geometry ────────────────────────────────────────────────────
    private static final float KEYCAP_SIZE   = 56f;
    private static final float KEYCAP_RADIUS = 8f;
    private static final float ARROW_HALF    = 11f;
    private static final float TEXT_W        = 240f;
    private static final float COL1_TEXT_X   = 599f;
    private static final float COL1_KEY_X    = 869f;
    private static final float COL2_TEXT_X   = 995f;
    private static final float COL2_KEY_X    = 1265f;
    private static final float GRID_START_Y  = 700f;
    private static final float GRID_SPACING  = 75f;

    // ── Colors ───────────────────────────────────────────────────────────
    private static final Color COLOR_SELECTED  = new Color(1f,    1f,    1f,    1f);
    private static final Color COLOR_REBINDING = new Color(1f, 0.85f, 0.22f,    1f);
    private static final Color COLOR_IDLE      = new Color(1f,    1f,    1f,    1f);

    private ShapeRenderer shapeRenderer;

    private final GameAction[] actions = {
        GameAction.UP, GameAction.LEFT, GameAction.ATTACK, GameAction.DASH,
        GameAction.DOWN, GameAction.RIGHT, GameAction.JUMP, GameAction.FOCUS_CAST,
        GameAction.INVENTORY
    };

    private final int totalSelectables = actions.length + 2; // + RESET + BACK

    private boolean isRebinding = false;

    @Override protected String getTitle()     { return LocalizationManager.get("keyboard.title"); }
    @Override protected int    getItemCount() { return totalSelectables; }

    @Override
    protected float getItemY(int index) {
        if (index == actions.length + 1) return BACK_Y;
        if (index == actions.length)     return BACK_Y + 110f;
        int row = (index == 8) ? 4 : (index < 4 ? index : index - 4);
        return GRID_START_Y - (row * GRID_SPACING);
    }

    @Override
    protected float getRowStartX(int index) {
        if (index < actions.length) {
            boolean left = (index < 4) || (index == 8);
            return left ? COL1_TEXT_X : COL2_TEXT_X;
        }
        String label = (index == actions.length)
            ? LocalizationManager.get("keyboard.reset")
            : LocalizationManager.get("menu.back");
        return centeredStartX(label, FontManager.getMenuSmall());
    }

    @Override
    protected float getRowEndX(int index) {
        if (index < actions.length) {
            boolean left = (index < 4) || (index == 8);
            return (left ? COL1_KEY_X : COL2_KEY_X) + KEYCAP_SIZE;
        }
        String label = (index == actions.length)
            ? LocalizationManager.get("keyboard.reset")
            : LocalizationManager.get("menu.back");
        return centeredEndX(label, FontManager.getMenuSmall());
    }

    @Override
    protected void onShow() {
        shapeRenderer = new ShapeRenderer();
        isRebinding   = false;
    }

    // Override render() fully to inject ShapeRenderer passes
    @Override
    public void render(float delta) {
        if (delta > 0.1f) delta = 0.1f;

        handleInput(delta);
        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        // Pass 1: background (blurred game frame if inGame, atmosphere otherwise) + header
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderBackground();
        renderHeader();
        batch.end();

        // Pass 2: keycap outlines (ShapeRenderer Line)
        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < actions.length; i++) renderKeycapOutline(i);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);

        // Pass 3: arrow triangles (ShapeRenderer Filled)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < actions.length; i++) renderArrowContent(i);
        shapeRenderer.end();

        // Pass 4: labels + pointers
        batch.begin();
        renderItems();
        drawPointers(getRowStartX(selectedIndex), getRowEndX(selectedIndex), currentPointerY);
        batch.end();
    }

    @Override
    protected void renderItems() {
        FontManager.getMenuSmall().setColor(1f, 1f, 1f, 1f);

        for (int i = 0; i < totalSelectables; i++) {
            float y = getItemY(i);

            if (i < actions.length) {
                boolean isLeft = (i < 4) || (i == 8);
                float textX = isLeft ? COL1_TEXT_X : COL2_TEXT_X;
                float keyX  = isLeft ? COL1_KEY_X  : COL2_KEY_X;

                FontManager.getMenuSmall().draw(batch,
                    actions[i].getDescription().toUpperCase(), textX, y);

                String  raw       = InputManager.getKeyName(actions[i]);
                boolean showArrow = isArrowName(raw)
                    && !(isRebinding && selectedIndex == i);

                if (!showArrow) {
                    String label = (isRebinding && selectedIndex == i) ? "..." : toKeyLabel(raw);
                    layout.setText(FontManager.getMenuSmall(), label);
                    float tx = keyX  + (KEYCAP_SIZE - layout.width)  / 2f;
                    float ty = keycapBlockY(i) + (KEYCAP_SIZE + layout.height) / 2f;
                    if (isRebinding && selectedIndex == i)
                        FontManager.getMenuSmall().setColor(COLOR_REBINDING);
                    FontManager.getMenuSmall().draw(batch, label, tx, ty);
                    FontManager.getMenuSmall().setColor(1f, 1f, 1f, 1f);
                }

            } else {
                String label = (i == actions.length)
                    ? LocalizationManager.get("keyboard.reset")
                    : LocalizationManager.get("menu.back");
                layout.setText(FontManager.getMenuSmall(), label);
                FontManager.getMenuSmall().draw(batch, label,
                    (V_WIDTH - layout.width) / 2f, y);
            }
        }
    }

    // Override handleInput for rebinding mode + custom grid navigation
    @Override
    protected void handleInput(float delta) {
        if (isRebinding) {
            for (int k = 0; k < 256; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    if (k == Input.Keys.ESCAPE) {
                        isRebinding = false;
                    } else if (isAllowedKey(k)) {
                        InputManager.updateKeybinding(actions[selectedIndex], k);
                        isRebinding = false;
                    }
                    break;
                }
            }
            return; // swallow all other input while rebinding
        }

        // Custom grid navigation
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if      (selectedIndex >= 0 && selectedIndex <= 2) selectedIndex++;
            else if (selectedIndex == 3)                       selectedIndex = 8;
            else if (selectedIndex >= 4 && selectedIndex <= 6) selectedIndex++;
            else if (selectedIndex == 7 || selectedIndex == 8) selectedIndex = 9;
            else if (selectedIndex == 9)                       selectedIndex = 10;
            else if (selectedIndex == 10)                      selectedIndex = 0;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if      (selectedIndex == 0 || selectedIndex == 4) selectedIndex = 10;
            else if (selectedIndex >= 1 && selectedIndex <= 3) selectedIndex--;
            else if (selectedIndex >= 5 && selectedIndex <= 7) selectedIndex--;
            else if (selectedIndex == 8)                       selectedIndex = 3;
            else if (selectedIndex == 9)                       selectedIndex = 8;
            else if (selectedIndex == 10)                      selectedIndex = 9;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
            || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            if      (selectedIndex < 4)                       selectedIndex += 4;
            else if (selectedIndex >= 4 && selectedIndex < 8) selectedIndex -= 4;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.Z))   selectCurrent();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) goBack();

        // Mouse hover
        float mx = getMouseX();
        float my = getMouseY();
        for (int i = 0; i < totalSelectables; i++) {
            float y = getItemY(i);
            layout.setText(FontManager.getMenuSmall(), "A");
            float rh = layout.height;
            if (mx >= getRowStartX(i) && mx <= getRowEndX(i)
                && my >= y - rh && my <= y) selectedIndex = i;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) selectCurrent();
    }

    @Override
    protected void selectCurrent() {
        if      (selectedIndex < actions.length) isRebinding = true;
        else if (selectedIndex == actions.length) InputManager.resetToDefaults();
        else    goBack();
    }

    @Override
    protected void goBack() {
        SettingsMenuController.returnToSettings();
    }

    // ── ShapeRenderer helpers ─────────────────────────────────────────────
    private void renderKeycapOutline(int index) {
        boolean left = (index < 4) || (index == 8);
        float   kx   = left ? COL1_KEY_X : COL2_KEY_X;
        float   by   = keycapBlockY(index);
        float   r    = KEYCAP_RADIUS;
        float   w    = KEYCAP_SIZE, h = KEYCAP_SIZE;

        if      (isRebinding && selectedIndex == index) shapeRenderer.setColor(COLOR_REBINDING);
        else if (selectedIndex == index)                shapeRenderer.setColor(COLOR_SELECTED);
        else                                            shapeRenderer.setColor(COLOR_IDLE);

        shapeRenderer.line(kx + r, by,     kx + w - r, by);
        shapeRenderer.line(kx + r, by + h, kx + w - r, by + h);
        shapeRenderer.line(kx,     by + r, kx,          by + h - r);
        shapeRenderer.line(kx + w, by + r, kx + w,      by + h - r);
        drawArcOutline(kx + r,     by + r,     r, 180f, 270f, 16);
        drawArcOutline(kx + w - r, by + r,     r, 270f, 360f, 16);
        drawArcOutline(kx + r,     by + h - r, r,  90f, 180f, 16);
        drawArcOutline(kx + w - r, by + h - r, r,   0f,  90f, 16);
    }

    private void renderArrowContent(int index) {
        if (isRebinding && selectedIndex == index) return;
        String name = InputManager.getKeyName(actions[index]);
        if (!isArrowName(name)) return;

        boolean left = (index < 4) || (index == 8);
        float   kx   = left ? COL1_KEY_X : COL2_KEY_X;
        float   cx   = kx + KEYCAP_SIZE / 2f;
        float   cy   = keycapBlockY(index) + KEYCAP_SIZE / 2f;
        float   s    = ARROW_HALF;

        shapeRenderer.setColor(selectedIndex == index ? COLOR_SELECTED : COLOR_IDLE);
        switch (name) {
            case "Left":  shapeRenderer.triangle(cx+s, cy-s, cx+s, cy+s, cx-s, cy); break;
            case "Right": shapeRenderer.triangle(cx-s, cy-s, cx-s, cy+s, cx+s, cy); break;
            case "Up":    shapeRenderer.triangle(cx-s, cy-s, cx+s, cy-s, cx,  cy+s); break;
            case "Down":  shapeRenderer.triangle(cx-s, cy+s, cx+s, cy+s, cx,  cy-s); break;
        }
    }

    private void drawArcOutline(float cx, float cy, float r,
                                float s, float e, int n) {
        float step = (e - s) / n;
        for (int i = 0; i < n; i++) {
            double a1 = Math.toRadians(s + i * step);
            double a2 = Math.toRadians(s + (i + 1) * step);
            shapeRenderer.line(
                cx + r * (float) Math.cos(a1), cy + r * (float) Math.sin(a1),
                cx + r * (float) Math.cos(a2), cy + r * (float) Math.sin(a2));
        }
    }

    private float keycapBlockY(int i) { return getItemY(i) - KEYCAP_SIZE / 2f - 6f; }

    private static boolean isAllowedKey(int k) {
        return (k >= Input.Keys.A && k <= Input.Keys.Z)
            || (k >= Input.Keys.NUM_0 && k <= Input.Keys.NUM_9)
            || k == Input.Keys.LEFT || k == Input.Keys.RIGHT
            || k == Input.Keys.UP   || k == Input.Keys.DOWN;
    }

    private static boolean isArrowName(String n) {
        return "Left".equals(n) || "Right".equals(n)
            || "Up".equals(n)   || "Down".equals(n);
    }

    private static String toKeyLabel(String raw) {
        if (raw == null || raw.isEmpty()) return "?";
        return raw.length() == 1 ? raw.toUpperCase() : raw;
    }

    @Override
    protected void onDispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
