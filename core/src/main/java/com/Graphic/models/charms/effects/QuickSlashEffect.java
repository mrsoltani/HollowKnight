package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class QuickSlashEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.attackCooldown *= 0.1f;
    }
}
