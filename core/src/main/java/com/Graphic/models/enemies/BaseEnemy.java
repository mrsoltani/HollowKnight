package com.Graphic.models.enemies;

import com.Graphic.managers.EventBus;
import com.Graphic.managers.SaveManager;
import com.Graphic.models.spells.Damageable;
import com.Graphic.utils.DamageFlash;
import com.Graphic.utils.EffectSpawnData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public abstract class BaseEnemy implements EnemyAI, Damageable {


    protected static final float GRAVITY          = -1400f;
    protected static final float MAX_FALL_SPEED   = -1200f;
    protected static final float CORPSE_FALL_NUDGE = 0f;



    protected final Rectangle bounds;
    protected final Vector2 velocity = new Vector2();
    protected boolean facingRight;
    protected boolean onGround = false;


    protected float health;
    protected EnemyState state;


    protected boolean affectedByGravityWhileAlive;


    protected final AnimationComponent anim = new AnimationComponent();






    protected float frameWidth;
    protected float frameHeight;
    protected float spriteOffsetX;
    protected float spriteOffsetY;


    private boolean fullyDead = false;



    private static final float HIT_INVULN = 0.28f;
    private float hitInvulnTimer = 0f;


    /** Seconds of red hit-flash left. Mirrors DamageFlash.DURATION. */
    protected float hitFlashTimer = 0f;




    private static final float KNOCKBACK_X    = 1000f;
    private static final float KNOCKBACK_Y    = 160f;
    private static final float KNOCKBACK_TIME = 0.16f;
    private float knockbackTimer = 0f;




    private static final float DEATH_KNOCKBACK_X = 1600f;
    private static final float DEATH_KNOCKBACK_Y = 500f;
    private float groundStepTimer = 0f;

    protected BaseEnemy(float x, float y, float width, float height,
                        boolean facingRight, float health,
                        boolean affectedByGravityWhileAlive) {
        this.bounds = new Rectangle(x, y, width, height);
        this.facingRight = facingRight;
        this.health = health;
        this.affectedByGravityWhileAlive = affectedByGravityWhileAlive;
        this.state = EnemyState.IDLE;




        this.frameWidth = width;
        this.frameHeight = height;
        this.spriteOffsetX = 0f;
        this.spriteOffsetY = 0f;
    }


    protected void setSpriteBox(float w, float h, float offsetX, float offsetY) {
        this.frameWidth = w;
        this.frameHeight = h;
        this.spriteOffsetX = offsetX;
        this.spriteOffsetY = offsetY;
    }





    @Override
    public final void update(float delta, Rectangle target, Array<Rectangle> platforms) {
        anim.update(delta);
        if (hitInvulnTimer > 0f) hitInvulnTimer -= delta;
        // Ticked before the dying branch so the killing blow still flashes on the corpse.
        if (hitFlashTimer  > 0f) hitFlashTimer  -= delta;

        if (isDying()) {


            updateDeath(delta, platforms);
            return;
        }



        if (knockbackTimer > 0f) {
            knockbackTimer -= delta;
            if (affectedByGravityWhileAlive) applyGravity(delta);
            moveAndCollide(delta, platforms);
            dispatchContactDamage(target);
            return;
        }


        updateAI(delta, target, platforms);


        if (affectedByGravityWhileAlive) {
            applyGravity(delta);
        }
        moveAndCollide(delta, platforms);
        updateGroundSteps(delta);


        dispatchContactDamage(target);
    }






    protected abstract void updateAI(float delta, Rectangle target, Array<Rectangle> platforms);


    protected abstract float getContactDamage();


    protected abstract String enemyTypeName();






    public void kill() {
        kill(facingRight);
    }


    public void kill(boolean fromRight) {
        if (isDying()) return;
        health = 0;

        velocity.x = fromRight ? -DEATH_KNOCKBACK_X : DEATH_KNOCKBACK_X;
        velocity.y = DEATH_KNOCKBACK_Y;
        onGround = false;



        EventBus.emit(EventBus.Event.ENEMY_KILLED, enemyTypeName());
        onKilled();

        changeState(EnemyState.DEAD_AIR);
    }


    protected float groundStepInterval() {
        return 0f;
    }

    private void updateGroundSteps(float delta) {
        float interval = groundStepInterval();
        boolean movingOnGround = interval > 0f && onGround && Math.abs(velocity.x) > 1f && isAlive();
        if (!movingOnGround) {
            groundStepTimer = 0f;
            return;
        }

        groundStepTimer -= delta;
        if (groundStepTimer <= 0f) {
            EventBus.emit(EventBus.Event.ENEMY_GROUND_STEP, enemyTypeName());
            groundStepTimer = interval;
        }
    }

    protected void onKilled() {
    }

    @Override
    public void takeDamage(float amount, boolean fromRight) {
        if (isDying()) return;
        if (hitInvulnTimer > 0f) return;
        health -= amount;
        hitInvulnTimer = HIT_INVULN;
        hitFlashTimer  = DamageFlash.DURATION;



        EventBus.emit(EventBus.Event.ENEMY_HIT,enemyTypeName());

        if (health <= 0) {
            kill(fromRight);
            SaveManager.currentSave.enemiesKilled++;
            return;
        }


        knockbackTimer = KNOCKBACK_TIME;
        velocity.x = fromRight ? -KNOCKBACK_X : KNOCKBACK_X;
        velocity.y = KNOCKBACK_Y;
        onGround = false;
    }


    private void updateDeath(float delta, Array<Rectangle> platforms) {
        if (state == EnemyState.DEAD_AIR) {


            applyGravity(delta);
            moveAndCollide(delta, platforms);


            if (onGround) {
                changeState(EnemyState.DEAD_LAND);
            }
        } else if (state == EnemyState.DEAD_LAND) {





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


    public boolean isAlive() {
        return !isDying();
    }





    protected void applyGravity(float delta) {
        velocity.y += GRAVITY * delta;
        if (velocity.y < MAX_FALL_SPEED) velocity.y = MAX_FALL_SPEED;
    }


    private boolean hitWallThisFrame = false;
    private boolean hitSolidThisFrame = false;

    protected void moveAndCollide(float delta, Array<Rectangle> platforms) {
        hitWallThisFrame = false;
        hitSolidThisFrame = false;


        bounds.x += velocity.x * delta;
        for (Rectangle p : platforms) {
            if (!bounds.overlaps(p)) continue;
            if (velocity.x > 0) {
                bounds.x = p.x - bounds.width;
                hitWallThisFrame = true;
                hitSolidThisFrame = true;
            } else if (velocity.x < 0) {
                bounds.x = p.x + p.width;
                hitWallThisFrame = true;
                hitSolidThisFrame = true;
            }
        }


        onGround = false;
        bounds.y += velocity.y * delta;
        for (Rectangle p : platforms) {
            if (!bounds.overlaps(p)) continue;
            if (velocity.y <= 0) {
                bounds.y = p.y + p.height;
                velocity.y = 0;
                onGround = true;
                hitSolidThisFrame = true;
            } else {
                bounds.y = p.y - bounds.height;
                velocity.y = 0;
                hitSolidThisFrame = true;
            }
        }
    }


    protected boolean isBlockedByWall() {
        return hitWallThisFrame;
    }


    protected boolean isBlockedBySolid() {
        return hitSolidThisFrame;
    }


    protected boolean isCliffAhead(Array<Rectangle> platforms, float lookAhead) {

        float footX = facingRight ? bounds.x + bounds.width + lookAhead
            : bounds.x - lookAhead;
        float footY = bounds.y - 2f;

        for (Rectangle p : platforms) {
            if (p.contains(footX, footY)) {
                return false;
            }
        }
        return true;
    }


    protected boolean isWallAhead(Array<Rectangle> platforms, float lookAhead) {
        float probeX = facingRight ? bounds.x + bounds.width
            : bounds.x - lookAhead;
        Rectangle probe = new Rectangle(probeX, bounds.y + 2f, lookAhead, bounds.height - 4f);
        for (Rectangle p : platforms) {
            if (probe.overlaps(p)) return true;
        }
        return false;
    }






    protected void dispatchContactDamage(Rectangle target) {
        if (target == null) return;
        if (bounds.overlaps(target)) {





            onContact(target);
        }
    }


    protected void onContact(Rectangle target) {

    }






    protected void changeState(EnemyState newState) {
        if (this.state == newState) return;
        this.state = newState;
        anim.resetStateTime();
    }

    protected void faceRight(boolean right) {
        this.facingRight = right;
    }






    @Override
    public void render(SpriteBatch batch) {
        TextureRegion frame = anim.getFrame(state, facingRight);
        if (frame == null) return;


        float drawX = bounds.x + bounds.width / 2f - frameWidth / 2f + spriteOffsetX;
        float drawY = bounds.y + spriteOffsetY;
        DamageFlash.draw(batch, frame, drawX, drawY, frameWidth, frameHeight, hitFlashTimer);
    }


    @Override
    public void renderDebug(ShapeRenderer shapes) {
        shapes.setColor(isDying() ? Color.GRAY : Color.RED);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }





    @Override
    public Rectangle getHitbox() {
        return bounds;
    }


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
