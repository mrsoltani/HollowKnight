package com.Graphic.models;

import com.Graphic.managers.EventBus;
import com.Graphic.models.enums.PlatformDirection;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class PlatformPuzzle {

    public enum State { IDLE, REVEALING, RETURNING, CHALLENGE, SOLVED }

    private static final float REVEAL_HOLD_DURATION = 5f;

    private final String groupId;
    private final PressurePlate plate;
    private final Array<RotatablePlatform> platforms;
    private final Array<PlatformDirection> targets = new Array<>();

    private State state = State.IDLE;
    private float revealHoldTimer = 0f;
    private boolean rewardGiven = false;

    // True for exactly the update() call where this puzzle transitions to
    // SOLVED — GameScreen polls and consumes this to trigger the cutscene +
    // charm unlock exactly once, without needing an EventBus subscriber.
    private boolean solvedPending = false;

    public PlatformPuzzle(String groupId, PressurePlate plate, Array<RotatablePlatform> platforms) {
        this.groupId = groupId;
        this.plate = plate;
        this.platforms = platforms;

        // Answer is rolled once per puzzle instance (i.e. once per room
        // load) so the correct configuration is fixed for the room's
        // lifetime rather than re-randomizing on every plate press.
        for (int i = 0; i < platforms.size; i++) {
            targets.add(PlatformDirection.random());
        }
    }

    public void update(float delta, Rectangle playerBounds) {
        for (RotatablePlatform p : platforms) p.update(delta);

        boolean triggered = plate.update(delta, playerBounds);

        if (state == State.SOLVED) return;

        if (triggered) {
            if (state == State.IDLE) {
                startReveal();
            } else if (state == State.CHALLENGE) {
                checkAnswer();
            }
            // A press during REVEALING/RETURNING is ignored — plate.update()
            // itself won't even return triggered then, since its own state
            // machine only re-arms from IDLE, but this guards intent clearly.
            return;
        }

        switch (state) {
            case REVEALING:
                if (allPlatformsIdle()) {
                    revealHoldTimer += delta;
                    if (revealHoldTimer >= REVEAL_HOLD_DURATION) {
                        startReturn();
                    }
                }
                break;

            case RETURNING:
                if (allPlatformsIdle()) {
                    state = State.CHALLENGE;
                }
                break;

            case CHALLENGE:
                // No auto-check here anymore — solving is only evaluated when
                // the player deliberately steps on the plate again.
                break;

            case IDLE:
            default:
                break;
        }
    }

    private void startReveal() {
        state = State.REVEALING;
        revealHoldTimer = 0f;
        for (int i = 0; i < platforms.size; i++) {
            platforms.get(i).rotateTo(targets.get(i));
        }
        EventBus.emit(EventBus.Event.PUZZLE_CHALLENGE_START, groupId);
    }

    private void startReturn() {
        state = State.RETURNING;
        for (RotatablePlatform p : platforms) p.rotateTo(PlatformDirection.UP);
    }

    /** Called when the player steps on the plate a second time, during
     *  CHALLENGE — submits their current platform configuration for checking. */
    private void checkAnswer() {
        if (isCurrentlySolved()) {
            state = State.SOLVED;
            if (!rewardGiven) {
                rewardGiven = true;
                solvedPending = true;
                EventBus.emit(EventBus.Event.PUZZLE_SOLVED, groupId);
            }
        }
        // Wrong answer: stay in CHALLENGE, platforms keep whatever the
        // player left them at, they can keep adjusting and try the plate
        // again whenever they think it's right.
    }

    private boolean allPlatformsIdle() {
        for (RotatablePlatform p : platforms) {
            if (p.isRotating()) return false;
        }
        return true;
    }

    private boolean isCurrentlySolved() {
        for (int i = 0; i < platforms.size; i++) {
            if (!platforms.get(i).matches(targets.get(i))) return false;
        }
        return true;
    }

    /** Consumed once by GameScreen to fire the unlock cutscene exactly once. */
    public boolean consumeSolvedPending() {
        if (!solvedPending) return false;
        solvedPending = false;
        return true;
    }

    public boolean isChallengeActive() { return state == State.CHALLENGE; }
    public boolean isRevealing() { return state == State.REVEALING || state == State.RETURNING; }
    public State getState() { return state; }
    public Array<PlatformDirection> getTargets() { return targets; }
    public Array<RotatablePlatform> getPlatforms() { return platforms; }
    public PressurePlate getPlate() { return plate; }
    public String getGroupId() { return groupId; }
}
