package com.Graphic.models;

public class PlayerStats {


    public int   soulPerHit          = 11;


    public int   nailDamage          = 10;
    public float attackCooldown      = 0.5f;


    public float dashCooldown        = 0.4f;
    public float dashDuration        = 0.4f;
    public float dashSpeed           = 800f;
    public boolean sharpShadowActive = false;


    public float knockbackMultiplier = 1.0f;


    public float focusDuration       = 1.5f;


    public float spellDamageMultiplier = 1.0f;
    public boolean voidHeartActive     = false;


    public void reset() {
        soulPerHit             = 11;
        nailDamage             = 100;
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

