package com.Graphic.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {

    public enum Event {
        // ── System & UI ──────────────────────────────────────────────────
        MENU_NAVIGATE,
        MENU_SELECT,
        MUSIC_CHANGED,
        SFX_CHANGED,
        GAME_PAUSED,
        GAME_COMPLETED,       // payload: Float elapsed run time in seconds
        ACHIEVEMENT_UNLOCKED, // payload: Achievement
        CHARM_COLLECTED,
        // ── Player Movement & Combat ─────────────────────────────────────
        PLAYER_WALK,        // Emitted continuously/on timer during walking
        PLAYER_RUN,          // Emitted continuously/on timer during running
        PLAYER_STOP_WALK,
        PLAYER_JUMP,
        PLAYER_DOUBLE_JUMP, // Mapped to the Wings sound
        PLAYER_LAND_HARD,
        PLAYER_DASH,
        PLAYER_SHADOW_DASH,
        PLAYER_WALL_JUMP,
        PLAYER_WALL_SLIDE,
        PLAYER_ATTACK,
        PLAYER_ATTACK_ALT,
        PLAYER_DOWN_SLASH,
        PLAYER_DAMAGED,
        PLAYER_DEATH,
        PLAYER_FIREBALL,
        PLAYER_SCREAM_SPELL,
        PLAYER_HEAL,
        PLAYER_LAND_SOFT,
        PLAYER_HIT_WALL,
        PLAYER_SOUL_GAIN,

        // ── Generic Enemies ──────────────────────────────────────────────
        ENEMY_HIT,
        ENEMY_KILLED,

        // ── Boss: False Knight / FKnight ─────────────────────────────────
        BOSS_FK_JUMP,
        BOSS_FK_LAND,
        BOSS_FK_SWING,
        BOSS_FK_STRIKE_GROUND,
        BOSS_FK_ATTACK_MULTI, // For the generic "Attack New 01-05" sounds
        BOSS_FK_ARMOUR_HIT,   // When hitting the armour
        BOSS_FKNIGHT_HIT,     // When hitting the exposed maggot
        BOSS_FKNIGHT_RAGE,    // The tantrum phase
        BOSS_FKNIGHT_DEATH,

        ENTER_MENU,
        ENTER_CROSSROADS,
        ENTER_CRYSTAL_PEAK,
        ENTER_BOSS,

        BREAKABLE_WALL_HIT,
        BREAKABLE_WALL_BROKEN,
    }

    private static final Map<Event, List<Consumer<Object>>> listeners = new HashMap<>();

    public static void subscribe(Event event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public static void unsubscribe(Event event, Consumer<Object> listener) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    public static void emit(Event event) {
        emit(event, null);
    }

    public static void emit(Event event, Object data) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null) new ArrayList<>(list).forEach(l -> l.accept(data));
    }

    public static void clear() {
        listeners.clear();
    }
}
