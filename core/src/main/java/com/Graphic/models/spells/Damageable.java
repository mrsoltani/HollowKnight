package com.Graphic.models.spells;

import com.badlogic.gdx.math.Rectangle;


public interface Damageable {
    Rectangle getBounds();
    void takeDamage(float amount, boolean fromRight);
}
