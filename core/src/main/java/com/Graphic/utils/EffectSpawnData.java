package com.Graphic.utils;

/**
 * Payload for EventBus effect events. The class publishing the event
 * (Player) knows where the effect happened; the class consuming it
 * (EffectManager) just needs this to know where/how to draw.
 */
public record EffectSpawnData(float x, float y, boolean facingRight) {}
