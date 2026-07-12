package com.Graphic.models;

public class PlayerStats {

    // ── Soul ──────────────────────────────────────────────────────────────
    public int   soulPerHit          = 11;

    // ── Nail (attack) ─────────────────────────────────────────────────────
    public int   nailDamage          = 10;
    public float attackCooldown      = 0.5f;   // seconds between nail swings

    // ── Dash ──────────────────────────────────────────────────────────────
    public float dashCooldown        = 0.4f;
    public float dashDuration        = 0.4f;
    public float dashSpeed           = 800f;
    public boolean sharpShadowActive = false;  // dash-through enemies

    // ── Knockback ─────────────────────────────────────────────────────────
    public float knockbackMultiplier = 1.0f;   // applied to enemy knockback force

    // ── Focus (heal) ──────────────────────────────────────────────────────
    public float focusDuration       = 1.5f;   // seconds to hold for one heal

    // ── Spells ────────────────────────────────────────────────────────────
    public float spellDamageMultiplier = 1.0f;
    public boolean voidHeartActive     = false; // triggers upgraded spell anim

    /** Reset to base values — called before reapplying all active charms. */
    public void reset() {
        soulPerHit             = 11;
        nailDamage             = 10;
        attackCooldown         = 0.5f;
        dashCooldown           =0.4f;
        dashDuration           = 0.4f;
        dashSpeed              = 800f;
        sharpShadowActive      = false;
        knockbackMultiplier    = 1.0f;
        focusDuration          = 1.5f;
        spellDamageMultiplier  = 1.0f;
        voidHeartActive        = false;
    }
}

