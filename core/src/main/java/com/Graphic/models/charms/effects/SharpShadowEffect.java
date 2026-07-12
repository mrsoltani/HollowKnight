package com.Graphic.models.charms.effects;

import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmEffect;

// ── 7. Sharp Shadow ───────────────────────────────────────────────────────────
public class SharpShadowEffect implements CharmEffect {
    private java.util.function.Consumer<Object> dashHitListener;

    @Override
    public void applyStats(PlayerStats s) {
        s.dashSpeed *= 1.2f;  // +20% dash distance
        s.dashDuration *= 1.2f;
        s.sharpShadowActive = true;  // GameScreen checks this to enable dash-through
    }

}
