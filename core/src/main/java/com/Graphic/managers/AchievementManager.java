package com.Graphic.managers;

import com.Graphic.models.Achievement;
import com.Graphic.models.AchievementId;
import com.Graphic.models.charms.CharmId;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AchievementManager {

    private static AchievementStore store;
    private static final Map<AchievementId, Achievement> achievements = new EnumMap<>(AchievementId.class);



    private static final Set<String> killedEnemyTypes = new HashSet<>();
    private static final int TOTAL_ENEMY_TYPES = 4;


    private static final Set<CharmId> collectedCharms = new HashSet<>();
    private static final int TOTAL_COLLECTABLE_CHARMS = 1;



    private static final float SPEEDRUN_TARGET_SECONDS = 20f * 60f;

    public static void init() {
        store = new PreferencesAchievementStore();

        register(AchievementId.COMPLETION,
            "achievement.completion.title", "achievement.completion.description",
            "ui/achievements/completion.png");
        register(AchievementId.SPEEDRUN,
            "achievement.speedrun.title", "achievement.speedrun.description",
            "ui/achievements/speedrun.png");
        register(AchievementId.TRUE_HUNTER,
            "achievement.true_hunter.title", "achievement.true_hunter.description",
            "ui/achievements/true hunter.png");
        register(AchievementId.DEFEAT_FALSE_KNIGHT,
            "achievement.defeat_false_knight.title", "achievement.defeat_false_knight.description",
            "ui/achievements/false knight.png");
        register(AchievementId.CHARM_MASTER,
            "achievement.charm_master.title", "achievement.charm_master.description",
            "ui/achievements/charm master.png");

        subscribeToEvents();
    }

    private static void register(AchievementId id, String titleKey, String descriptionKey, String iconPath) {
        Texture icon = new Texture(Gdx.files.internal(iconPath));
        boolean unlocked = store.isUnlocked(id);
        achievements.put(id, new Achievement(id, titleKey, descriptionKey, icon, unlocked));
    }

    private static void subscribeToEvents() {
        EventBus.subscribe(EventBus.Event.BOSS_FKNIGHT_DEATH, e -> unlock(AchievementId.DEFEAT_FALSE_KNIGHT));
        EventBus.subscribe(EventBus.Event.GAME_COMPLETED,     AchievementManager::onGameCompleted);
        EventBus.subscribe(EventBus.Event.ENEMY_KILLED,       AchievementManager::onEnemyKilled);
        EventBus.subscribe(EventBus.Event.CHARM_COLLECTED,    AchievementManager::onCharmCollected);
    }


    private static void onGameCompleted(Object data) {
        unlock(AchievementId.COMPLETION);
        if (data instanceof Float elapsedSeconds && elapsedSeconds <= SPEEDRUN_TARGET_SECONDS) {
            unlock(AchievementId.SPEEDRUN);
        }
    }


    private static void onEnemyKilled(Object data) {
        if (data instanceof String enemyType) {
            killedEnemyTypes.add(enemyType);
            if (TOTAL_ENEMY_TYPES > 0 && killedEnemyTypes.size() >= TOTAL_ENEMY_TYPES) {
                unlock(AchievementId.TRUE_HUNTER);
            }
        }
    }


    private static void onCharmCollected(Object data) {
            if(data instanceof CharmId) {
                collectedCharms.add((CharmId)data);
                if (collectedCharms.size() >= TOTAL_COLLECTABLE_CHARMS) {
                    unlock(AchievementId.CHARM_MASTER);
                }
            }
    }

    private static void unlock(AchievementId id) {
        Achievement achievement = achievements.get(id);
        if (achievement == null || achievement.isUnlocked()) return;

        achievement.setUnlocked(true);
        store.setUnlocked(id, true);
        EventBus.emit(EventBus.Event.ACHIEVEMENT_UNLOCKED, achievement);
    }


    public static Array<Achievement> getAll() {
        Array<Achievement> list = new Array<>();
        for (AchievementId id : AchievementId.values()) {
            list.add(achievements.get(id));
        }
        return list;
    }

    public static void dispose() {
        for (Achievement a : achievements.values()) {
            a.getIcon().dispose();
        }
        achievements.clear();
        killedEnemyTypes.clear();
        collectedCharms.clear();
    }
}
