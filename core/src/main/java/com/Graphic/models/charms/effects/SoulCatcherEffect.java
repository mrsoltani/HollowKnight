package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class SoulCatcherEffect implements CharmEffect {
    @Override public void applyStats(PlayerStats s) {
        s.soulPerHit += 22;
    }
}

