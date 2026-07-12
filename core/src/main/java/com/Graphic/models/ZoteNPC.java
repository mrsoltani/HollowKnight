package com.Graphic.models;

import com.Graphic.managers.AudioManager;
import com.Graphic.managers.DialogueManager;
import com.Graphic.managers.InputManager;
import com.Graphic.utils.GameAction;
import com.Graphic.views.ui.GameHUD;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class ZoteNPC {

    public enum ZoteState { IDLE, TALKING, ATTACKING, FALLING, GETTING_UP }
    private ZoteState currentState = ZoteState.IDLE;

    private final TextureAtlas atlas;
    private final Rectangle bounds;
    private final Vector2 velocity;
    private float stateTime = 0f;
    private boolean faceRight = false;

    // Animations
    private Animation<TextureRegion> idleAnim;
    private Animation<TextureRegion> talkAnim;
    private Animation<TextureRegion> attackAnim;
    private Animation<TextureRegion> fallAnim;
    private Animation<TextureRegion> getUpAnim;

    // State Timers
    private float stateTimer = 0f;
    private float soundCadenceTimer = 0f;
    private static final float ATTACK_DURATION = 3.5f;
    private static final float FALL_DURATION   = 1.5f;
    private static final float GET_UP_DURATION = 1.2f;

    // Runtime Dialogue Tracking
    private String[] currentInitialLines;
    private int dialogueIndex = 0;
    private boolean showingPrecept = false;

    public ZoteNPC(Vector2 spawnPoint) {
        this.bounds = new Rectangle(spawnPoint.x, spawnPoint.y, 24, 48);
        this.velocity = new Vector2(0, 0);

        this.atlas = new TextureAtlas(Gdx.files.internal("sprites/zote/zote.atlas"));
        initAnimations();
    }

    private void initAnimations() {
        idleAnim   = new Animation<>(0.15f, atlas.findRegions("Idle"), Animation.PlayMode.LOOP);
        talkAnim   = new Animation<>(0.15f, atlas.findRegions("Talk"), Animation.PlayMode.LOOP);
        attackAnim = new Animation<>(0.10f, atlas.findRegions("Attack"), Animation.PlayMode.LOOP);
        fallAnim   = new Animation<>(0.15f, atlas.findRegions("Fall"), Animation.PlayMode.NORMAL);
        getUpAnim  = new Animation<>(0.15f, atlas.findRegions("Get Up"), Animation.PlayMode.NORMAL);
    }

    public void update(float delta, Player player, GameHUD hud, Array<SolidBlock> solids) {
        stateTime += delta;
        float distanceToPlayer = player.getBounds().getCenter(new Vector2()).dst(bounds.getCenter(new Vector2()));

        if (currentState == ZoteState.IDLE || currentState == ZoteState.TALKING) {
            faceRight = player.getBounds().x > bounds.x;
        }

        switch (currentState) {
            case IDLE:
                velocity.set(0, 0);
                int upKey = InputManager.getKeyCode(GameAction.UP);
                if (distanceToPlayer <= 100f && Gdx.input.isKeyJustPressed(upKey)) {
                    startDialogue(hud);
                }
                checkHitboxes(player);
                break;

            case TALKING:
                velocity.set(0, 0);
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    advanceDialogue(hud);
                }
                break;

            case ATTACKING:
                stateTimer += delta;
                soundCadenceTimer += delta;

                // Keep playing the running loop audio clear of overlap stacking
                if (soundCadenceTimer >= 0.28f) {
                    soundCadenceTimer = 0f;
                    AudioManager.playChannelSFX("zote_run", 0.96f, 1.04f, 0.8f, "zote_run_loop");
                }

                if (velocity.x == 0) {
                    velocity.x = (player.getBounds().x > bounds.x) ? 140f : -140f;
                }
                faceRight = velocity.x > 0;

                // Player impact contact
                if (bounds.overlaps(player.getBounds())) {
                    AudioManager.playSFX("zote_impact_tonk", 0.9f);
                    velocity.x = -velocity.x;
                    faceRight = velocity.x > 0;
                    bounds.x += velocity.x * delta * 2f;
                }

                float nextX = bounds.x + velocity.x * delta;
                Rectangle futureBounds = new Rectangle(nextX, bounds.y, bounds.width, bounds.height);

                boolean wallHit = false;
                boolean hasGroundAhead = false;

                float sensorX = faceRight ? nextX + bounds.width + 4 : nextX - 4;
                float sensorY = bounds.y - 8;

                for (SolidBlock solid : solids) {
                    if (futureBounds.overlaps(solid.bounds)) {
                        wallHit = true;
                    }
                    if (solid.bounds.contains(sensorX, sensorY)) {
                        hasGroundAhead = true;
                    }
                }

                // Wall or Edge Turnaround processing
                if (wallHit || !hasGroundAhead) {
                    AudioManager.playSFX("zote_impact_tonk", 0.7f);
                    velocity.x = -velocity.x;
                    faceRight = velocity.x > 0;
                }

                bounds.x += velocity.x * delta;

                if (stateTimer >= ATTACK_DURATION) {
                    changeState(ZoteState.FALLING);
                }
                break;

            case FALLING:
                stateTimer += delta;
                velocity.set(0, 0);
                if (stateTimer >= FALL_DURATION) {
                    changeState(ZoteState.GETTING_UP);
                }
                break;

            case GETTING_UP:
                stateTimer += delta;
                if (stateTimer >= GET_UP_DURATION) {
                    changeState(ZoteState.IDLE);
                }
                break;
        }
    }

    private void startDialogue(GameHUD hud) {
        changeState(ZoteState.TALKING);
        playRandomGrunt();

        if (!DialogueManager.hasFinishedInitial("zote")) {
            showingPrecept = false;
            currentInitialLines = DialogueManager.getInitialLines("zote");
            dialogueIndex = 0;

            if (currentInitialLines != null && currentInitialLines.length > 0) {
                hud.showDialogueText(currentInitialLines[dialogueIndex]);
            } else {
                DialogueManager.markInitialDone("zote");
                hud.closeDialogue();
                changeState(ZoteState.IDLE);
            }
        } else {
            showingPrecept = true;
            String randomPrecept = DialogueManager.getRandomPrecept("zote");
            hud.showDialogueText(randomPrecept);
        }
    }

    private void advanceDialogue(GameHUD hud) {
        if (showingPrecept) {
            hud.closeDialogue();
            changeState(ZoteState.IDLE);
        } else {
            dialogueIndex++;
            if (dialogueIndex < currentInitialLines.length) {
                playRandomGrunt();
                hud.showDialogueText(currentInitialLines[dialogueIndex]);
            } else {
                DialogueManager.markInitialDone("zote");
                hud.closeDialogue();
                changeState(ZoteState.IDLE);
            }
        }
    }

    private void checkHitboxes(Player player) {
        boolean isPlayerAttacking = (player.getState() == PlayerState.ATTACK ||
            player.getState() == PlayerState.ATTACK_ALT ||
            player.getState() == PlayerState.DOWN_SLASH);

        if (!isPlayerAttacking) return;

        Rectangle fw = player.getForwardAttackBox();
        Rectangle dw = player.getDownwardAttackBox();

        if ((fw != null && fw.overlaps(bounds)) || (dw != null && dw.overlaps(bounds))) {
            changeState(ZoteState.ATTACKING);
        }
    }

    private void changeState(ZoteState newState) {
        this.currentState = newState;
        this.stateTime = 0f;
        this.stateTimer = 0f;
        this.soundCadenceTimer = 0f;

        // Cleanup and route state transitions to correct SFX keys
        if (newState != ZoteState.ATTACKING) {
            AudioManager.stopChannel("zote_run");
        }

        switch (newState) {
            case ATTACKING:
                AudioManager.playSFX("zote_roar", 1.0f);
                break;
            case FALLING:
                AudioManager.playSFX("zote_fall_air", 1.0f);
                break;
            case GETTING_UP:
                AudioManager.playSFX("zote_land_floor", 1.0f);
                AudioManager.playSFX("zote_get_up", 1.0f);
                break;
            case IDLE:
            case TALKING:
                velocity.set(0, 0);
                break;
        }
    }

    private void playRandomGrunt() {
        // Picks randomly across your real Zote 01.mp3 through Zote 05.mp3 files
        int gruntIndex = MathUtils.random(1, 5);
        AudioManager.playSFX("zote_grunt_" + gruntIndex, 1.0f);
    }

    public void render(SpriteBatch batch) {
        Animation<TextureRegion> activeAnim = idleAnim;

        switch (currentState) {
            case TALKING:    activeAnim = talkAnim; break;
            case ATTACKING:  activeAnim = attackAnim; break;
            case FALLING:    activeAnim = fallAnim; break;
            case GETTING_UP: activeAnim = getUpAnim; break;
            default:         activeAnim = idleAnim; break;
        }

        TextureRegion frame = activeAnim.getKeyFrame(stateTime);

        if (faceRight && !frame.isFlipX()) frame.flip(true, false);
        if (!faceRight && frame.isFlipX()) frame.flip(true, false);

        float drawX = bounds.x + (bounds.width / 2f) - (frame.getRegionWidth() / 2f);
        float drawY = bounds.y;

        batch.draw(frame, drawX, drawY);
    }

    public Rectangle getBounds() { return bounds; }
    public void dispose() {
        if (atlas != null) atlas.dispose();
    }
}
