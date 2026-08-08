package com.Graphic.models;

import com.Graphic.managers.EventBus;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Downward map laser hazard.
 *
 * Each hazard is anchored at a spawn point on the map and fires straight down.
 * The beam collides with non-deadly solid blocks (same behavior as deadly blocks
 * that stop on solid geometry) and produces a thin damage rectangle while active.
 *
 * State machine:
 *   IDLE    -> sleeping, playing nothing
 *   WINDUP  -> visible charge orb growing, audible prepare cue
 *   FIRING  -> active beam, damage band, loop cue
 *
 * Each hazard maintains its own randomized off/on durations so a cluster of
 * lasers never resolves into a single metronome. A short biased-off window plus
 * longer active windows creates the challenging, unpredictable rhythm the
 * designer wants without leaving the player any safe fixed gap.
 *
 * One damage application per activation cycle — re-arming only happens after a
 * full IDLE/WINDUP cycle, so the laser can never apply per-frame contact damage.
 */
public class LaserHazard {

    public enum State { IDLE, WINDUP, FIRING }


    private static final float OFF_MIN = 0.45f;
    private static final float OFF_MAX = 1.35f;

    private static final float CHARGE_MIN = 0.55f;
    private static final float CHARGE_MAX = 1.05f;

    private static final float ON_MIN = 1.4f;
    private static final float ON_MAX = 2.3f;

    private static final float ORB_SIZE = 56f;
    private static final float ORB_PULSE_DEPTH = 0.08f;
    private static final float ORB_PULSE_SPEED = 7.5f;

    // Beam art is reused from the existing Crystal Guardian pipeline: a thin
    // slice of a horizontal muzzle-burst sprite, stretched into a beam. For a
    // downward laser the SLICE itself becomes the BEAM THICKNESS and the LENGTH
    // becomes the height of the drawn quad. Visual height is fixed (matching
    // CrystalGuardian's LASER_VISUAL_HEIGHT) so the beam always reads the same
    // on screen regardless of how far it travels.
    private static final float LASER_CORE_SLICE_START = 160f / 228f;
    private static final float LASER_CORE_SLICE_END   = 184f / 228f;
    private static final float LASER_VISUAL_HEIGHT = 28f;
    private static final float LASER_MAX_LENGTH    = 4000f;

    private final Rectangle bounds = new Rectangle();
    private final Rectangle damageBand = new Rectangle();
    private float beamEndY;

    private final float spawnX;
    private final float spawnY;
    private final float beamWidth;

    private State state = State.IDLE;
    private float stateTimer = 0f;
    private float phaseDuration = rollDuration(OFF_MIN, OFF_MAX);
    private boolean damageAppliedThisCycle = false;

    private Animation<TextureAtlas.AtlasRegion> chargeStartAnim;
    private Animation<TextureAtlas.AtlasRegion> beamAnim;
    private Animation<TextureRegion> chargedOrbAnim;
    private float beamStateTime = 0f;

    private static TextureAtlas sharedAtlas;
    private static Texture sharedOrbTexture;
    private static Animation<TextureAtlas.AtlasRegion> sharedChargeAnim;
    private static Animation<TextureAtlas.AtlasRegion> sharedBeamAnim;
    private static Animation<TextureRegion> sharedOrbAnim;
    // Overshoot the visible/collidable beam past its logical start/end so it
// reads as flush against the emitter and the floor, independent of sprite
// padding. Expressed as a fraction of beam thickness since that scales
// naturally with the art rather than being an arbitrary pixel constant.
    private static final float OVERSHOOT_FRACTION = 3f;
    private static final float BEAM_OVERSHOOT = LASER_VISUAL_HEIGHT * OVERSHOOT_FRACTION;
    public LaserHazard(float spawnX, float spawnY, float beamWidth, float beamHeight) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.beamWidth = beamWidth;

        bounds.set(spawnX - beamWidth / 2f, spawnY, beamWidth, beamHeight);
        damageBand.set(bounds);

        loadSharedAssets();
        this.chargeStartAnim = sharedChargeAnim;
        this.beamAnim = sharedBeamAnim;
        this.chargedOrbAnim = sharedOrbAnim;

        // Stagger initial phase so all hazards don't fire on the first tick.
        phaseDuration = rollDuration(0f, 1.6f);
    }

    private static synchronized void loadSharedAssets() {
        if (sharedAtlas != null) return;

        sharedAtlas = new TextureAtlas(Gdx.files.internal("enemies/Laser/Laser.atlas"));

        Array<TextureAtlas.AtlasRegion> growFrames = new Array<>();
        Array<TextureAtlas.AtlasRegion> beamFrames = new Array<>();

        for (int i = 0; i <= 7; i++) {
            TextureAtlas.AtlasRegion r = sharedAtlas.findRegion(
                String.format("Laser Beam Cln_beam_shot_effect%04d", i));
            if (r != null) growFrames.add(r);
        }
        for (int i = 4; i <= 7; i++) {
            TextureAtlas.AtlasRegion r = sharedAtlas.findRegion(
                String.format("Laser Beam Cln_beam_shot_effect%04d", i));
            if (r != null) beamFrames.add(r);
        }

        sharedChargeAnim = growFrames.size > 0
            ? new Animation<>(0.09f, growFrames, Animation.PlayMode.NORMAL)
            : null;
        sharedBeamAnim = beamFrames.size > 0
            ? new Animation<>(0.08f, beamFrames, Animation.PlayMode.LOOP)
            : null;

        sharedOrbTexture = new Texture(Gdx.files.internal("enemies/Crystalized/ChargedOrbClean.png"));
        sharedOrbTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] orbGrid = TextureRegion.split(sharedOrbTexture, 81, 81);
        Array<TextureRegion> orbFrames = new Array<>();
        int[] orbOrder = {0, 1, 2, 3, 2, 1};
        for (int frameIndex : orbOrder) orbFrames.add(orbGrid[0][frameIndex]);
        sharedOrbAnim = new Animation<>(0.08f, orbFrames, Animation.PlayMode.LOOP);
    }

    public static void disposeSharedAssets() {
        if (sharedAtlas != null) { sharedAtlas.dispose(); sharedAtlas = null; }
        if (sharedOrbTexture != null) { sharedOrbTexture.dispose(); sharedOrbTexture = null; }
        sharedChargeAnim = null;
        sharedBeamAnim = null;
        sharedOrbAnim = null;
    }

    public void update(float delta, Array<SolidBlock> solidBlocks) {
        stateTimer += delta;

        switch (state) {
            case IDLE:
                if (stateTimer >= phaseDuration) {
                    enterState(State.WINDUP, rollDuration(CHARGE_MIN, CHARGE_MAX));
                    EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_CHARGE, this);
                }
                break;

            case WINDUP:
                if (stateTimer >= phaseDuration) {
                    enterState(State.FIRING, rollDuration(ON_MIN, ON_MAX));
                    EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_START, this);
                }
                break;

            case FIRING: {
                beamStateTime += delta;
                float rawEndY = computeBeamEnd(solidBlocks);
                float clampedEndY = Math.max(rawEndY, spawnY - LASER_MAX_LENGTH);
                beamEndY = clampedEndY;

                // Extend both ends by the overshoot margin: start pushes UP past the
                // emitter, end pushes DOWN past the block surface.
                float bandTop    = spawnY + BEAM_OVERSHOOT;
                float bandBottom = clampedEndY - BEAM_OVERSHOOT;

                damageBand.x = spawnX - beamWidth / 2f;
                damageBand.y = bandBottom;
                damageBand.width = beamWidth;
                damageBand.height = Math.max(0f, bandTop - bandBottom);

                if (stateTimer >= phaseDuration) {
                    EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_STOP, this);
                    enterState(State.IDLE, rollDuration(OFF_MIN, OFF_MAX));
                }
                break;
            }
        }
    }

    public boolean consumeDamageOnPlayer(Rectangle playerBounds) {
        if (state != State.FIRING) return false;
        if (damageAppliedThisCycle) return false;
        if (!damageBand.overlaps(playerBounds)) return false;
        damageAppliedThisCycle = true;
        return true;
    }
    private float computeBeamEnd(Array<SolidBlock> solidBlocks) {
        // LibGDX world/batch space is Y-UP: "down" on screen means DECREASING y.
        // A blocking block is one whose TOP edge (r.y + r.height) sits at or
        // below spawnY. The beam stops at the highest such top edge below the
        // spawn point (the nearest blocker underneath), i.e. the LARGEST
        // (r.y + r.height) that is still <= spawnY. Sentinel NEGATIVE_INFINITY
        // means "no blocker found"; the caller caps with LASER_MAX_LENGTH.
        float bestEnd = Float.NEGATIVE_INFINITY;
        for (SolidBlock b : solidBlocks) {
            if (b.isDeadly) continue;
            Rectangle r = b.bounds;

            boolean spansHorizontally =
                spawnX + beamWidth / 2f > r.x &&
                    spawnX - beamWidth / 2f < r.x + r.width;
            if (!spansHorizontally) continue;

            float blockTop = r.y + r.height;

            // Block wholly above the spawn point never blocks a downward beam.
            if (blockTop >= spawnY) continue;

            // Pick the highest top edge below spawnY (closest blocker).
            if (blockTop <= bestEnd) continue;
            bestEnd = blockTop;
        }
        return bestEnd;
    }
    private void enterState(State next, float duration) {
        state = next;
        stateTimer = 0f;
        phaseDuration = duration;
        if (next == State.FIRING) {
            beamStateTime = 0f;
            damageAppliedThisCycle = false;
        }
    }

    private float rollDuration(float min, float max) {
        return MathUtils.random(min, max);
    }

    public Rectangle getBounds() { return bounds; }
    public Rectangle getDamageBand() { return damageBand; }
    public State getState() { return state; }
    public boolean isActive() { return state == State.FIRING; }
    public float getBeamEndY() { return beamEndY; }

    public void render(SpriteBatch batch) {
        if (state == State.IDLE) return;

        TextureRegion orbFrame = null;
        boolean firing = state == State.FIRING;
        float orbTime = firing ? beamStateTime : stateTimer;

        if (state == State.WINDUP) {
            if (chargeStartAnim != null) orbFrame = chargeStartAnim.getKeyFrame(stateTimer);
        } else if (state == State.FIRING) {
            if (chargedOrbAnim != null) orbFrame = chargedOrbAnim.getKeyFrame(orbTime);
        }

        // ── Beam — only after windup ──────────────────────────────────────────
        // In y-down screen pixels: the beam rect starts at spawnY and extends
        // DOWNWARD by `length` pixels. The texture slice is a thin HORIZONTAL
        // sliver of the muzzle-burst sprite; we draw it NARROW (LASER_VISUAL_HEIGHT)
        // and TALL (length) so the sliver becomes a thin vertical beam. Beam
        // is drawn before the orb so the orb can mask the seam at the muzzle.
        if (firing && beamAnim != null) {
            TextureAtlas.AtlasRegion frame = beamAnim.getKeyFrame(beamStateTime);
            float drawTop    = spawnY + BEAM_OVERSHOOT;
            float drawBottom = beamEndY - BEAM_OVERSHOOT;
            float length = drawTop - drawBottom;
            if (length > 0f) {
                float uSpan = frame.getU2() - frame.getU();
                float u1 = frame.getU() + uSpan * LASER_CORE_SLICE_START;
                float u2 = frame.getU() + uSpan * LASER_CORE_SLICE_END;

                float drawX = spawnX - LASER_VISUAL_HEIGHT / 2f;
                float drawY = drawBottom;
                float drawW = LASER_VISUAL_HEIGHT;
                float drawH = length;
                batch.draw(frame.getTexture(),
                    drawX, drawY, drawW, drawH,
                    u1, frame.getV2(), u2, frame.getV());
            }
        }

        if (orbFrame != null) {
            float pulse = firing
                ? 1f + ORB_PULSE_DEPTH * (float) Math.sin(beamStateTime * ORB_PULSE_SPEED)
                : 1f;
            float size = ORB_SIZE * pulse;
            batch.draw(orbFrame, spawnX - size / 2f, spawnY - size / 2f, size, size);
        }
    }

    public void drawDebugHitbox(ShapeRenderer shapes) {
        Color c;
        switch (state) {
            case WINDUP: c = Color.YELLOW; break;
            case FIRING: c = Color.RED; break;
            default:     c = Color.GRAY;  break;
        }
        shapes.setColor(c);
        shapes.rect(spawnX - beamWidth / 2f, spawnY, beamWidth, 4f);

        if (state == State.FIRING) {
            shapes.setColor(Color.SCARLET);
            Rectangle r = damageBand;
            shapes.rect(r.x, r.y, r.width, r.height);
        }
    }
}
