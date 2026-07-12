package com.Graphic.managers;

import com.Graphic.models.Charm;
import com.Graphic.models.GameSaveData;
import com.Graphic.models.PlayerStats;
import com.Graphic.models.charms.CharmId;
import com.Graphic.models.charms.effects.*;

import java.util.ArrayList;
import java.util.List;

public class CharmManager {

    private static final int MAX_NOTCHES = 3;
    private static final int MAX_EQUIPPED_SLOTS = 3;

    private static final List<Charm> charms = new ArrayList<>();
    private static final PlayerStats stats  = new PlayerStats();

    public static void init() {
        charms.clear();
        charms.add(new Charm(
            CharmId.SOUL_CATCHER,         "Soul Catcher",
            "Soul Catcher.png",
            "Increases soul gained per nail hit.",
            true, 1, new SoulCatcherEffect()
        ));
        charms.add(new Charm(
            CharmId.DASHMASTER,           "Dashmaster",
            "Dashmaster.png",
            "Reduces dash cooldown significantly.",
            true, 1, new DashMasterEffect()
        ));
        charms.add(new Charm(
            CharmId.UNBREAKABLE_STRENGTH, "Unbreakable Strength",
            "Unbreakable Strength.png",
            "Increases nail damage by 50%.",
            true, 1, new UnbreakableStrengthEffect()
        ));
        charms.add(new Charm(
            CharmId.QUICK_SLASH,          "Quick Slash",
            "Quick Slash.png",
            "Attack much faster with the nail.",
            true, 1, new QuickSlashEffect()
        ));
        charms.add(new Charm(
            CharmId.QUICK_FOCUS,          "Quick Focus",
            "Quick Focus.png",
            "Reduces focus time to heal.",
            true, 1, new QuickFocusEffect()
        ));
        charms.add(new Charm(
            CharmId.HEAVY_BLOW,           "Heavy Blow",
            "Heavy Blow.png",
            "Enemies are knocked back further.",
            true, 1, new HeavyBlowEffect()
        ));
        charms.add(new Charm(
            CharmId.SHARP_SHADOW,         "Sharp Shadow",
            "Sharp Shadow.png",
            "Dash through enemies dealing damage. Dash distance +20%.",
            true, 1, new SharpShadowEffect()
        ));
        charms.add(new Charm(
            CharmId.VOID_HEART,           "Void Heart",
            "Void Heart.png",
            "Spells deal 50% more damage and use upgraded animations.",
            false, 1, new VoidHeartEffect()
        ));

        recomputeStats();
    }

    // ── Equip / unequip ───────────────────────────────────────────────────

    public static boolean equip(CharmId id) {
        Charm charm = find(id);
        if (charm == null || !charm.unlocked || charm.equipped) return false;

        if (getUsedNotches() + charm.notchCost > MAX_NOTCHES) return false;

        charm.equipped = true;
        charm.effect.onEquip();
        recomputeStats();
        return true;
    }

    public static void unequip(CharmId id) {
        Charm charm = find(id);
        if (charm == null || !charm.equipped) return;

        charm.equipped = false;
        charm.effect.onUnequip();
        recomputeStats();
    }

    public static void toggle(CharmId id) {
        Charm charm = find(id);
        if (charm == null) return;
        if (charm.equipped) unequip(id);
        else                equip(id);
    }

    public static void unlockVoidHeart(){
        charms.get(7).unlocked=true;
        SaveManager.currentSave.charmAcquired=true;
    }

    // ── Save / Load ──────────────────────────────────────────────────────

    /**
     * Unequips everything currently equipped and re-equips whatever is stored
     * in the given save data's equippedCharm1/2/3 fields (NONE slots are skipped).
     * Call this right after loading a save, once CharmManager.init() has run.
     */
    public static void loadCharms(GameSaveData save) {
        if (save == null) return;

        // check if void heart is collected or not
        charms.get(7).unlocked=save.charmAcquired;

        for (Charm c : charms) {
            if (c.equipped) {
                c.equipped = false;
                c.effect.onUnequip();
            }
        }

        equipFromSaveIfPresent(save.equippedCharm1);
        equipFromSaveIfPresent(save.equippedCharm2);
        equipFromSaveIfPresent(save.equippedCharm3);

        recomputeStats();
    }

    private static void equipFromSaveIfPresent(CharmId id) {
        if (id == null || id == CharmId.NONE) return;

        Charm charm = find(id);
        if (charm == null || !charm.unlocked || charm.equipped) return;
        if (getUsedNotches() + charm.notchCost > MAX_NOTCHES) return;

        charm.equipped = true;
        charm.effect.onEquip();
    }

    /**
     * Writes the currently equipped charms into the given save data's
     * equippedCharm1/2/3 fields (unused slots are set to NONE).
     * Call this right before SaveManager.saveCurrentGame().
     */
    public static void writeEquippedCharms(GameSaveData save) {
        if (save == null) return;

        List<Charm> equipped = new ArrayList<>();
        for (Charm c : charms) {
            if (c.equipped) equipped.add(c);
        }

        save.equippedCharm1 = equipped.size() > 0 ? equipped.get(0).id : CharmId.NONE;
        save.equippedCharm2 = equipped.size() > 1 ? equipped.get(1).id : CharmId.NONE;
        save.equippedCharm3 = equipped.size() > 2 ? equipped.get(2).id : CharmId.NONE;
    }

    // ── Stats recompute ───────────────────────────────────────────────────

    /**
     * Called every time equip state changes.
     * Resets to base then applies all active charms in order.
     * Player just reads getStats() — no charm checks anywhere in Player code.
     */
    private static void recomputeStats() {
        stats.reset();
        for (Charm c : charms) {
            if (c.equipped) {
                c.effect.applyStats(stats);
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public static PlayerStats getStats()    { return stats; }
    public static List<Charm> getAll()      { return charms; }
    public static int getMaxNotches()       { return MAX_NOTCHES; }

    public static int getUsedNotches() {
        return charms.stream()
            .filter(c -> c.equipped)
            .mapToInt(c -> c.notchCost)
            .sum();
    }

    public static boolean isEquipped(CharmId id) {
        Charm c = find(id);
        return c != null && c.equipped;
    }

    public static Charm find(CharmId id) {
        for (Charm c : charms) {
            if (c.id == id) return c;
        }
        return null;
    }

    public static void toggleOneHitMode(){
        if(stats.nailDamage<1000){
            stats.nailDamage=1000;
        }
        else{
            recomputeStats();
        }
    }
}
