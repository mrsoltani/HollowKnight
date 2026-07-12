package com.Graphic.models.enemies;

/**
 * Union of every animation/behaviour state used across all four enemies.
 *
 * Not every enemy uses every state — each concrete enemy only registers
 * animations for (and only ever transitions into) the subset that its brain
 * actually needs. Keeping a single shared enum lets {@code BaseEnemy} own the
 * generic bits (death physics, the {@code AnimationComponent} lookup, debug
 * labels) without caring which specific enemy it is driving.
 *
 * Death-state contract (shared by all enemies):
 *  - DEAD_AIR  : enemy was killed while airborne and is now falling. When it
 *                finally touches the ground the state machine swaps it to
 *                DEAD_LAND.
 *  - DEAD_LAND : enemy was killed while already on the ground, OR an airborne
 *                DEAD_AIR corpse just landed. This is the terminal state.
 */
public enum EnemyState {
    IDLE,
    WALK,
    TURN,
    ATTACK_ANTICIPATE,
    ATTACK,          // Mosquito swoop / generic attack loop
    ATTACK_LUNGE,    // Husk Hornhead high-speed charge
    SHOOT,           // Crystal Guardian gun wind-up (laser logic added later)
    EVADE,           // Crystal Guardian dodge/recovery clip
    DEAD_AIR,
    DEAD_LAND
}
