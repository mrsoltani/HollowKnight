package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class UnbreakableStrengthEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.nailDamage = (int) (s.nailDamage * 1.5f);
    }
}
