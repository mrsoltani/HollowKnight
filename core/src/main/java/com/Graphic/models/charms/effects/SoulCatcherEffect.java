package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;

// ── 1. Soul Catcher ───────────────────────────────────────────────────────────
public class SoulCatcherEffect implements CharmEffect {
    @Override public void applyStats(PlayerStats s) {
        s.soulPerHit += 22; // doubles soul gain per nail hit (11 → 33)
    }
}

