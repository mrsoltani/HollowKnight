package com.Graphic.views.screens;

import com.Graphic.models.spells.Fireball;
import com.Graphic.models.spells.Scream;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class SpellTestScreen implements Screen {

    private static final float WORLD_WIDTH  = 960f;
    private static final float WORLD_HEIGHT = 540f;
    private static final float STEP_SMALL   = 2f;
    private static final float STEP_ANGLE   = 1f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    private TextureAtlas ballAtlas;
    private TextureAtlas screamAtlas;

    private final Array<Fireball> fireballs = new Array<>();
    private final Array<Scream> screams = new Array<>();

    private boolean voidHeartActive = false;
    private boolean showHitboxes = true;

    private float casterX = 300f;
    private final float casterY = 120f;



    private final Rectangle wall = new Rectangle(820f, 0f, 20f, WORLD_HEIGHT);

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        ballAtlas = new TextureAtlas(Gdx.files.internal("sprites/spells/Ball.atlas"));
        screamAtlas = new TextureAtlas(Gdx.files.internal("sprites/spells/Scream.atlas"));
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        update(delta);
        draw();
    }

    private void handleInput(float delta) {
        boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);

        if (Gdx.input.isKeyJustPressed(Keys.F)) {
            TextureAtlas atlas = ballAtlas;
            fireballs.add(new Fireball(atlas, casterX, casterY, !shift, voidHeartActive));
        }

        if (Gdx.input.isKeyJustPressed(Keys.G)) {
            screams.add(new Scream(screamAtlas, casterX, casterY, voidHeartActive));
        }

        if (Gdx.input.isKeyJustPressed(Keys.V)) {
            voidHeartActive = !voidHeartActive;
        }

        if (Gdx.input.isKeyJustPressed(Keys.H)) {
            showHitboxes = !showHitboxes;
        }

        if (Gdx.input.isKeyJustPressed(Keys.C)) {
            fireballs.clear();
            screams.clear();
        }

        if (Gdx.input.isKeyPressed(Keys.LEFT))  casterX -= 150f * delta;
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) casterX += 150f * delta;


        if (Gdx.input.isKeyJustPressed(Keys.Q)) Fireball.HITBOX_WIDTH  = Math.max(2f, Fireball.HITBOX_WIDTH - STEP_SMALL);
        if (Gdx.input.isKeyJustPressed(Keys.E)) Fireball.HITBOX_WIDTH  += STEP_SMALL;
        if (Gdx.input.isKeyJustPressed(Keys.Z)) Fireball.HITBOX_HEIGHT = Math.max(2f, Fireball.HITBOX_HEIGHT - STEP_SMALL);
        if (Gdx.input.isKeyJustPressed(Keys.X)) Fireball.HITBOX_HEIGHT += STEP_SMALL;


        if (Gdx.input.isKeyJustPressed(Keys.R)) Scream.HEIGHT = Math.max(4f, Scream.HEIGHT - STEP_SMALL * 4f);
        if (Gdx.input.isKeyJustPressed(Keys.T)) Scream.HEIGHT += STEP_SMALL * 4f;
        if (Gdx.input.isKeyJustPressed(Keys.Y)) Scream.ANGLE_DEGREES = Math.max(2f, Scream.ANGLE_DEGREES - STEP_ANGLE);
        if (Gdx.input.isKeyJustPressed(Keys.U)) Scream.ANGLE_DEGREES = Math.min(179f, Scream.ANGLE_DEGREES + STEP_ANGLE);
    }

    private void update(float delta) {
        for (Fireball fb : fireballs) {
            fb.update(delta);
            if (fb.getPhase() == Fireball.Phase.TRAVELING && fb.getHitbox().overlaps(wall)) {
                fb.hitWall();
            }
        }
        for (int i = fireballs.size - 1; i >= 0; i--) {
            if (fireballs.get(i).isFinished()) fireballs.removeIndex(i);
        }

        for (Scream sc : screams) {
            sc.update(delta);
        }
        for (int i = screams.size - 1; i >= 0; i--) {
            if (screams.get(i).isFinished()) screams.removeIndex(i);
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0, 200, 200, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);


        batch.begin();
        for (Fireball fb : fireballs) fb.render(batch);
        for (Scream sc : screams) sc.render(batch);
        batch.end();


        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(wall.x, wall.y, wall.width, wall.height);
        shapes.setColor(voidHeartActive ? Color.PURPLE : Color.CYAN);
        shapes.circle(casterX, casterY, 4f);
        shapes.end();

        if (showHitboxes) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(voidHeartActive ? Color.RED : Color.YELLOW);
            for (Fireball fb : fireballs) {
                Rectangle r = fb.getHitbox();
                shapes.rect(r.x, r.y, r.width, r.height);
            }
            for (Scream sc : screams) {
                float[] v = sc.getHitboxPolygon().getTransformedVertices();
                shapes.triangle(v[0], v[1], v[2], v[3], v[4], v[5]);
            }
            shapes.end();
        }


        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch,
            "F: fireball right | SHIFT+F: left | G: scream | V: void heart (" + voidHeartActive + ") | H: hitboxes (" + showHitboxes + ") | C: clear",
            10, WORLD_HEIGHT - 10);
        font.draw(batch,
            "Fireball hitbox  Q/E width=" + Fireball.HITBOX_WIDTH + "  Z/X height=" + Fireball.HITBOX_HEIGHT,
            10, WORLD_HEIGHT - 28);
        font.draw(batch,
            "Scream   hitbox  R/T height=" + Scream.HEIGHT + "  Y/U angle=" + Scream.ANGLE_DEGREES,
            10, WORLD_HEIGHT - 46);
        font.draw(batch, "LEFT/RIGHT: move caster", 10, WORLD_HEIGHT - 64);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        ballAtlas.dispose();
        screamAtlas.dispose();
    }
}
