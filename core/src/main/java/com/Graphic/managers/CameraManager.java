package com.Graphic.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

// CameraManager.java - set this up before any game rendering
public class CameraManager {
    private OrthographicCamera camera;
    private Vector2 bounds;      // map boundaries for clamping
    private float lerpSpeed = 5f;

    public void update(float targetX, float targetY, float delta) {
        // lerp toward player
        camera.position.x += (targetX - camera.position.x) * lerpSpeed * delta;
        camera.position.y += (targetY - camera.position.y) * lerpSpeed * delta;
        clamp();
        camera.update();
    }

    // screen shake lives here too
    public void shake(float intensity, float duration) {  }

    private void clamp() {  }
}
