package com.Graphic.models.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;


public class AnimationComponent implements Disposable {


    private final Map<EnemyState, Animation<TextureRegion>> animations =
        new EnumMap<>(EnemyState.class);


    private final Array<Texture> ownedTextures = new Array<>();


    private float stateTime = 0f;






    public void register(EnemyState state, String assetPath,
                         int frameWidth, int frameHeight,
                         float frameDuration, Animation.PlayMode mode) {

        Texture sheet = new Texture(Gdx.files.internal(assetPath));
        ownedTextures.add(sheet);


        TextureRegion[][] grid = TextureRegion.split(sheet, frameWidth, frameHeight);


        int columns = grid[0].length;
        Array<TextureRegion> frames = new Array<>(columns);
        for (int col = 0; col < columns; col++) {
            frames.add(grid[0][col]);
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, mode);
        animations.put(state, animation);
    }






    public void update(float delta) {
        stateTime += delta;
    }


    public void resetStateTime() {
        stateTime = 0f;
    }

    public float getStateTime() {
        return stateTime;
    }


    public boolean isFinished(EnemyState state) {
        Animation<TextureRegion> anim = animations.get(state);
        return anim != null && anim.isAnimationFinished(stateTime);
    }

    public boolean hasState(EnemyState state) {
        return animations.containsKey(state);
    }


    public TextureRegion getFrame(EnemyState state, boolean facingRight) {
        Animation<TextureRegion> anim = animations.get(state);
        if (anim == null) {


            for (Animation<TextureRegion> any : animations.values()) {
                anim = any;
                break;
            }
            if (anim == null) return null;
        }

        TextureRegion frame = anim.getKeyFrame(stateTime);


        boolean shouldFlip = facingRight;
        if (frame.isFlipX() != shouldFlip) {
            frame.flip(true, false);
        }
        return frame;
    }






    @Override
    public void dispose() {
        for (Texture texture : ownedTextures) {
            texture.dispose();
        }
        ownedTextures.clear();
        animations.clear();
    }
}
