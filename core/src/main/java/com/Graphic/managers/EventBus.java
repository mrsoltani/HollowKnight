package com.Graphic.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {

    public enum Event {

        MENU_NAVIGATE,
        MENU_SELECT,
        MUSIC_CHANGED,
        SFX_CHANGED,
        GAME_PAUSED,
        GAME_COMPLETED,
        ACHIEVEMENT_UNLOCKED,
        CHARM_COLLECTED,

        PLAYER_WALK,
        PLAYER_RUN,
        PLAYER_STOP_WALK,
        PLAYER_JUMP,
        PLAYER_DOUBLE_JUMP,
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


        ENEMY_HIT,
        ENEMY_KILLED,
        ENEMY_MOSQUITO_ATTACK,
        ENEMY_GROUND_STEP,
        ENEMY_CRYSTAL_LASER_CHARGE,
        ENEMY_CRYSTAL_LASER_START,
        ENEMY_CRYSTAL_LASER_STOP,


        BOSS_FK_JUMP,
        BOSS_FK_LAND,
        BOSS_FK_SWING,
        BOSS_FK_STRIKE_GROUND,
        BOSS_FK_ATTACK_MULTI,
        BOSS_FK_ARMOUR_HIT,
        BOSS_FKNIGHT_HIT,
        BOSS_FKNIGHT_RAGE,
        BOSS_FKNIGHT_DEATH,

        ENTER_MENU,
        ENTER_CROSSROADS,
        ENTER_CRYSTAL_PEAK,
        ENTER_BOSS,
        ENTER_END,

        BREAKABLE_WALL_HIT,
        BREAKABLE_WALL_BROKEN,


        FALSE_KNIGHT_ATTACK_WINDUP,
        FALSE_KNIGHT_JUMP_TAKEOFF,
        FALSE_KNIGHT_JUMP_LAND,
        FALSE_KNIGHT_SLAM_IMPACT,
        FALSE_KNIGHT_STUN_ENTER,
        FALSE_KNIGHT_STUN_RECOVER,
        FALSE_KNIGHT_DEATH,
        FALSE_KNIGHT_HIT,
        FALSE_KNIGHT_HIT_PHASE2,
        FALSE_KNIGHT_RAGE,
        FALSE_KNIGHT_CEILING_BREAK,
        FALSE_KNIGHT_LAND_FIRST,
        FALSE_KNIGHT_CHARGE_SWING
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
