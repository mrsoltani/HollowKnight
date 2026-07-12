package com.Graphic.models.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class FalseKnightAI {


    private static final float IDLE_DURATION_MIN  = 0.3f;
    private static final float IDLE_DURATION_MAX  = 0.8f;
    private static final float DECISION_COOLDOWN  = 0.25f;


    private static final float CLOSE_RANGE  = 200f;
    private static final float MEDIUM_RANGE = 450f;





    private static final float RUSH_VELOCITY_THRESHOLD = 250f;


    private BossMove lastMove     = BossMove.IDLE;
    private BossMove prevLastMove = BossMove.IDLE;

    private float decisionTimer = 0.6f;

    public enum BossMove {
        IDLE, SLAM, CHARGE, OFFENSIVE_LEAP, DEFENSIVE_LEAP, POWER_SLAM
    }


    public BossMove update(float delta, Rectangle bossBounds, Rectangle playerBounds,
                           boolean isPhase2, float playerVelX) {
        decisionTimer -= delta;
        if (decisionTimer > 0) return BossMove.IDLE;

        float bossCX    = bossBounds.x + bossBounds.width / 2f;
        float playerCX  = playerBounds.x + playerBounds.width / 2f;
        float dist      = Math.abs(bossCX - playerCX);



        float approachSpeed = (playerCX > bossCX) ? -playerVelX : playerVelX;

        BossMove chosen = decide(dist, isPhase2, approachSpeed);
        decisionTimer   = computeNextCooldown(isPhase2);

        prevLastMove = lastMove;
        lastMove     = chosen;
        return chosen;
    }


    public BossMove update(float delta, Rectangle bossBounds, Rectangle playerBounds, boolean isPhase2) {
        return update(delta, bossBounds, playerBounds, isPhase2, 0f);
    }

    private BossMove decide(float dist, boolean isPhase2, float approachSpeed) {
        java.util.List<BossMove> pool = new java.util.ArrayList<>();
        boolean isRushingIn  = approachSpeed >  RUSH_VELOCITY_THRESHOLD;
        boolean isRetreating = approachSpeed < -RUSH_VELOCITY_THRESHOLD;

        if (dist < CLOSE_RANGE) {
            pool.add(BossMove.SLAM);
            pool.add(BossMove.SLAM);
            pool.add(BossMove.SLAM);
            pool.add(BossMove.DEFENSIVE_LEAP);
            if (isPhase2) {
                pool.add(BossMove.POWER_SLAM);
                pool.add(BossMove.POWER_SLAM);
            }

            if (isRushingIn) {
                pool.add(BossMove.SLAM);
                if (isPhase2) pool.add(BossMove.POWER_SLAM);
            }

        } else if (dist < MEDIUM_RANGE) {
            pool.add(BossMove.CHARGE);
            pool.add(BossMove.CHARGE);
            pool.add(BossMove.OFFENSIVE_LEAP);
            pool.add(BossMove.SLAM);
            if (isPhase2) pool.add(BossMove.POWER_SLAM);

            if (isRushingIn) {


                pool.add(BossMove.OFFENSIVE_LEAP);
                pool.add(BossMove.OFFENSIVE_LEAP);
            } else if (isRetreating) {

                pool.add(BossMove.CHARGE);
                pool.add(BossMove.CHARGE);
            }

        } else {
            pool.add(BossMove.OFFENSIVE_LEAP);
            pool.add(BossMove.OFFENSIVE_LEAP);
            pool.add(BossMove.CHARGE);
            pool.add(BossMove.DEFENSIVE_LEAP);
            if (isPhase2) pool.add(BossMove.POWER_SLAM);

            if (isRetreating) {

                pool.add(BossMove.OFFENSIVE_LEAP);
                pool.add(BossMove.CHARGE);
            }
        }


        pool.remove(lastMove);




        java.util.List<BossMove> strictPool = new java.util.ArrayList<>(pool);
        strictPool.remove(prevLastMove);
        if (!strictPool.isEmpty()) pool = strictPool;

        if (pool.isEmpty()) {

            pool.add(dist < CLOSE_RANGE ? BossMove.SLAM : BossMove.CHARGE);
        }

        return pool.get(MathUtils.random(pool.size() - 1));
    }

    private float computeNextCooldown(boolean isPhase2) {
        float base = MathUtils.random(IDLE_DURATION_MIN, IDLE_DURATION_MAX);
        float scaled = isPhase2 ? base * 0.55f : base * 0.85f;
        return Math.max(DECISION_COOLDOWN, scaled);
    }

    public void reset() {
        lastMove      = BossMove.IDLE;
        prevLastMove  = BossMove.IDLE;
        decisionTimer = 0.6f;
    }
}
