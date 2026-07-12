package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class HeavyBlowEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.knockbackMultiplier *= 2.0f;
    }
}
