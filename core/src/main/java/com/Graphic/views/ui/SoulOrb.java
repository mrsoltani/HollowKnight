package com.Graphic.views.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

public class SoulOrb {

    private static final int MIN_SOUL = 0;
    private static final int MAX_SOUL = 99;
    private static final int HALF_MARK_SOUL = 50;

    private static final float DISPLAY_SIZE = 120f;
    private static final float TOP_MARGIN = 50f;
    private static final float LEFT_MARGIN = 30f;

    private static final float MASK_CENTER_X = 0.5f;
    private static final float MASK_CENTER_Y = 0.4f;
    private static final float MASK_RADIUS = 0.42f;

    private static final float EMPTY_PAN_Y = -0.72f;
    private static final float FULL_PAN_Y = 0.2f;
    private static final float PAN_EASE_SPEED = 3f;

    private static final float FULL_SOUL_IMG_SIZE = 112f;
    private static final float CONTAINER_IMG_WIDTH = 367f;
    private static final float CONTAINER_IMG_HEIGHT = 239f;
    private static final float CONTAINER_CIRCLE_CENTER_X_PX = 105.93f;
    private static final float CONTAINER_CIRCLE_CENTER_Y_FROM_BOTTOM_PX = 61.47f;
    private static final float CONTAINER_CIRCLE_RADIUS_PX = 86f;

    private static final float FACE_OFFSET_X = -8f;
    private static final float FACE_OFFSET_Y = 28f;
    private static final float FACE_SCALE_MULTIPLIER = 1f;
    private static final boolean CENTER_ON_HALF_SHAPE = false;

    private static final String STATIC_PATCH_COLOR_HEX = "f3e7eb";
    private static final float STATIC_PATCH_MIN_X = 0.15f;
    private static final float STATIC_PATCH_MAX_X = 0.85f;
    private static final float STATIC_PATCH_MIN_Y = 0.6f;
    private static final float STATIC_PATCH_MAX_Y = 1.0f;

    private enum AnimState { IDLE, GROWING, SHRINKING }

    private TextureAtlas atlas;
    private Texture containerTexture;
    private Texture fullSoulTexture;
    private TextureRegion fullSoulBottomHalf;
    private ShaderProgram circleMaskShader;
    private ShaderProgram rectPatchShader;
    private Texture whiteTexture;
    private Color staticPatchColor;

    private ObjectMap<String, Animation<TextureRegion>> animations;
    private float stateTime;
    private float animStateTime;
    private AnimState animState = AnimState.IDLE;

    private int soul = 0;
    private float panY = EMPTY_PAN_Y;

    private float orbX, orbY;
    private float containerX, containerY, containerW, containerH;

    public SoulOrb() {
        circleMaskShader = buildCircleMaskShader();
        rectPatchShader = buildRectPatchShader();
        animations = new ObjectMap<>();

        atlas = new TextureAtlas(Gdx.files.internal("ui/soul/Soulorb.atlas"));

        ObjectMap<String, Array<TextureRegion>> grouped = new ObjectMap<>();
        for (AtlasRegion region : atlas.getRegions()) {
            String baseName = region.name.replaceAll("\\d+$", "");
            if (!grouped.containsKey(baseName)) {
                grouped.put(baseName, new Array<TextureRegion>());
            }
            grouped.get(baseName).add(region);
        }

        Array<TextureRegion> growFrames = grouped.get("HUD_Soulorb_fills_soul_grow");
        Array<TextureRegion> idleFrames = grouped.get("HUD_Soulorb_fills_soul_idle");
        Array<TextureRegion> shrinkFrames = grouped.get("HUD_Soulorb_fills_soul_shrink");

        if (growFrames == null || idleFrames == null || shrinkFrames == null) {
            throw new GdxRuntimeException("Missing expected soul animation group(s) — check atlas region names.");
        }

        animations.put("grow", new Animation<>(1f / 24f, growFrames, Animation.PlayMode.NORMAL));
        animations.put("idle", new Animation<>(0.1f, idleFrames, Animation.PlayMode.LOOP));
        animations.put("shrink", new Animation<>(1f / 24f, shrinkFrames, Animation.PlayMode.NORMAL));

        containerTexture = new Texture(Gdx.files.internal("ui/soul/SoulContainer.png"));
        containerTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        fullSoulTexture = new Texture(Gdx.files.internal("ui/soul/FullSoul.png"));
        fullSoulTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        int fullSize = (int) FULL_SOUL_IMG_SIZE;
        fullSoulBottomHalf = new TextureRegion(fullSoulTexture, 0, fullSize / 2, fullSize, fullSize / 2);

        staticPatchColor = Color.valueOf(STATIC_PATCH_COLOR_HEX);
        whiteTexture = buildSolidWhiteTexture();

        computeHudPosition();
        computeContainerTransform();
    }

    private void computeHudPosition() {
        orbX = LEFT_MARGIN;
        orbY = Gdx.graphics.getHeight() - TOP_MARGIN - DISPLAY_SIZE;
    }

    private void computeContainerTransform() {
        float maskScreenCenterX = orbX + MASK_CENTER_X * DISPLAY_SIZE;
        float maskScreenCenterY = orbY + MASK_CENTER_Y * DISPLAY_SIZE;
        float maskScreenRadius = MASK_RADIUS * DISPLAY_SIZE;

        float scale = maskScreenRadius / CONTAINER_CIRCLE_RADIUS_PX;

        containerW = CONTAINER_IMG_WIDTH * scale;
        containerH = CONTAINER_IMG_HEIGHT * scale;

        containerX = maskScreenCenterX - CONTAINER_CIRCLE_CENTER_X_PX * scale;
        containerY = maskScreenCenterY - CONTAINER_CIRCLE_CENTER_Y_FROM_BOTTOM_PX * scale;
    }

    private Texture buildSolidWhiteTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private ShaderProgram buildRectPatchShader() {
        ShaderProgram defaultShader = SpriteBatch.createDefaultShader();
        String vertexShader = defaultShader.getVertexShaderSource();
        defaultShader.dispose();

        String fragmentShader = "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform vec2 u_maskCenter;\n"
            + "uniform float u_radius;\n"
            + "uniform vec2 u_rectMin;\n"
            + "uniform vec2 u_rectMax;\n"
            + "void main() {\n"
            + "  vec2 local = v_texCoords;\n"
            + "  float dist = length(local - u_maskCenter);\n"
            + "  float circleEdge = smoothstep(u_radius, u_radius - 0.02, dist);\n"
            + "  float insideX = step(u_rectMin.x, local.x) * step(local.x, u_rectMax.x);\n"
            + "  float insideY = step(u_rectMin.y, local.y) * step(local.y, u_rectMax.y);\n"
            + "  float rectMask = insideX * insideY;\n"
            + "  vec4 texColor = texture2D(u_texture, v_texCoords);\n"
            + "  gl_FragColor = vec4(v_color.rgb * texColor.rgb, v_color.a * texColor.a * circleEdge * rectMask);\n"
            + "}";
        ShaderProgram.pedantic = false;
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            throw new GdxRuntimeException("Rect patch shader failed to compile:\n" + shader.getLog());
        }
        return shader;
    }

    private ShaderProgram buildCircleMaskShader() {
        ShaderProgram defaultShader = SpriteBatch.createDefaultShader();
        String vertexShader = defaultShader.getVertexShaderSource();
        defaultShader.dispose();

        String fragmentShader = "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform vec2 u_uvMin;\n"
            + "uniform vec2 u_uvMax;\n"
            + "uniform vec2 u_maskCenter;\n"
            + "uniform float u_radius;\n"
            + "uniform vec2 u_panOffset;\n"
            + "void main() {\n"
            + "  vec2 local = (v_texCoords - u_uvMin) / (u_uvMax - u_uvMin);\n"
            + "  float dist = length(local - u_maskCenter);\n"
            + "  float edge = smoothstep(u_radius, u_radius - 0.02, dist);\n"
            + "  vec2 sampleLocal = clamp(local + u_panOffset, 0.0, 1.0);\n"
            + "  vec2 sampleUV = u_uvMin + sampleLocal * (u_uvMax - u_uvMin);\n"
            + "  vec4 texColor = texture2D(u_texture, sampleUV);\n"
            + "  gl_FragColor = vec4(v_color.rgb * texColor.rgb, v_color.a * texColor.a * edge);\n"
            + "}";
        ShaderProgram.pedantic = false;
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            throw new GdxRuntimeException("Circle mask shader failed to compile:\n" + shader.getLog());
        }
        return shader;
    }

    public void addSoul(int amount) {
        setSoul(soul + amount);
    }

    public void removeSoul(int amount) {
        setSoul(soul - amount);
    }

    public void setSoul(int amount) {
        int clamped = MathUtils.clamp(amount, MIN_SOUL, MAX_SOUL);
        if (clamped == soul) return;

        AnimState newState = clamped > soul ? AnimState.GROWING : AnimState.SHRINKING;
        soul = clamped;
        animState = newState;
        animStateTime = 0f;
    }

    public int getSoul() {
        return soul;
    }

    public void resize(int width, int height) {
        computeHudPosition();
        computeContainerTransform();
    }

    public void update(float delta) {
        stateTime += delta;
        animStateTime += delta;

        float t = soul / (float) MAX_SOUL;
        float targetPanY = MathUtils.lerp(EMPTY_PAN_Y, FULL_PAN_Y, t);
        panY = MathUtils.lerp(panY, targetPanY, Math.min(1f, PAN_EASE_SPEED * delta));

        Animation<TextureRegion> growAnim = animations.get("grow");
        Animation<TextureRegion> shrinkAnim = animations.get("shrink");
        if (animState == AnimState.GROWING && growAnim.isAnimationFinished(animStateTime)) {
            animState = AnimState.IDLE;
        } else if (animState == AnimState.SHRINKING && shrinkAnim.isAnimationFinished(animStateTime)) {
            animState = AnimState.IDLE;
        }
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame;
        switch (animState) {
            case GROWING:
                frame = animations.get("grow").getKeyFrame(animStateTime);
                break;
            case SHRINKING:
                frame = animations.get("shrink").getKeyFrame(animStateTime);
                break;
            case IDLE:
            default:
                frame = animations.get("idle").getKeyFrame(stateTime);
                break;
        }

        batch.setShader(null);
        batch.draw(containerTexture, containerX, containerY, containerW, containerH);

        batch.setShader(circleMaskShader);
        circleMaskShader.setUniformf("u_uvMin", frame.getU(), frame.getV());
        circleMaskShader.setUniformf("u_uvMax", frame.getU2(), frame.getV2());
        circleMaskShader.setUniformf("u_maskCenter", MASK_CENTER_X, MASK_CENTER_Y);
        circleMaskShader.setUniformf("u_radius", MASK_RADIUS);
        circleMaskShader.setUniformf("u_panOffset", 0f, panY);
        batch.draw(frame, orbX, orbY, DISPLAY_SIZE, DISPLAY_SIZE);

        if (soul >= HALF_MARK_SOUL) {
            batch.setShader(rectPatchShader);
            rectPatchShader.setUniformf("u_maskCenter", MASK_CENTER_X, MASK_CENTER_Y);
            rectPatchShader.setUniformf("u_radius", MASK_RADIUS);
            rectPatchShader.setUniformf("u_rectMin", STATIC_PATCH_MIN_X, STATIC_PATCH_MIN_Y);
            rectPatchShader.setUniformf("u_rectMax", STATIC_PATCH_MAX_X, STATIC_PATCH_MAX_Y);
            batch.setColor(staticPatchColor);
            batch.draw(whiteTexture, orbX, orbY, DISPLAY_SIZE, DISPLAY_SIZE);
            batch.setColor(Color.WHITE);
            batch.setShader(null);

            float maskScreenCenterX = orbX + MASK_CENTER_X * DISPLAY_SIZE;
            float maskScreenCenterY = orbY + MASK_CENTER_Y * DISPLAY_SIZE;

            float faceDiameter = MASK_RADIUS * DISPLAY_SIZE * 2f * FACE_SCALE_MULTIPLIER;
            float faceHeight = faceDiameter / 2f;

            float faceX = maskScreenCenterX - faceDiameter / 2f + FACE_OFFSET_X;
            float faceY = CENTER_ON_HALF_SHAPE
                ? maskScreenCenterY - faceHeight / 2f + FACE_OFFSET_Y
                : maskScreenCenterY - faceHeight + FACE_OFFSET_Y;

            batch.draw(fullSoulBottomHalf, faceX, faceY, faceDiameter, faceHeight);
        }

        batch.setShader(null);
    }

    public void dispose() {
        if (atlas != null) atlas.dispose();
        if (containerTexture != null) containerTexture.dispose();
        if (fullSoulTexture != null) fullSoulTexture.dispose();
        if (circleMaskShader != null) circleMaskShader.dispose();
        if (rectPatchShader != null) rectPatchShader.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
    }
}
