package com.Graphic.models.enemies;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;


public interface EnemyAI {


    void update(float delta, Rectangle target, Array<Rectangle> platforms);


    void render(SpriteBatch batch);


    void renderDebug(ShapeRenderer shapes);


    Rectangle getHitbox();


    boolean isDead();


    void dispose();
}
