package com.Graphic.models.charms;

import com.Graphic.models.PlayerStats;

public interface CharmEffect {


    void applyStats(PlayerStats stats);


    default void onEquip()   {}


    default void onUnequip() {}
}
