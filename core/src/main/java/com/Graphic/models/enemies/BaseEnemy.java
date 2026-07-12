package com.Graphic.models.enemies;

import com.Graphic.managers.EventBus;
import com.Graphic.models.spells.Damageable;
import com.Graphic.utils.EffectSpawnData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Abstract base for every enemy. Owns everything that is identical across the
 * four brains so each concrete enemy only has to describe its own behaviour:
 *
 *   - the physics body ({@link #bounds}) which doubles as the damage hitbox
 *   - gravity + ground/wall resolution against world {@link Rectangle}s
 *   - "wall ahead" and "cliff edge ahead" sensors (the classic patrol probes)
 *   - the shared DEAD_AIR -> DEAD_LAND death machine and its physics
 *   - contact-damage dispatch to any {@link Damageable} target
 *   - default sprite rendering (centered on the body, flipped by facing)
 *
 * Implements {@link Damageable} so the existing Fireball / Scream spell system
 * can hurt enemies with zero extra plumbing.
 *
 * Subclasses implement {@link #updateAI} (their brain, run only while alive)
 * and {@link #getContactDamage} (how hard they hit the player).
 */
public abstract class BaseEnemy implements EnemyAI, Damageable {

    // ── Shared physics tuning ────────────────────────────────────────────────
    protected static final float GRAVITY          = -1400f; // px/s², applied to affected enemies
    protected static final float MAX_FALL_SPEED   = -1200f; // terminal velocity clamp
    protected static final float CORPSE_FALL_NUDGE = 0f;    // corpses use gravity as-is

    // ── Core body / motion ───────────────────────────────────────────────────
    /** The hitbox AND the physics body. Collisions with this damage the player. */
    protected final Rectangle bounds;
    protected final Vector2 velocity = new Vector2();
    protected boolean facingRight;
    protected boolean onGround = false;

    // ── Health / state ───────────────────────────────────────────────────────
    protected float health;
    protected EnemyState state;

    /**
     * Whether gravity acts on this enemy while ALIVE. Ground enemies (Crawler,
     * Husk, the ground-charging Guardian) = true; pure flyers (Mosquito) = false.
     * Note: gravity is ALWAYS applied once dead and airborne, regardless of this
     * flag, so a killed flyer falls out of the sky and then lands.
     */
    protected boolean affectedByGravityWhileAlive;

    // ── Animation (encapsulated per enemy) ───────────────────────────────────
    protected final AnimationComponent anim = new AnimationComponent();

    // ── Sprite draw box (decoupled from the hitbox) ──────────────────────────
    // The hitbox (bounds) is the physics/damage body and can be made SMALLER
    // than the artwork for fair collisions. These describe how big to DRAW the
    // sprite and where to place it relative to the hitbox, so you can tune the
    // hitbox freely without the sprite moving.
    protected float frameWidth;    // sprite draw width  (px)
    protected float frameHeight;   // sprite draw height (px)
    protected float spriteOffsetX; // sprite x offset from the hitbox centre-align
    protected float spriteOffsetY; // sprite y offset from the hitbox bottom

    /** Set true once the corpse has settled on the ground (DEAD_LAND done). */
    private boolean fullyDead = false;

    // Tiny window that only guards against a double-apply on the same frame. The
    // real "one hit per nail swing" rule lives in GameScreen (per-swing set).
    private static final float HIT_INVULN = 0.28f;
    private float hitInvulnTimer = 0f;

    // Knockback: when hit, the enemy is shoved and briefly stunned (its brain is
    // suspended so the shove reads clearly) before AI resumes.
    private static final float KNOCKBACK_X    = 1000f;
    private static final float KNOCKBACK_Y    = 160f;
    private static final float KNOCKBACK_TIME = 0.16f;
    private float knockbackTimer = 0f;

    protected BaseEnemy(float x, float y, float width, float height,
                        boolean facingRight, float health,
                        boolean affectedByGravityWhileAlive) {
        this.bounds = new Rectangle(x, y, width, height);
        this.facingRight = facingRight;
        this.health = health;
        this.affectedByGravityWhileAlive = affectedByGravityWhileAlive;
        this.state = EnemyState.IDLE; // subclass ctor overrides via changeState()

        // By default the sprite is drawn at the hitbox size (backwards-compatible
        // with enemies whose hitbox == frame size). Enemies with a tuned hitbox
        // call setSpriteBox(...) to draw the full-size art over a smaller body.
        this.frameWidth = width;
        this.frameHeight = height;
        this.spriteOffsetX = 0f;
        this.spriteOffsetY = 0f;
    }

    /**
     * Configure how the sprite is drawn independently of the hitbox.
     *
     * @param w        sprite draw width in px (usually the real frame width)
     * @param h        sprite draw height in px (usually the real frame height)
     * @param offsetX  horizontal nudge from centre-aligned-on-hitbox
     * @param offsetY  vertical nudge from the hitbox's bottom edge
     */
    protected void setSpriteBox(float w, float h, float offsetX, float offsetY) {
        this.frameWidth = w;
        this.frameHeight = h;
        this.spriteOffsetX = offsetX;
        this.spriteOffsetY = offsetY;
    }

    // =========================================================================
    // Main update — template method
    // =========================================================================

    @Override
    public final void update(float delta, Rectangle target, Array<Rectangle> platforms) {
        anim.update(delta);
        if (hitInvulnTimer > 0f) hitInvulnTimer -= delta;

        if (isDying()) {
            // While dead we ignore the brain entirely and just run corpse
            // physics + the DEAD_AIR -> DEAD_LAND transition.
            updateDeath(delta, platforms);
            return;
        }

        // Knockback stun: keep the shove velocity, suspend the brain, still let
        // the body hurt the player on contact.
        if (knockbackTimer > 0f) {
            knockbackTimer -= delta;
            if (affectedByGravityWhileAlive) applyGravity(delta);
            moveAndCollide(delta, platforms);
            dispatchContactDamage(target);
            return;
        }

        // Alive: let the concrete brain drive velocity / state...
        updateAI(delta, target, platforms);

        // ...then apply gravity + world collision if this enemy is grounded.
        if (affectedByGravityWhileAlive) {
            applyGravity(delta);
        }
        moveAndCollide(delta, platforms);

        // Finally, if we're touching the player, hurt them.
        dispatchContactDamage(target);
    }

    // =========================================================================
    // Abstract hooks
    // =========================================================================

    /** The enemy's brain. Runs every frame WHILE ALIVE. Set velocity + state. */
    protected abstract void updateAI(float delta, Rectangle target, Array<Rectangle> platforms);

    /** Damage dealt to the player on hitbox contact. */
    protected abstract float getContactDamage();

    /**
     * Stable identifier for this enemy type (e.g. "mosquito"). Sent as the
     * ENEMY_KILLED payload so the EventBus / AchievementManager can react.
     */
    protected abstract String enemyTypeName();

    // =========================================================================
    // Death machine (shared by all enemies)
    // =========================================================================

    /**
     * Kill the enemy immediately, choosing the correct death animation from the
     * current physics situation:
     *   - airborne  -> DEAD_AIR (falls, then flips to DEAD_LAND on landing)
     *   - grounded  -> DEAD_LAND directly
     */
    public void kill() {
        if (isDying()) return; // already dead, ignore
        health = 0;
        velocity.x = 0; // corpses don't keep walking; vertical handled by gravity

        // Notify the game (death SFX/VFX + achievements). The payload is the
        // type string the AchievementManager's ENEMY_KILLED handler expects.
        EventBus.emit(EventBus.Event.ENEMY_KILLED, enemyTypeName());

        if (onGround) {
            changeState(EnemyState.DEAD_LAND);
        } else {
            changeState(EnemyState.DEAD_AIR);
        }
    }

    /**
     * {@link Damageable} entry point — lets the existing spell system damage
     * enemies. When health hits zero we route into the death machine.
     *
     * @param amount     damage to apply
     * @param fromRight  true if the hit came from the enemy's right side
     *                   (used for knockback / could flip facing; kept simple).
     */
    @Override
    public void takeDamage(float amount, boolean fromRight) {
        if (isDying()) return;
        if (hitInvulnTimer > 0f) return; // still in post-hit i-frames — ignore
        health -= amount;
        hitInvulnTimer = HIT_INVULN;

        // Fire a hit event at the enemy's centre so the EventBus can play the
        // nail SFX and a hit-spark VFX. facingRight = direction the hit pushes.
        EventBus.emit(EventBus.Event.ENEMY_HIT,
            new EffectSpawnData(bounds.x + bounds.width / 2f,
                                bounds.y + bounds.height / 2f, !fromRight));

        if (health <= 0) {
            kill();
            return;
        }

        // Survived the hit -> shove away from the source and briefly stun.
        knockbackTimer = KNOCKBACK_TIME;
        velocity.x = fromRight ? -KNOCKBACK_X : KNOCKBACK_X;
        velocity.y = KNOCKBACK_Y;
        onGround = false;
    }

    /** Corpse physics + the airborne-corpse-lands transition. */
    private void updateDeath(float delta, Array<Rectangle> platforms) {
        if (state == EnemyState.DEAD_AIR) {
            // Falling corpse: always subject to gravity regardless of the
            // alive-gravity flag (a shot-down Mosquito must drop).
            applyGravity(delta);
            moveAndCollide(delta, platforms);

            // The moment we settle on the ground, swap to the landed clip.
            if (onGround) {
                changeState(EnemyState.DEAD_LAND);
            }
        } else if (state == EnemyState.DEAD_LAND) {
            // Landed corpse: hold still and let the one-shot clip play out. Once
            // finished we flag fullyDead — but the body STAYS in the world and
            // keeps rendering its final frame (DEAD_LAND is PlayMode.NORMAL, so
            // getKeyFrame clamps to the last frame). The owner should NOT remove
            // it; fullyDead just means "settled corpse, inert".
            velocity.set(0, 0);
            if (anim.isFinished(EnemyState.DEAD_LAND)) {
                fullyDead = true;
            }
        }
    }

    protected boolean isDying() {
        return state == EnemyState.DEAD_AIR || state == EnemyState.DEAD_LAND;
    }

    @Override
    public boolean isDead() {
        return fullyDead;
    }

    /** True while the enemy is alive (not in a death state). */
    public boolean isAlive() {
        return !isDying();
    }

    // =========================================================================
    // Physics helpers (shared)
    // =========================================================================

    protected void applyGravity(float delta) {
        velocity.y += GRAVITY * delta;
        if (velocity.y < MAX_FALL_SPEED) velocity.y = MAX_FALL_SPEED;
    }

    /**
     * Integrates velocity into {@link #bounds} and resolves collision against
     * world geometry axis-by-axis. Sets {@link #onGround} when we land on top
     * of a platform. Horizontal blocking is reported to subclasses via
     * {@link #hitWallThisFrame}.
     */
    private boolean hitWallThisFrame = false;    // horizontal block only
    private boolean hitSolidThisFrame = false;   // ANY axis block (for diagonal moves)

    protected void moveAndCollide(float delta, Array<Rectangle> platforms) {
        hitWallThisFrame = false;
        hitSolidThisFrame = false;

        // ── Horizontal ──
        bounds.x += velocity.x * delta;
        for (Rectangle p : platforms) {
            if (!bounds.overlaps(p)) continue;
            if (velocity.x > 0) {
                bounds.x = p.x - bounds.width; // hit left face of platform
                hitWallThisFrame = true;
                hitSolidThisFrame = true;
            } else if (velocity.x < 0) {
                bounds.x = p.x + p.width;       // hit right face of platform
                hitWallThisFrame = true;
                hitSolidThisFrame = true;
            }
        }

        // ── Vertical ──
        onGround = false;
        bounds.y += velocity.y * delta;
        for (Rectangle p : platforms) {
            if (!bounds.overlaps(p)) continue;
            if (velocity.y <= 0) {
                bounds.y = p.y + p.height;      // landed on top
                velocity.y = 0;
                onGround = true;
                hitSolidThisFrame = true;
            } else {
                bounds.y = p.y - bounds.height; // bonked head on ceiling
                velocity.y = 0;
                hitSolidThisFrame = true;
            }
        }
    }

    /** True if horizontal movement was blocked by geometry this frame. */
    protected boolean isBlockedByWall() {
        return hitWallThisFrame;
    }

    /**
     * True if movement on ANY axis was blocked by solid geometry this frame.
     * Use this for diagonal motion (e.g. the Mosquito swoop) where a hit could
     * come from a wall, floor or ceiling.
     */
    protected boolean isBlockedBySolid() {
        return hitSolidThisFrame;
    }

    /**
     * Classic patrol probe: is there NO ground under the point just ahead of
     * the enemy's leading foot? Used to stop/turn at cliff edges.
     *
     * @param lookAhead how far past the leading edge to sample, in px
     */
    protected boolean isCliffAhead(Array<Rectangle> platforms, float lookAhead) {
        // Sample a point a little beyond the leading edge and just below feet.
        float footX = facingRight ? bounds.x + bounds.width + lookAhead
                                  : bounds.x - lookAhead;
        float footY = bounds.y - 2f; // just under the body

        for (Rectangle p : platforms) {
            if (p.contains(footX, footY)) {
                return false; // ground present ahead -> not a cliff
            }
        }
        return true; // nothing under the probe -> cliff edge
    }

    /**
     * Is a wall directly ahead within {@code lookAhead} px? Complements
     * {@link #isBlockedByWall()} (which is post-move) with a predictive probe
     * so patrols can turn a frame early and never visually clip.
     */
    protected boolean isWallAhead(Array<Rectangle> platforms, float lookAhead) {
        float probeX = facingRight ? bounds.x + bounds.width
                                   : bounds.x - lookAhead;
        Rectangle probe = new Rectangle(probeX, bounds.y + 2f, lookAhead, bounds.height - 4f);
        for (Rectangle p : platforms) {
            if (probe.overlaps(p)) return true;
        }
        return false;
    }

    // =========================================================================
    // Contact damage
    // =========================================================================

    /** Hurt the target if our (living) hitbox overlaps it. */
    protected void dispatchContactDamage(Rectangle target) {
        if (target == null) return;
        if (bounds.overlaps(target)) {
            // The test screen's mock player IS a Damageable; the real player
            // will be too. We report damage via a side channel the screen reads
            // (kept as an overridable hook so different integrations can choose
            // how contact resolves). Default: no-op here; the screen polls
            // overlap. Subclasses / integrators may override.
            onContact(target);
        }
    }

    /**
     * Hook fired when the hitbox overlaps the target this frame. Default is a
     * no-op; the test screen reads {@link #getHitbox()} overlap + a helper to
     * apply damage so it can show hit feedback. Overridable for real gameplay.
     */
    protected void onContact(Rectangle target) {
        // Intentionally empty — see EnemyTestScreen for how contact is consumed.
    }

    // =========================================================================
    // State transitions
    // =========================================================================

    /** Change state and restart the animation clock so one-shots replay. */
    protected void changeState(EnemyState newState) {
        if (this.state == newState) return;
        this.state = newState;
        anim.resetStateTime();
    }

    protected void faceRight(boolean right) {
        this.facingRight = right;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    /**
     * Default sprite render: draw the sprite at the configured sprite-box size,
     * centred horizontally on the hitbox and offset by the tuned sprite offsets.
     * The sprite box is decoupled from the hitbox so a small damage body can sit
     * under full-size art (see {@link #setSpriteBox}).
     */
    @Override
    public void render(SpriteBatch batch) {
        TextureRegion frame = anim.getFrame(state, facingRight);
        if (frame == null) return;

        // Centre the sprite horizontally on the hitbox, then apply offsets.
        float drawX = bounds.x + bounds.width / 2f - frameWidth / 2f + spriteOffsetX;
        float drawY = bounds.y + spriteOffsetY; // relative to the hitbox bottom
        batch.draw(frame, drawX, drawY, frameWidth, frameHeight);
    }

    /** Draw the damage hitbox (red). Subclasses add vision boxes on top. */
    @Override
    public void renderDebug(ShapeRenderer shapes) {
        shapes.setColor(isDying() ? Color.GRAY : Color.RED);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    @Override
    public Rectangle getHitbox() {
        return bounds;
    }

    /** {@link Damageable} — the spell system reads this to test overlap. */
    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    public EnemyState getState() {
        return state;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    @Override
    public void dispose() {
        anim.dispose();
    }
}
