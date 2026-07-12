package com.Graphic.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class BossDoor {

    private static final float DROP_SPEED = 600f;

    private final Texture texture;
    private final Vector2 openPos;
    private final Vector2 closedPos;

    private float   currentY;
    private boolean dropping = false;
    private boolean closed   = false;

    public BossDoor(Texture texture, Vector2 openPos, Vector2 closedPos) {
        this.texture   = texture;
        this.openPos   = openPos;
        this.closedPos = closedPos;
        this.currentY  = openPos.y;
        this.openPos.y=1080-this.openPos.y;
        this.closedPos.y=1080- this.closedPos.y;
        this.currentY  = openPos.y;
    }

    public void trigger() {
        if (!closed && !dropping) {
            Gdx.app.log("BossDoor", "open=" + openPos + " closed=" + closedPos + " currentY=" + currentY);
            dropping = true;
        }


    }

    public void update(float delta) {
        if (!dropping || closed) return;

        currentY -= DROP_SPEED * delta;
        if (currentY <= closedPos.y) {
            currentY = closedPos.y;
            dropping = false;
            closed   = true;
        }
    }

    public void render(SpriteBatch batch) {
        float drawX = openPos.x - texture.getWidth()  / 2f;
        batch.draw(texture, drawX, currentY, texture.getWidth(), texture.getHeight());
    }


    public boolean isClosed()  { return closed;   }
    public boolean isDropping(){ return dropping; }


    public com.badlogic.gdx.math.Rectangle getBounds() {
        return new com.badlogic.gdx.math.Rectangle(
            openPos.x - texture.getWidth() / 2f,
            currentY,
            texture.getWidth(),
            texture.getHeight()
        );
    }
}
