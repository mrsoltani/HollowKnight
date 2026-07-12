package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;


public class SharpShadowEffect implements CharmEffect {
    private java.util.function.Consumer<Object> dashHitListener;

    @Override
    public void applyStats(PlayerStats s) {
        s.dashSpeed *= 1.2f;
        s.dashDuration *= 1.2f;
        s.sharpShadowActive = true;
    }

}
