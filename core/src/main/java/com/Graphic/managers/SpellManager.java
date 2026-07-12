package com.Graphic.managers;

import com.Graphic.models.SolidBlock;
import com.Graphic.models.spells.Damageable;
import com.Graphic.models.spells.Fireball;
import com.Graphic.models.spells.Scream;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;


public class SpellManager {

    private final TextureAtlas ballAtlas;
    private final TextureAtlas screamAtlas;

    private final Array<Fireball> fireballs = new Array<>();
    private final Array<Scream> screams = new Array<>();

    public SpellManager() {
        ballAtlas   = new TextureAtlas(Gdx.files.internal("sprites/spells/Ball.atlas"));
        screamAtlas = new TextureAtlas(Gdx.files.internal("sprites/spells/Scream.atlas"));
    }

    public void spawnFireball(float x, float y, boolean facingRight, boolean voidHeartActive) {
        fireballs.add(new Fireball(ballAtlas, x, y, facingRight, voidHeartActive));
    }

    public void spawnScream(float x, float y, boolean voidHeartActive) {
        screams.add(new Scream(screamAtlas, x, y, voidHeartActive));
    }


    public void update(float delta, Array<SolidBlock> solidBlocks, Array<? extends Damageable> targets) {
        updateFireballs(delta, solidBlocks, targets);
        updateScreams(delta, targets);
    }

    private void updateFireballs(float delta, Array<SolidBlock> solidBlocks, Array<? extends Damageable> targets) {
        for (int i = fireballs.size - 1; i >= 0; i--) {
            Fireball fb = fireballs.get(i);
            fb.update(delta);

            if (fb.getPhase() == Fireball.Phase.TRAVELING) {
                boolean blocked = false;
                if (solidBlocks != null) {
                    for (SolidBlock b : solidBlocks) {
                        if (b.isDeadly) continue;
                        if (fb.getHitbox().overlaps(b.bounds)) {
                            blocked = true;
                            break;
                        }
                    }
                }

                if (blocked) {
                    fb.hitWall();
                } else if (targets != null) {
                    for (Damageable target : targets) {
                        if (fb.hasHit(target)) continue;
                        if (fb.getHitbox().overlaps(target.getBounds())) {
                            boolean fromRight = isFromRight(fb.getHitbox().x + fb.getHitbox().width / 2f, target);
                            target.takeDamage(fb.getDamage(), fromRight);
                            fb.registerHit(target);
                        }
                    }
                }
            }

            if (fb.isFinished()) fireballs.removeIndex(i);
        }
    }

    private void updateScreams(float delta, Array<? extends Damageable> targets) {
        for (int i = screams.size - 1; i >= 0; i--) {
            Scream sc = screams.get(i);
            sc.update(delta);

            if (sc.isHitboxActive() && targets != null) {
                for (Damageable target : targets) {
                    if (sc.hasHit(target)) continue;
                    if (overlaps(sc.getHitboxPolygon(), target.getBounds())) {
                        boolean fromRight = isFromRight(sc.getPosition().x, target);
                        target.takeDamage(sc.getDamage(), fromRight);
                        sc.registerHit(target);
                    }
                }
            }

            if (sc.isFinished()) screams.removeIndex(i);
        }
    }

    private static boolean isFromRight(float sourceX, Damageable target) {
        float targetCenterX = target.getBounds().x + target.getBounds().width / 2f;
        return sourceX > targetCenterX;
    }

    private static boolean overlaps(Polygon triangle, Rectangle rect) {
        if (!triangle.getBoundingRectangle().overlaps(rect)) return false;

        Polygon rectPoly = new Polygon(new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        });
        return Intersector.overlapConvexPolygons(triangle, rectPoly);
    }

    public void render(SpriteBatch batch) {
        for (Fireball fb : fireballs) fb.render(batch);
        for (Scream sc : screams) sc.render(batch);
    }

    public Array<Fireball> getFireballs() { return fireballs; }
    public Array<Scream> getScreams() { return screams; }

    public void dispose() {
        ballAtlas.dispose();
        screamAtlas.dispose();
    }
}
