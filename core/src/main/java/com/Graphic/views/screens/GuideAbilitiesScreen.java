package com.Graphic.views.screens;

import com.Graphic.controllers.AbilitiesScreenController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.views.atmosphere.MenuAtmosphere;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import static com.Graphic.utils.Constants.V_HEIGHT;
import static com.Graphic.utils.Constants.V_WIDTH;

public class GuideAbilitiesScreen implements Screen {

    // ── Layout ────────────────────────────────────────────────────────────
    private static final float TITLE_Y       = V_HEIGHT * 0.92f;
    private static final float IMAGE_Y       = V_HEIGHT * 0.82f;
    private static final float ABILITY_TITLE_Y = V_HEIGHT * 0.38f;
    private static final float DESC_Y        = V_HEIGHT * 0.31f;
    private static final float DESC_WIDTH    = V_WIDTH  * 0.55f;
    private static final float DOT_Y         = V_HEIGHT * 0.08f;
    private static final float DOT_SPACING   = 32f;
    private static final float DOT_RADIUS    = 7f;

    // ── Slide data ────────────────────────────────────────────────────────
    private static final String[] IMAGE_PATHS = {
        "ui/abilities/Focus_prompt_temp.png",
        "ui/abilities/Fireball_prompt.png",
        "ui/abilities/Dash_Prompt.png",
        "ui/abilities/Wall_Jump_Prompt.png",
        "ui/abilities/_0006_scream_spell.png"
    };

    private static final String[] TITLE_KEYS = {
        "abilities.focus.title",
        "abilities.fireball.title",
        "abilities.dash.title",
        "abilities.walljump.title",
        "abilities.scream.title"
    };

    private static final String[] DESC_KEYS = {
        "abilities.focus.desc",
        "abilities.fireball.desc",
        "abilities.dash.desc",
        "abilities.walljump.desc",
        "abilities.scream.desc"
    };

    // ── State ─────────────────────────────────────────────────────────────
    private int   currentSlide   = 0;
    private float slideOffset    = 0f;   // 0 = fully on screen, ±V_WIDTH = fully off
    private float slideTarget    = 0f;
    private int   direction      = 1;    // +1 = next coming from right, -1 = prev from left

    // ── Rendering ─────────────────────────────────────────────────────────
    private SpriteBatch        batch;
    private FitViewport        viewport;
    private OrthographicCamera camera;
    private Texture[]          images;
    private final GlyphLayout  layout = new GlyphLayout();

    // ── Input throttle ────────────────────────────────────────────────────
    private float inputCooldown = 0f;

    // =========================================================================
    // Screen lifecycle
    // =========================================================================

    @Override
    public void show() {
        batch    = new SpriteBatch();
        camera   = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply(true);

        images = new Texture[IMAGE_PATHS.length];
        for (int i = 0; i < IMAGE_PATHS.length; i++) {
            images[i] = new Texture(Gdx.files.internal(IMAGE_PATHS[i]));
        }

        currentSlide = 0;
        slideOffset  = 0f;
        slideTarget  = 0f;
    }

    @Override
    public void render(float delta) {
        if (delta > 0.1f) delta = 0.1f;

        handleInput(delta);
        updateSlide(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        MenuAtmosphere.getInstance().render(batch);
        renderTitle();
        renderSlide(currentSlide,  slideOffset);
        renderIndicatorDots();

        batch.end();

        MenuAtmosphere.getInstance().update(delta);
    }

    // =========================================================================
    // Input
    // =========================================================================

    private void handleInput(float delta) {
        inputCooldown -= delta;

        if (inputCooldown > 0f) return;

        boolean right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);
        boolean left  = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean back  = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.Z);

        if (right && currentSlide < images.length - 1) {
            currentSlide++;
            slideOffset   = V_WIDTH;   // next slide enters from the right
            direction     = 1;
            inputCooldown = 0.15f;
        } else if (left && currentSlide > 0) {
            currentSlide--;
            slideOffset   = -V_WIDTH;  // previous slide enters from the left
            direction     = -1;
            inputCooldown = 0.15f;
        } else if (back) {
            goBack();
        }
    }

    // =========================================================================
    // Slide animation
    // =========================================================================

    private void updateSlide(float delta) {
        if (Math.abs(slideOffset) < 1f) {
            slideOffset = 0f;
            return;
        }
        // Lerp toward 0 (fully on screen)
        slideOffset += (0f - slideOffset) * Math.min(1f, 14f * delta);
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    private void renderTitle() {
        String title = LocalizationManager.get("guide.abilities.title");
        FontManager.getTitle().setColor(Color.WHITE);
        layout.setText(FontManager.getTitle (), title);
        FontManager.getTitle().draw(batch, title,
            (V_WIDTH - layout.width) / 2f, TITLE_Y);
    }

    private void renderSlide(int index, float xOffset) {
        if (index < 0 || index >= images.length) return;

        Texture img  = images[index];
        float   imgW = img.getWidth();
        float   imgH = img.getHeight();
        float   imgX = (V_WIDTH - imgW) / 2f + xOffset;
        float   imgY = IMAGE_Y - imgH;

        // Image at its natural resolution, centered
        batch.draw(img, imgX, imgY, imgW, imgH);

        // Ability title
        String titleText = LocalizationManager.get(TITLE_KEYS[index]);
        FontManager.getMenu().setColor(Color.WHITE);
        layout.setText(FontManager.getMenu(), titleText);
        FontManager.getMenu().draw(batch, titleText,
            (V_WIDTH - layout.width) / 2f + xOffset, ABILITY_TITLE_Y);

        // Description — wrapped, centered block
        String descText = LocalizationManager.get(DESC_KEYS[index]);
        FontManager.getBody().setColor(new Color(0.8f, 0.8f, 0.85f, 1f));
        FontManager.getBody().draw(
            batch,
            descText,
            (V_WIDTH - DESC_WIDTH) / 2f + xOffset,
            DESC_Y,
            DESC_WIDTH,
            Align.center,
            true
        );

        // Navigation hints at sides (only show arrows if there's a slide to go to)
        FontManager.getBody().setColor(new Color(1f, 1f, 1f, 0.5f));
        if (index > 0) {
            FontManager.getBody().draw(batch, "<",
                (V_WIDTH - DESC_WIDTH) / 2f - 60f + xOffset, ABILITY_TITLE_Y);
        }
        if (index < images.length - 1) {
            FontManager.getBody().draw(batch, ">",
                (V_WIDTH + DESC_WIDTH) / 2f + 20f + xOffset, ABILITY_TITLE_Y);
        }

        FontManager.getBody().setColor(Color.WHITE);
    }

    private void renderIndicatorDots() {
        int   count      = images.length;
        float totalWidth = (count - 1) * DOT_SPACING;
        float startX     = (V_WIDTH - totalWidth) / 2f;

        for (int i = 0; i < count; i++) {
            float cx = startX + i * DOT_SPACING;

            if (i == currentSlide) {
                // Active dot — draw as "●" using body font
                FontManager.getBody().setColor(Color.WHITE);
                FontManager.getBody().draw(batch, "●", cx - 6f, DOT_Y + 10f);
            } else {
                // Inactive dot — dimmed "○"
                FontManager.getBody().setColor(new Color(1f, 1f, 1f, 0.35f));
                FontManager.getBody().draw(batch, "○", cx - 6f, DOT_Y + 10f);
            }
        }

        FontManager.getBody().setColor(Color.WHITE);
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    private void goBack() {
        AbilitiesScreenController.back();
    }

    // =========================================================================
    // Boilerplate
    // =========================================================================

    @Override
    public void resize(int w, int h) { viewport.update(w, h, true); }

    @Override
    public void dispose() {
        batch.dispose();
        if (images != null) {
            for (Texture t : images) {
                if (t != null) t.dispose();
            }
        }
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
}
