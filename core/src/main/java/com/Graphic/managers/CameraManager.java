package com.Graphic.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class CameraManager {

    private static OrthographicCamera camera;
    private static Vector2 mapBounds;

    // Shake State
    private static float shakeIntensity = 0f;
    private static float shakeDuration = 0f;
    private static float shakeTimer = 0f;

    public static void init(OrthographicCamera cam, Vector2 bounds) {
        camera = cam;
        mapBounds = bounds;
    }

    public static void shake(float intensity, float duration) {
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTimer = duration;
    }

    public static void update(float targetX, float targetY, float delta) {
        if (camera == null) return;

        // Smooth camera lerp tracking (adjust 0.1f to change camera weight)
        float newX = MathUtils.lerp(camera.position.x, targetX, 0.1f);
        float newY = MathUtils.lerp(camera.position.y, targetY, 0.1f);

        // Clamp camera positions within map dimensions (accounting for viewport halves)
        float halfViewportX = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewportY = (camera.viewportHeight * camera.zoom) / 2f;

        if (mapBounds != null) {
            newX = MathUtils.clamp(newX, halfViewportX, mapBounds.x - halfViewportX);
            newY = MathUtils.clamp(newY, halfViewportY, mapBounds.y - halfViewportY);
        }

        camera.position.set(newX, newY, 0);

        // Apply screen shake offsets
        if (shakeTimer > 0) {
            // Taper the intensity off linearly as the timer drops
            float currentIntensity = shakeIntensity * (shakeTimer / shakeDuration);
            camera.position.x += MathUtils.random(-currentIntensity, currentIntensity);
            camera.position.y += MathUtils.random(-currentIntensity, currentIntensity);
            shakeTimer -= delta;
        }

        camera.update();
    }
}
