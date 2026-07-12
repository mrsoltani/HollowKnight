package com.Graphic.views.atmosphere;

import com.badlogic.gdx.graphics.Color;

import static com.Graphic.utils.Constants.Menu.*;

public enum Theme {
    VOID(
        PATH_VOID_HEART_BACKGROUND,
        new Color(0.16f, 0.18f, 0.22f, 1f),
        0.4f, 0.65f,
        0.01f, 0.22f,
        65
    ),
    GREENPATH(
        PATH_GREEN_PATH_BACKGROUND,
        new Color(0.12f, 0.32f, 0.20f, 1f),
        0.35f, 0.55f,
        0.4f, 0.75f,
        80
    ),
    CRYSTAL_PEAK(
        PATH_CRYSTAL_PEAK_BACKGROUND,
        new Color(0.28f, 0.18f, 0.35f, 1f),
        0.3f, 0.5f,
        0.6f, 0.9f,
        50
    );

    final String bgPath;
    final Color fogColor;
    final float minFogAlpha, maxFogAlpha;
    final float minPartBright, maxPartBright;
    final int particleCount;

    Theme(String bgPath, Color fogColor, float minFogAlpha, float maxFogAlpha, float minPartBright, float maxPartBright, int particleCount) {
        this.bgPath = bgPath;
        this.fogColor = fogColor;
        this.minFogAlpha = minFogAlpha;
        this.maxFogAlpha = maxFogAlpha;
        this.minPartBright = minPartBright;
        this.maxPartBright = maxPartBright;
        this.particleCount = particleCount;
    }
}
