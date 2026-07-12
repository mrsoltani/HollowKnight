package com.Graphic.models.charms;

import com.Graphic.models.PlayerStats;

public interface CharmEffect {

    /**
     * Pure stat modification — called every time equip state changes.
     * Modify stats directly; do not store references or trigger side effects.
     */
    void applyStats(PlayerStats stats);

    /**
     * Called once when the charm is equipped.
     * Use for EventBus subscriptions or one-time setup.
     */
    default void onEquip()   {}

    /**
     * Called once when the charm is unequipped.
     * Use to unsubscribe from EventBus or clean up.
     */
    default void onUnequip() {}
}
