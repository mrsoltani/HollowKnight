package com.Graphic.managers;

import com.Graphic.models.AchievementId;

public interface AchievementStore {
    boolean isUnlocked(AchievementId id);
    void setUnlocked(AchievementId id, boolean unlocked);
}
