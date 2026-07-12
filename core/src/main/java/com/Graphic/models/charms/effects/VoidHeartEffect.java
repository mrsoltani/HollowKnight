package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class VoidHeartEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.spellDamageMultiplier *= 1.5f;
        s.voidHeartActive = true;
    }
}
