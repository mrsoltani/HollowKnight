package com.Graphic.models.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-enemy animation encapsulation.
 *
 * Deliberately NOT a centralised "God class": every concrete enemy owns its own
 * private {@code AnimationComponent} instance and registers only the clips it
 * needs. This component is responsible for three things and nothing else:
 *
 *   1. Loading a horizontal spritesheet strip and slicing it into frames
 *      (via {@link TextureRegion#split}).
 *   2. Building and storing one {@link Animation} per {@link EnemyState}.
 *   3. Tracking {@code stateTime} and returning the correct frame each render,
 *      with horizontal flipping for facing direction.
 *
 * It also owns every {@link Texture} it loads and disposes them all in
 * {@link #dispose()}, so an enemy simply forwards its own {@code dispose()} here
 * and never leaks GPU memory.
 *
 * All reference sheets are a single horizontal row: sheetHeight == frameHeight
 * and sheetWidth == frameWidth * frameCount, so a 1-row {@code split} is exact.
 */
public class AnimationComponent implements Disposable {

    /** One animation clip per registered state. */
    private final Map<EnemyState, Animation<TextureRegion>> animations =
        new EnumMap<>(EnemyState.class);

    /** Every texture we loaded, tracked purely so we can dispose them. */
    private final Array<Texture> ownedTextures = new Array<>();

    /**
     * Advances every frame relative to this. Reset to 0 by the owning enemy
     * whenever it changes state, so NORMAL (one-shot) clips play from frame 0.
     */
    private float stateTime = 0f;

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Loads {@code assetPath}, slices it into {@code frameWidth x frameHeight}
     * frames (single row, left-to-right) and stores the resulting animation
     * under {@code state}.
     *
     * @param state         the state this clip is played for
     * @param assetPath     internal path to the spritesheet strip
     * @param frameWidth    width of a single frame in px
     * @param frameHeight   height of a single frame in px
     * @param frameDuration seconds per frame
     * @param mode          LOOP for continuous states, NORMAL for one-shots
     */
    public void register(EnemyState state, String assetPath,
                         int frameWidth, int frameHeight,
                         float frameDuration, Animation.PlayMode mode) {

        Texture sheet = new Texture(Gdx.files.internal(assetPath));
        ownedTextures.add(sheet); // remember it so dispose() can free it

        // Slice the strip. split() returns [rows][cols]; these sheets are 1 row.
        TextureRegion[][] grid = TextureRegion.split(sheet, frameWidth, frameHeight);

        // Flatten row 0 into an ordered frame list.
        int columns = grid[0].length;
        Array<TextureRegion> frames = new Array<>(columns);
        for (int col = 0; col < columns; col++) {
            frames.add(grid[0][col]);
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, mode);
        animations.put(state, animation);
    }

    // =========================================================================
    // Playback
    // =========================================================================

    /** Advance the shared clock. Call once per frame from the enemy's update. */
    public void update(float delta) {
        stateTime += delta;
    }

    /** Restart the current clip from frame 0 (call on every state change). */
    public void resetStateTime() {
        stateTime = 0f;
    }

    public float getStateTime() {
        return stateTime;
    }

    /** True once a NORMAL (non-looping) clip for {@code state} has fully played. */
    public boolean isFinished(EnemyState state) {
        Animation<TextureRegion> anim = animations.get(state);
        return anim != null && anim.isAnimationFinished(stateTime);
    }

    public boolean hasState(EnemyState state) {
        return animations.containsKey(state);
    }

    /**
     * Returns the frame to draw for {@code state} at the current stateTime,
     * flipped to match {@code facingRight}.
     *
     * NOTE: the returned region is the shared frame instance owned by the
     * Animation, so we correct its flip flag every call rather than assuming a
     * fixed orientation (frames are authored facing left by default here, but
     * we normalise defensively so callers never fight stale flip state).
     *
     * @param state        state whose clip to sample; falls back to any
     *                     registered clip if the requested one is missing
     * @param facingRight  desired horizontal facing
     */
    public TextureRegion getFrame(EnemyState state, boolean facingRight) {
        Animation<TextureRegion> anim = animations.get(state);
        if (anim == null) {
            // Defensive fallback: never NPE mid-render if a state was not
            // registered. Grab whatever clip exists so something draws.
            for (Animation<TextureRegion> any : animations.values()) {
                anim = any;
                break;
            }
            if (anim == null) return null; // nothing registered at all
        }

        TextureRegion frame = anim.getKeyFrame(stateTime);

        // Source art faces LEFT. Flip when we want to face right.
        boolean shouldFlip = facingRight;
        if (frame.isFlipX() != shouldFlip) {
            frame.flip(true, false);
        }
        return frame;
    }

    // =========================================================================
    // Cleanup
    // =========================================================================

    /**
     * Disposes every texture this component loaded. Safe to call more than once
     * (the array is cleared afterwards so a second call is a no-op).
     */
    @Override
    public void dispose() {
        for (Texture texture : ownedTextures) {
            texture.dispose();
        }
        ownedTextures.clear();
        animations.clear();
    }
}
