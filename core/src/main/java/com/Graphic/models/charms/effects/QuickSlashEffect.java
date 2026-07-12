package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;

// ── 4. Quick Slash ────────────────────────────────────────────────────────────
public class QuickSlashEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.attackCooldown *= 0.1f; // attack 2.5x faster
    }
}
