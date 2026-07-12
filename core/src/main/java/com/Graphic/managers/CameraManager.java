package com.Graphic.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CameraManager {

    private static OrthographicCamera camera;
    private static Vector2 mapBounds;
    private static Array<Rectangle> cameraZones;


    private static float shakeIntensity = 0f;
    private static float shakeDuration = 0f;
    private static float shakeTimer = 0f;

    public static void init(OrthographicCamera cam, Vector2 bounds) {
        camera = cam;
        mapBounds = bounds;
    }

    public static void setCameraZones(Array<Rectangle> zones) {
        cameraZones = zones;
    }

    public static void shake(float intensity, float duration) {
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTimer = duration;
    }

    public static void update(float targetX, float targetY, float delta) {
        if (camera == null) return;


        float halfViewportX = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewportY = (camera.viewportHeight * camera.zoom) / 2f;


        Rectangle activeZone = null;
        if (cameraZones != null) {
            for (Rectangle zone : cameraZones) {
                if (zone.contains(targetX, targetY)) {
                    activeZone = zone;
                    break;
                }
            }
        }


        float minX, maxX, minY, maxY;
        if (activeZone != null) {

            if (activeZone.width < camera.viewportWidth * camera.zoom) {
                minX = maxX = activeZone.x + activeZone.width / 2f;
            } else {
                minX = activeZone.x + halfViewportX;
                maxX = activeZone.x + activeZone.width - halfViewportX;
            }


            if (activeZone.height < camera.viewportHeight * camera.zoom) {
                minY = maxY = activeZone.y + activeZone.height / 2f;
            } else {
                minY = activeZone.y + halfViewportY;
                maxY = activeZone.y + activeZone.height - halfViewportY;
            }
        } else if (mapBounds != null) {

            minX = halfViewportX;
            maxX = mapBounds.x - halfViewportX;
            minY = halfViewportY;
            maxY = mapBounds.y - halfViewportY;
        } else {
            minX = maxX = targetX;
            minY = maxY = targetY;
        }


        float clampedTargetX = MathUtils.clamp(targetX, minX, maxX);
        float clampedTargetY = MathUtils.clamp(targetY, minY, maxY);


        float newX, newY;
        if (delta > 10f) {

            newX = clampedTargetX;
            newY = clampedTargetY;
        } else {

            newX = MathUtils.lerp(camera.position.x, clampedTargetX, 0.1f);
            newY = MathUtils.lerp(camera.position.y, clampedTargetY, 0.1f);
        }

        camera.position.set(newX, newY, 0);


        if (shakeTimer > 0) {
            float currentIntensity = shakeIntensity * (shakeTimer / shakeDuration);
            camera.position.x += MathUtils.random(-currentIntensity, currentIntensity);
            camera.position.y += MathUtils.random(-currentIntensity, currentIntensity);
            shakeTimer -= delta;
        }

        camera.update();
    }
}
