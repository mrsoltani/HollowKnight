package com.Graphic.models;

import com.Graphic.models.enums.PlatformDirection;
import com.Graphic.utils.PlatformSpawnData;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
public class RotatablePlatform {

    private static final float ROTATE_SPEED_DEG_PER_SEC = 540f;

    // ---- Vertical axis (UP / DOWN) slab config ----
    // Healthy (safe-to-stand) slab
    private static final float V_HEALTHY_WIDTH   = 200f;  // 0 = use full bounds.width; set explicitly to override
    private static final float V_HEALTHY_HEIGHT  = 46f;
    private static final float V_HEALTHY_Y_SHIFT = 30f;  // offset of slab center from platform center, along y
    // Deadly slab
    private static final float V_DEADLY_WIDTH    = 200f;
    private static final float V_DEADLY_HEIGHT   = 60f;
    private static final float V_DEADLY_Y_SHIFT  = 20f;

    // ---- Horizontal axis (LEFT / RIGHT) slab config ----
    // Healthy (safe-to-stand) slab
    private static final float H_HEALTHY_WIDTH   = 46f;
    private static final float H_HEALTHY_HEIGHT  = 200f;  // 0 = use full bounds.height
    private static final float H_HEALTHY_X_SHIFT =-50f;
    // Deadly slab
    private static final float H_DEADLY_WIDTH    = 80f;
    private static final float H_DEADLY_HEIGHT   = 170f;
    private static final float H_DEADLY_X_SHIFT  = 20f;

    private static final boolean TOP_SOLID_ENABLED = true;
    private static final boolean BOTTOM_DEADLY_ENABLED = true;

    private final String groupId;
    private final float centerX, centerY;
    private final Texture texture;
    private final float width, height;

    private PlatformDirection currentDir = PlatformDirection.UP;
    private float displayAngle = PlatformDirection.UP.angleDeg;
    private float goalAngle = PlatformDirection.UP.angleDeg;
    private boolean rotating = false;

    private final Rectangle bounds;

    private SolidBlock healthySlab;
    private SolidBlock deadlySlab;

    public RotatablePlatform(PlatformSpawnData data, Texture texture) {
        this.groupId = data.groupId;
        this.centerX = data.x;
        this.centerY = data.y;
        this.texture = texture;
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.bounds = new Rectangle(centerX - width / 2f, centerY - height / 2f, width, height);
    }

    public void update(float delta) {
        if (rotating) {
            float diff = shortestAngleDiff(displayAngle, goalAngle);
            float step = ROTATE_SPEED_DEG_PER_SEC * delta;
            if (Math.abs(diff) <= step) {
                displayAngle = normalizeDeg(goalAngle);
                goalAngle = displayAngle; // keep both in the same canonical space
                rotating = false;
            } else {
                displayAngle = normalizeDeg(displayAngle + Math.signum(diff) * step);
            }
        }
        updateSlabPositions();
    }


    public void hit(boolean fromRight) {
        currentDir = fromRight ? currentDir.ccw() : currentDir.cw();
        setGoalAngle(currentDir.angleDeg);
    }

    public void rotateTo(PlatformDirection dir) {
        currentDir = dir;
        setGoalAngle(dir.angleDeg);
    }


    private static boolean isOpposite(PlatformDirection a, PlatformDirection b) {
        return (a == PlatformDirection.UP && b == PlatformDirection.DOWN)
            || (a == PlatformDirection.DOWN && b == PlatformDirection.UP)
            || (a == PlatformDirection.LEFT && b == PlatformDirection.RIGHT)
            || (a == PlatformDirection.RIGHT && b == PlatformDirection.LEFT);
    }


    private void setGoalAngle(float target) {
        float diff = shortestAngleDiff(displayAngle, target);
        goalAngle = normalizeDeg(displayAngle + diff);
        rotating = true;
    }

    private static float shortestAngleDiff(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff < -180f) diff += 360f;
        if (diff > 180f) diff -= 360f;
        return diff;
    }


    public void resetToUp() {
        currentDir = PlatformDirection.UP;
        displayAngle = goalAngle = PlatformDirection.UP.angleDeg;
        rotating = false;
    }

    public boolean matches(PlatformDirection target) { return currentDir == target; }

    public void render(SpriteBatch batch) {
        batch.draw(texture,
            centerX - width / 2f, centerY - height / 2f,
            width / 2f, height / 2f,
            width, height,
            1f, 1f,
            displayAngle,
            0, 0, texture.getWidth(), texture.getHeight(),
            false, false);
    }

    public String getGroupId() { return groupId; }
    public Rectangle getBounds() { return bounds; }
    public PlatformDirection getCurrentDirection() { return currentDir; }
    public boolean isRotating() { return rotating; }

    public Array<SolidBlock> buildCollisionSlabs() {
        Array<SolidBlock> slabs = new Array<>();

        if (TOP_SOLID_ENABLED) {
            healthySlab = new SolidBlock(0, 0, 0, 0, false, false);
            slabs.add(healthySlab);
        }
        if (BOTTOM_DEADLY_ENABLED) {
            deadlySlab = new SolidBlock(0, 0, 0, 0, true, false);
            slabs.add(deadlySlab);
        }

        updateSlabPositions();
        return slabs;
    }

    /** Repositions the healthy/deadly slabs for currentDir, using the
     *  per-axis constants above so each orientation can be tuned
     *  independently of a simple width/height swap. */
    private void updateSlabPositions() {
        if (healthySlab == null && deadlySlab == null) return;

        switch (currentDir) {
            case UP:
                placeVertical(healthySlab, V_HEALTHY_WIDTH, V_HEALTHY_HEIGHT,  V_HEALTHY_Y_SHIFT);
                placeVertical(deadlySlab,  V_DEADLY_WIDTH,  V_DEADLY_HEIGHT,  -V_DEADLY_Y_SHIFT);
                break;

            case DOWN:
                placeVertical(healthySlab, V_HEALTHY_WIDTH, V_HEALTHY_HEIGHT, -V_HEALTHY_Y_SHIFT);
                placeVertical(deadlySlab,  V_DEADLY_WIDTH,  V_DEADLY_HEIGHT,   V_DEADLY_Y_SHIFT);
                break;

            case RIGHT:
                placeHorizontal(healthySlab, H_HEALTHY_WIDTH, H_HEALTHY_HEIGHT, -H_HEALTHY_X_SHIFT);
                placeHorizontal(deadlySlab,  H_DEADLY_WIDTH,  H_DEADLY_HEIGHT,   H_DEADLY_X_SHIFT);
                break;

            case LEFT:
                placeHorizontal(healthySlab, H_HEALTHY_WIDTH, H_HEALTHY_HEIGHT,  H_HEALTHY_X_SHIFT);
                placeHorizontal(deadlySlab,  H_DEADLY_WIDTH,  H_DEADLY_HEIGHT,  -H_DEADLY_X_SHIFT);
                break;
        }
    }

    /** width/height of 0 falls back to bounds.width/bounds.height respectively.
     *  yShift moves the slab's center up(+)/down(-) from the platform center. */
    private void placeVertical(SolidBlock slab, float w, float h, float yShift) {
        if (slab == null) return;
        float slabW = w > 0f ? w : bounds.width;
        float slabH = h > 0f ? h : bounds.height;
        float x = centerX - slabW / 2f;
        float y = (centerY + yShift) - slabH / 2f;
        slab.bounds.set(x, y, slabW, slabH);
    }

    /** width/height of 0 falls back to bounds.width/bounds.height respectively.
     *  xShift moves the slab's center right(+)/left(-) from the platform center. */
    private void placeHorizontal(SolidBlock slab, float w, float h, float xShift) {
        if (slab == null) return;
        float slabW = w > 0f ? w : bounds.width;
        float slabH = h > 0f ? h : bounds.height;
        float x = (centerX + xShift) - slabW / 2f;
        float y = centerY - slabH / 2f;
        slab.bounds.set(x, y, slabW, slabH);
    }

    private static float normalizeDeg(float a) {
        a %= 360f;
        if (a < 0f) a += 360f;
        return a;
    }
}
