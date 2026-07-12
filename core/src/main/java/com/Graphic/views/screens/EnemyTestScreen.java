package com.Graphic.views.screens;

import com.Graphic.models.enemies.BaseEnemy;
import com.Graphic.models.enemies.CrystalCrawler;
import com.Graphic.models.enemies.CrystalGuardian;
import com.Graphic.models.enemies.HuskHornhead;
import com.Graphic.models.enemies.Mosquito;
import com.Graphic.models.spells.Damageable;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class EnemyTestScreen implements Screen {

    private static final float WORLD_WIDTH  = 1280f;
    private static final float WORLD_HEIGHT = 720f;
    private static final float PLAYER_SPEED = 260f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    private final Array<BaseEnemy> enemies = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();

    private MockPlayer player;
    private boolean showDebug = true;


    private float contactCooldown = 0f;
    private static final float CONTACT_INTERVAL = 0.5f;





    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        buildScene();
    }


    private void buildScene() {
        disposeEnemies();
        enemies.clear();
        platforms.clear();





        float t = 40f;
        platforms.add(new Rectangle(0, 0, WORLD_WIDTH, 60));
        platforms.add(new Rectangle(0, WORLD_HEIGHT - t, WORLD_WIDTH, t));
        platforms.add(new Rectangle(0, 0, t, WORLD_HEIGHT));
        platforms.add(new Rectangle(WORLD_WIDTH - t, 0, t, WORLD_HEIGHT));




        enemies.add(new Mosquito(760, 420, false));








        player = new MockPlayer(500, 60, 40, 72);

        contactCooldown = 0f;
    }





    @Override
    public void render(float delta) {
        handleInput(delta);
        update(delta);
        draw();
    }

    private void handleInput(float delta) {

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT))  dx -= 1;
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) dx += 1;
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP))    dy += 1;
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN))  dy -= 1;
        player.bounds.x += dx * PLAYER_SPEED * delta;
        player.bounds.y += dy * PLAYER_SPEED * delta;


        if (Gdx.input.isKeyJustPressed(Keys.K)) killNearest(true);
        if (Gdx.input.isKeyJustPressed(Keys.L)) killNearest(false);

        if (Gdx.input.isKeyJustPressed(Keys.H)) showDebug = !showDebug;
        if (Gdx.input.isKeyJustPressed(Keys.R)) buildScene();
    }


    private void killNearest(boolean inAir) {
        BaseEnemy nearest = null;
        float best = Float.MAX_VALUE;
        for (BaseEnemy e : enemies) {
            if (e.isDead() || e.getState().name().startsWith("DEAD")) continue;
            float d = Math.abs(e.getHitbox().x - player.bounds.x);
            if (d < best) { best = d; nearest = e; }
        }
        if (nearest == null) return;

        if (inAir) {

            nearest.getHitbox().y += 140f;
        }
        nearest.kill();
    }

    private void update(float delta) {

        if (player.hitFlash > 0f) player.hitFlash -= delta;


        for (BaseEnemy e : enemies) {
            e.update(delta, player.bounds, platforms);
        }







        contactCooldown -= delta;
        if (contactCooldown <= 0f) {
            for (BaseEnemy e : enemies) {
                if (e.getState().name().startsWith("DEAD")) continue;
                if (e.getHitbox().overlaps(player.bounds)) {
                    boolean fromRight = e.getHitbox().x > player.bounds.x;
                    player.takeDamage(1f, fromRight);
                    contactCooldown = CONTACT_INTERVAL;
                    break;
                }
            }
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0.09f, 0.10f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);


        batch.begin();
        for (BaseEnemy e : enemies) e.render(batch);
        batch.end();


        if (showDebug) {
            shapes.begin(ShapeRenderer.ShapeType.Line);


            shapes.setColor(Color.GRAY);
            for (Rectangle p : platforms) shapes.rect(p.x, p.y, p.width, p.height);


            for (BaseEnemy e : enemies) e.renderDebug(shapes);


            shapes.setColor(player.hitFlash > 0f ? Color.RED : Color.GREEN);
            shapes.rect(player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);

            shapes.end();
        }


        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch,
            "WASD/Arrows: move player | K: kill nearest in AIR | L: kill nearest on GROUND | H: debug(" + showDebug + ") | R: reset",
            12, WORLD_HEIGHT - 12);
        font.draw(batch, "Player HP: " + (int) player.health, 12, WORLD_HEIGHT - 32);
        float y = WORLD_HEIGHT - 60;
        for (BaseEnemy e : enemies) {
            font.draw(batch,
                e.getClass().getSimpleName() + " : " + e.getState()
                    + (e.isFacingRight() ? " >" : " <"),
                12, y);
            y -= 18;
        }
        batch.end();
    }





    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    private void disposeEnemies() {
        for (BaseEnemy e : enemies) e.dispose();
    }

    @Override
    public void dispose() {
        disposeEnemies();
        enemies.clear();
        if (batch != null)  batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null)   font.dispose();
    }






    private static final class MockPlayer implements Damageable {
        final Rectangle bounds;
        float health = 10f;
        float hitFlash = 0f;

        MockPlayer(float x, float y, float w, float h) {
            this.bounds = new Rectangle(x, y, w, h);
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public void takeDamage(float amount, boolean fromRight) {
            health -= amount;
            hitFlash = 0.15f;

            bounds.x += fromRight ? -18f : 18f;
            if (health < 0) health = 0;
        }
    }
}
