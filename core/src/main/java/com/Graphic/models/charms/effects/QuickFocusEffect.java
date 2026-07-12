package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class QuickFocusEffect implements CharmEffect {
    @Override
    public void applyStats(PlayerStats s) {
        s.focusDuration *= 0.6f;
    }
}
