package com.Graphic.views.screens;

import com.Graphic.Main;
import com.Graphic.managers.FontManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

    private static final float V_WIDTH        = 1920f;
    private static final float V_HEIGHT       = 1080f;
    private static final float FIRST_ITEM_Y   = 480f;
    private static final float ITEM_SPACING   = 90f;
    private static final float POINTER_OFFSET = 50f;
    private static final float BOB_SPEED      = 3f;
    private static final float BOB_AMPLITUDE  = 8f;
    private TextureRegion pointerRegion;


    private SpriteBatch batch;
    private FitViewport viewport;
    private OrthographicCamera camera;

    private Texture background;
    private Texture logo;
    private Texture pointer;
    private Texture beam;


    private final String[] items = {
        "START GAME", "OPTIONS", "ACHIEVEMENTS", "EXTRAS", "QUIT GAME"
    };

    private int selectedIndex  = 0;
    private float bobTimer     = 0f;
    private float currentPointerY;
    private final GlyphLayout layout = new GlyphLayout();

    @Override
    public void show() {
        batch    = new SpriteBatch();
        camera   = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply(true);

        background = new Texture(Gdx.files.internal("ui/menu/background.png"));
        logo       = new Texture(Gdx.files.internal("ui/menu/main menu/logo.png"));
        pointer    = new Texture(Gdx.files.internal("ui/menu/main menu/pointer.png"));
        pointerRegion = new TextureRegion(pointer);
        beam = new Texture(Gdx.files.internal("ui/menu/main menu/beam.png"));

        currentPointerY = getItemY(0);
    }

    @Override
    public void render(float delta) {
        handleInput();
        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // 1. Background
        batch.draw(background, 0, 0, V_WIDTH, V_HEIGHT);

        // 2. Logo
        float logoW = logo.getWidth();
        float logoH = logo.getHeight();
        batch.draw(logo,
            (V_WIDTH - logoW) / 2f,
            V_HEIGHT - logoH
        );

        // 3. Menu items
        for (int i = 0; i < items.length; i++) {
            layout.setText(FontManager.getMenu(), items[i]);
            float itemX = (V_WIDTH - layout.width) / 2f;

            if (i == selectedIndex) {
                FontManager.getMenu().setColor(1f, 1f, 1f, 1f);
            } else {
                FontManager.getMenu().setColor(0.5f, 0.5f, 0.5f, 1f);
            }

            FontManager.getMenu().draw(batch, items[i], itemX, getItemY(i));
        }

        // 4. Both-sided pointers
        layout.setText(FontManager.getMenu(), items[selectedIndex]);
        float textX    = (V_WIDTH - layout.width) / 2f;
        float pointerW = pointer.getWidth();
        float pointerH = pointer.getHeight();
        float bobOffset = MathUtils.sin(bobTimer) * BOB_AMPLITUDE;
        float pointerY  = currentPointerY - pointerH / 2f - 10;

        batch.draw(pointer,
            textX - POINTER_OFFSET - pointerW + bobOffset,
            pointerY
        );

        pointerRegion.flip(true, false); // flip horizontally
        batch.draw(pointerRegion,
            textX + layout.width + POINTER_OFFSET - bobOffset,
            pointerY
        );
        pointerRegion.flip(true, false); // flip back to normal for next frame
        batch.end();
    }

    private void update(float delta) {
        bobTimer += BOB_SPEED * delta;
        float targetY   = getItemY(selectedIndex);
        currentPointerY += (targetY - currentPointerY) * 12f * delta;
    }

    private void handleInput() {
        // Keyboard navigation
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex = (selectedIndex - 1 + items.length) % items.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex = (selectedIndex + 1) % items.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            selectCurrent();
        }

        // Mouse hover
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        for (int i = 0; i < items.length; i++) {
            layout.setText(FontManager.getMenu(), items[i]);
            float itemX = (V_WIDTH - layout.width) / 2f;
            float itemY = getItemY(i);
            if (mouseX >= itemX
                && mouseX <= itemX + layout.width
                && mouseY >= itemY - layout.height
                && mouseY <= itemY) {
                selectedIndex = i;
            }
        }

        // Mouse click
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = getMouseX();
            float my = getMouseY();
            for (int i = 0; i < items.length; i++) {
                layout.setText(FontManager.getMenu(), items[i]);
                float itemX = (V_WIDTH - layout.width) / 2f;
                float itemY = getItemY(i);
                if (mx >= itemX
                    && mx <= itemX + layout.width
                    && my >= itemY - layout.height
                    && my <= itemY) {
                    selectedIndex = i;
                    selectCurrent();
                }
            }
        }
    }

    private void selectCurrent() {
        switch (selectedIndex) {
            case 0: /* game.setScreen(new GameStartScreen(game)); */ break;
            case 1: /* game.setScreen(new SettingsScreen(game));  */ break;
            case 2: /* game.setScreen(new AchievementsScreen(game)); */ break;
            case 3: /* game.setScreen(new ExtrasScreen(game));    */ break;
            case 4: Gdx.app.exit(); break;
        }
    }

    private float getItemY(int index) {
        return FIRST_ITEM_Y - (index * ITEM_SPACING);
    }

    private float getMouseX() {
        return viewport.unproject(
            new Vector2(Gdx.input.getX(), Gdx.input.getY())
        ).x;
    }

    private float getMouseY() {
        return viewport.unproject(
            new Vector2(Gdx.input.getX(), Gdx.input.getY())
        ).y;
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        logo.dispose();
        pointer.dispose();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
}
