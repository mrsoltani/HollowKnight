package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;

// ── 8. Void Heart ────────────────────────────────────────────────────────────
public class VoidHeartEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.spellDamageMultiplier *= 1.5f; // +50% spell damage
        s.voidHeartActive = true; // triggers black spell animations
    }
}
