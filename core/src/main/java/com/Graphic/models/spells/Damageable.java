package com.Graphic.models.spells;

import com.badlogic.gdx.math.Rectangle;

/**
 * Minimal contract any entity must satisfy to be damaged by player spells
 * (Fireball / Scream). Implement this on your enemy classes (e.g. ZoteNPC)
 * to make them spell-damageable:
 *
 *   public class ZoteNPC implements Damageable {
 *       public Rectangle getBounds() { return bounds; }
 *       public void takeDamage(float amount, boolean fromRight) { ... }
 *   }
 */
public interface Damageable {
    Rectangle getBounds();
    void takeDamage(float amount, boolean fromRight);
}
