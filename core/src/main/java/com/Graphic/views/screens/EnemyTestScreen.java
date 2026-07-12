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

/**
 * Isolated playground for tuning the four enemies' AI, hitboxes and vision
 * boxes — the enemy analogue of {@link SpellTestScreen}.
 *
 * What it shows:
 *   - each enemy's animation drawn via SpriteBatch
 *   - RED   rectangles  : damage hitboxes (also the physics bodies)
 *   - YELLOW rectangles : vision / field-of-view boxes (orange = enraged)
 *   - GREEN rectangle   : the mock player (move it into vision boxes to trigger)
 *   - GRAY  rectangles  : solid platforms / walls
 *
 * Controls:
 *   WASD / Arrows : move the mock player
 *   K             : kill nearest enemy in the AIR   (-> DEAD_AIR, it falls)
 *   L             : kill nearest enemy on the GROUND (-> DEAD_LAND)
 *   H             : toggle debug shapes on/off
 *   R             : reset the whole scene
 *
 * The mock player is a real {@link Damageable} (same interface your spells use),
 * so contact damage flows through exactly the path the real player will.
 */
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

    // Cooldown so a single overlap doesn't drain the mock player instantly.
    private float contactCooldown = 0f;
    private static final float CONTACT_INTERVAL = 0.5f;

    // =========================================================================
    // Lifecycle
    // =========================================================================

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

    /** (Re)builds platforms, enemies and the mock player from scratch. */
    private void buildScene() {
        disposeEnemies();
        enemies.clear();
        platforms.clear();

        // ── World geometry ──────────────────────────────────────────────────
        // MOSQUITO ISOLATION ARENA: a fully enclosed box so the accelerating
        // diagonal swoop always terminates on a solid block (floor, ceiling or
        // a side wall) no matter which way it dives.
        float t = 40f; // wall thickness
        platforms.add(new Rectangle(0, 0, WORLD_WIDTH, 60));                     // floor
        platforms.add(new Rectangle(0, WORLD_HEIGHT - t, WORLD_WIDTH, t));       // ceiling
        platforms.add(new Rectangle(0, 0, t, WORLD_HEIGHT));                     // left wall
        platforms.add(new Rectangle(WORLD_WIDTH - t, 0, t, WORLD_HEIGHT));       // right wall

        // ── Enemies (ISOLATED: Mosquito only) ───────────────────────────────
        // The other three are temporarily disabled so you can tune the Mosquito
        // in isolation. Re-enable them when we move on to the next enemy.
        enemies.add(new Mosquito(760, 420, false));

        // enemies.add(new CrystalCrawler(180, 60, true));
        // enemies.add(new HuskHornhead(360, 60, true));
        // enemies.add(new CrystalGuardian(1040, 100, false));

        // ── Mock player ─────────────────────────────────────────────────────
        // Start it on the floor, roughly under the mosquito, so it's easy to
        // walk into the vision box and trigger a dive.
        player = new MockPlayer(500, 60, 40, 72);

        contactCooldown = 0f;
    }

    // =========================================================================
    // Frame
    // =========================================================================

    @Override
    public void render(float delta) {
        handleInput(delta);
        update(delta);
        draw();
    }

    private void handleInput(float delta) {
        // Move the mock player.
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT))  dx -= 1;
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) dx += 1;
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP))    dy += 1;
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN))  dy -= 1;
        player.bounds.x += dx * PLAYER_SPEED * delta;
        player.bounds.y += dy * PLAYER_SPEED * delta;

        // Kill helpers — target the nearest enemy to the player.
        if (Gdx.input.isKeyJustPressed(Keys.K)) killNearest(true);   // in air
        if (Gdx.input.isKeyJustPressed(Keys.L)) killNearest(false);  // on ground

        if (Gdx.input.isKeyJustPressed(Keys.H)) showDebug = !showDebug;
        if (Gdx.input.isKeyJustPressed(Keys.R)) buildScene();
    }

    /**
     * Force-kill the nearest LIVING enemy. If {@code inAir} we first lift it off
     * the ground so it enters DEAD_AIR and demonstrates the falling->DEAD_LAND
     * transition; otherwise it dies where it stands (DEAD_LAND).
     */
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
            // Nudge it upward so onGround becomes false -> kill() picks DEAD_AIR.
            nearest.getHitbox().y += 140f;
        }
        nearest.kill();
    }

    private void update(float delta) {
        // Fade the player's hit-flash tint.
        if (player.hitFlash > 0f) player.hitFlash -= delta;

        // Tick every enemy against the player's hitbox and the world geometry.
        for (BaseEnemy e : enemies) {
            e.update(delta, player.bounds, platforms);
        }

        // NOTE: we intentionally do NOT remove dead enemies. Once a corpse
        // settles (DEAD_LAND), its body stays on the ground and keeps rendering
        // its final frame. Press R to reset the scene when you want them gone.

        // Contact damage: any LIVING enemy hitbox overlapping the player hurts
        // it, on a shared cooldown so it's readable rather than instant death.
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

        // ── Sprites ──
        batch.begin();
        for (BaseEnemy e : enemies) e.render(batch);
        batch.end();

        // ── Debug shapes ──
        if (showDebug) {
            shapes.begin(ShapeRenderer.ShapeType.Line);

            // Platforms.
            shapes.setColor(Color.GRAY);
            for (Rectangle p : platforms) shapes.rect(p.x, p.y, p.width, p.height);

            // Enemy hitboxes + vision boxes (each enemy draws its own).
            for (BaseEnemy e : enemies) e.renderDebug(shapes);

            // Mock player in green, tinted red briefly when it just took a hit.
            shapes.setColor(player.hitFlash > 0f ? Color.RED : Color.GREEN);
            shapes.rect(player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);

            shapes.end();
        }

        // ── HUD ──
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

    // =========================================================================
    // Resize / teardown
    // =========================================================================

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

    // =========================================================================
    // Mock player — test-only stand-in until the real Player exists.
    // =========================================================================

    /**
     * Minimal movable target that implements the project's {@link Damageable}
     * contract exactly, so enemies (and later spells) treat it like the real
     * player. When {@code Player} lands, delete this and pass the real hitbox.
     */
    private static final class MockPlayer implements Damageable {
        final Rectangle bounds;
        float health = 10f;
        float hitFlash = 0f; // seconds of red-tint remaining after a hit

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
            // Small knockback away from the hit, purely for visual feedback.
            bounds.x += fromRight ? -18f : 18f;
            if (health < 0) health = 0;
        }
    }
}
