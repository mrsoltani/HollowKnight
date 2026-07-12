package com.Graphic.views.screens;

import com.Graphic.Main;
import com.Graphic.managers.EventBus;
import com.Graphic.managers.FontManager;
import com.Graphic.utils.ScreenCapture;
import com.Graphic.views.atmosphere.MenuAtmosphere;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import static com.Graphic.utils.Constants.*;
import static com.Graphic.utils.Constants.Menu.*;
import static com.Graphic.utils.Constants.SettingsMenu.TITLE_Y;

public abstract class BaseMenuScreen implements Screen {

    protected SpriteBatch        batch;
    // ExtendViewport instead of FitViewport: when the window's aspect ratio
    // doesn't match V_WIDTH:V_HEIGHT, this extends the world to fill the
    // screen completely instead of leaving black bars on the mismatched axis.
    protected ExtendViewport     viewport;
    protected OrthographicCamera camera;
    protected Texture            titleBottom;
    protected Texture            pointer;
    protected TextureRegion      pointerRegion;
    protected Texture            darkOverlay;
    protected final GlyphLayout  layout = new GlyphLayout();

    protected int     selectedIndex   = 0;
    protected float   currentPointerY = 0f;

    // Guards against the exact same ESC keypress that just switched us TO
    // this screen also being read as a fresh "just pressed" event here,
    // which would immediately call goBack() and bounce right back out.
    // Armed in show() if ESC happens to be down at that moment; disarmed
    // the first time it's observed released.
    private boolean escGuardActive = false;

    // ── Shared in-game background blur ───────────────────────────────────
    private static final float BLUR_RADIUS = 1.4f;
    private static final float DARKNESS    = 0.35f;

    private ShaderProgram blurShader;
    private boolean       blurShaderOk;

    // ── Abstract contract ────────────────────────────────────────────────
    protected abstract String getTitle();
    protected abstract int    getItemCount();
    protected abstract float  getItemY(int index);
    protected abstract float  getRowStartX(int index);
    protected abstract float  getRowEndX(int index);
    protected abstract void   renderItems();
    protected abstract void   selectCurrent();
    protected abstract void   goBack();

    // ── Optional hooks ───────────────────────────────────────────────────
    protected void onShow()                       {}
    protected void onDispose()                    {}
    protected void handleExtraInput(float delta)  {}
    protected void renderStage()                  {}
    protected void handleItemClick(int index)     { selectCurrent(); }
    protected void renderHeader() {
        String t = getTitle();
        layout.setText(FontManager.getTitle(), t);
        FontManager.getTitle().setColor(1f, 1f, 1f, 1f);
        FontManager.getTitle().draw(batch, t,
            (viewport.getWorldWidth() - layout.width) / 2f, TITLE_Y);

        float dw = titleBottom.getWidth();
        float dh = titleBottom.getHeight();
        batch.draw(titleBottom, (viewport.getWorldWidth() - dw) / 2f, TITLE_Y - dh - 90f);
    }

    // ── Background: frozen blurred game frame when paused mid-game,
    //    atmosphere when reached from the main menu. Always sized to the
    //    viewport's actual (possibly extended) world dimensions, never the
    //    fixed V_WIDTH/V_HEIGHT constants, so it always covers the full
    //    visible area with no gaps regardless of window aspect ratio. ─────
    protected void renderBackground() {
        boolean inGame = Main.getInstance().isInGame();
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        if (!inGame) {
            MenuAtmosphere.getInstance().render(batch);
            return;
        }

        if (!ScreenCapture.hasCaptured()) {
            batch.draw(darkOverlay, 0, 0, w, h);
            return;
        }

        TextureRegion capture = ScreenCapture.getRegion();

        if (blurShaderOk) {
            batch.setShader(blurShader);
            blurShader.setUniformf("u_resolution",
                (float) Gdx.graphics.getWidth(),
                (float) Gdx.graphics.getHeight());
            blurShader.setUniformf("u_blurRadius", BLUR_RADIUS);
            blurShader.setUniformf("u_darkness",   DARKNESS);

            batch.draw(capture, 0, 0, w, h);

            batch.setShader(null);
        } else {
            batch.draw(capture, 0, 0, w, h);
            batch.draw(darkOverlay, 0, 0, w, h);
        }
    }

    // ── Template ─────────────────────────────────────────────────────────
    @Override
    public void show() {
        batch         = new SpriteBatch();
        camera        = new OrthographicCamera();
        viewport      = new ExtendViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply(true);

        titleBottom   = new Texture(Gdx.files.internal(PATH_TITLE_BOTTOM));
        pointer       = new Texture(Gdx.files.internal(PATH_POINTER));
        pointerRegion = new TextureRegion(pointer);

        // 1x1 dark pixel used as in-game background overlay fallback
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0.82f);
        pm.fill();
        darkOverlay = new Texture(pm);
        pm.dispose();

        ShaderProgram.pedantic = false;
        blurShader = new ShaderProgram(
            Gdx.files.internal("shaders/blur.vert"),
            Gdx.files.internal("shaders/blur.frag")
        );
        blurShaderOk = blurShader.isCompiled();
        if (!blurShaderOk) {
            Gdx.app.error("BaseMenuScreen",
                "Blur shader FAILED to compile — falling back to plain dim overlay.\n"
                    + blurShader.getLog());
        }

        onShow();
        currentPointerY = getItemY(0);
        escGuardActive = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
    }

    @Override
    public void render(float delta) {
        if (delta > 0.1f) delta = 0.1f;

        handleInput(delta);

        // A navigation action inside handleInput() (goBack()/selectCurrent())
        // may have switched the active screen this frame. If so, stop here —
        // continuing to render this now-inactive screen is what caused the
        // "stuck on pause" bug: it kept drawing stale UI over the switch,
        // and touched ScreenCapture right after the new screen's resize()
        // had just reset it. Mirrors the `return;` GameScreen already uses
        // right after enterPause().
        if (Main.getInstance().getScreen() != this) {
            return;
        }

        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        renderBackground();
        renderHeader();
        renderItems();
        drawPointers(getRowStartX(selectedIndex), getRowEndX(selectedIndex), currentPointerY);
        batch.end();

        renderStage();
    }

    protected void update(float delta) {
        if (!Main.getInstance().isInGame()) MenuAtmosphere.getInstance().update(delta);
        currentPointerY += (getItemY(selectedIndex) - currentPointerY) * 12f * delta;
    }

    protected void handleInput(float delta) {
        int total = getItemCount();
        int prev  = selectedIndex;

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex = (selectedIndex - 1 + total) % total;
            if (selectedIndex != prev) EventBus.emit(EventBus.Event.MENU_NAVIGATE); // ← hover SFX
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex = (selectedIndex + 1) % total;
            if (selectedIndex != prev) EventBus.emit(EventBus.Event.MENU_NAVIGATE); // ← hover SFX
        }

        handleExtraInput(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            EventBus.emit(EventBus.Event.MENU_SELECT);  // ← select SFX
            selectCurrent();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            EventBus.emit(EventBus.Event.MENU_NAVIGATE); // ← back SFX
            goBack();
        }

        // Mouse hover
        float mx = getMouseX(), my = getMouseY();
        for (int i = 0; i < total; i++) {
            float y = getItemY(i);
            layout.setText(FontManager.getMenu(), "A");
            if (mx >= getRowStartX(i) && mx <= getRowEndX(i)
                && my >= y - layout.height && my <= y) {
                if (i != selectedIndex) {
                    selectedIndex = i;
                    EventBus.emit(EventBus.Event.MENU_NAVIGATE); // ← hover SFX on mouse move
                }
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            EventBus.emit(EventBus.Event.MENU_SELECT);  // ← click SFX
            handleItemClick(selectedIndex);
        }
    }

    @Override
    public void dispose() {
        if (batch       != null) batch.dispose();
        if (titleBottom != null) titleBottom.dispose();
        if (pointer     != null) pointer.dispose();
        if (darkOverlay != null) darkOverlay.dispose();
        if (blurShader  != null) blurShader.dispose();
        onDispose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    protected void drawPointers(float startX, float endX, float y) {
        float pw = pointer.getWidth()  * 0.4f;
        float ph = pointer.getHeight() * 0.4f;
        layout.setText(FontManager.getMenu(), "A");
        float py = (y - layout.height / 2f) - ph / 2f;

        batch.draw(pointer, startX - POINTER_OFFSET - pw, py, pw, ph);
        pointerRegion.flip(true, false);
        batch.draw(pointerRegion, endX + POINTER_OFFSET, py, pw, ph);
        pointerRegion.flip(true, false);
    }

    protected Vector2 drawCentered(String text, float y, BitmapFont font) {
        layout.setText(font, text);
        float x = (viewport.getWorldWidth() - layout.width) / 2f;
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, text, x, y);
        return new Vector2(x, x + layout.width);
    }

    protected float centeredStartX(String text, BitmapFont font) {
        layout.setText(font, text);
        return (viewport.getWorldWidth() - layout.width) / 2f;
    }

    protected float centeredEndX(String text, BitmapFont font) {
        layout.setText(font, text);
        return (viewport.getWorldWidth() + layout.width) / 2f;
    }

    protected float getMouseX() {
        return viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).x;
    }
    protected float getMouseY() {
        return viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).y;
    }


    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
}
