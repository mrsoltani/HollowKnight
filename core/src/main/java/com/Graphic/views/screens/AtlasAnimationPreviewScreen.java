package com.Graphic.views.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class AtlasAnimationPreviewScreen extends ScreenAdapter {

    private final com.badlogic.gdx.Game game;
    private SpriteBatch batch;
    private TextureAtlas atlas;
    private Texture containerTexture;
    private Texture fullSoulTexture;
    private TextureRegion fullSoulBottomHalf;
    private ShaderProgram circleMaskShader;

    private ObjectMap<String, Animation<TextureRegion>> animations;
    private float stateTime;
    private float animStateTime;

    private OrthographicCamera camera;
    private Viewport viewport;


    private static final float DISPLAY_SIZE = 120f;
    private static final float TOP_MARGIN   = 30f;
    private static final float LEFT_MARGIN  = 30f;
    private float orbX, orbY;


    private final float maskCenterX = 0.5f;
    private final float maskCenterY = 0.4f;
    private final float maskRadius  = 0.42f;


    private static final float EMPTY_PAN_Y = -0.7f;
    private static final float FULL_PAN_Y  = 0.3f;
    private float panY = EMPTY_PAN_Y;
    private static final float PAN_EASE_SPEED = 3f;


    private static final int MAX_LEVEL = 4;
    private int currentLevel = 0;


    private enum AnimState { IDLE, GROWING, SHRINKING }
    private AnimState animState = AnimState.IDLE;


    private Texture faceReferenceTexture;
    private static final float FULL_SOUL_IMG_SIZE = 112f;
    private static final int HALF_MARK_LEVEL = MAX_LEVEL / 2;


    private static final float CONTAINER_IMG_WIDTH  = 367f;
    private static final float CONTAINER_IMG_HEIGHT = 239f;
    private static final float CONTAINER_CIRCLE_CENTER_X_PX             = 105.93f;
    private static final float CONTAINER_CIRCLE_CENTER_Y_FROM_BOTTOM_PX = 61.47f;
    private static final float CONTAINER_CIRCLE_RADIUS_PX               = 86f;
    private float containerX, containerY, containerW, containerH;


    private static final float FACE_OFFSET_X = -8f;
    private static final float FACE_OFFSET_Y = 28f;
    private static final float FACE_SCALE_MULTIPLIER = 1f;
    private static final boolean CENTER_ON_HALF_SHAPE = false;

    public AtlasAnimationPreviewScreen(com.badlogic.gdx.Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);

        circleMaskShader = buildCircleMaskShader();

        animations = new ObjectMap<>();
        stateTime = 0f;
        animStateTime = 0f;

        atlas = new TextureAtlas(Gdx.files.internal("ui/soul/Soulorb.atlas"));

        ObjectMap<String, Array<TextureRegion>> grouped = new ObjectMap<>();
        for (AtlasRegion region : atlas.getRegions()) {
            String baseName = region.name.replaceAll("\\d+$", "");
            if (!grouped.containsKey(baseName)) {
                grouped.put(baseName, new Array<TextureRegion>());
            }
            grouped.get(baseName).add(region);
        }

        Array<TextureRegion> growFrames   = grouped.get("HUD_Soulorb_fills_soul_grow");
        Array<TextureRegion> idleFrames   = grouped.get("HUD_Soulorb_fills_soul_idle");
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

        computeHudPosition();
        computeContainerTransform();
    }

    private void computeHudPosition() {
        orbX = LEFT_MARGIN;
        orbY = Gdx.graphics.getHeight() - TOP_MARGIN - DISPLAY_SIZE;
    }

    private void computeContainerTransform() {
        float maskScreenCenterX = orbX + maskCenterX * DISPLAY_SIZE;
        float maskScreenCenterY = orbY + maskCenterY * DISPLAY_SIZE;
        float maskScreenRadius  = maskRadius * DISPLAY_SIZE;

        float scale = maskScreenRadius / CONTAINER_CIRCLE_RADIUS_PX;

        containerW = CONTAINER_IMG_WIDTH * scale;
        containerH = CONTAINER_IMG_HEIGHT * scale;

        containerX = maskScreenCenterX - CONTAINER_CIRCLE_CENTER_X_PX * scale;
        containerY = maskScreenCenterY - CONTAINER_CIRCLE_CENTER_Y_FROM_BOTTOM_PX * scale;
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



    private void riseLevel() {
        if (currentLevel >= MAX_LEVEL) return;
        currentLevel++;
        animState = AnimState.GROWING;
        animStateTime = 0f;
    }

    private void drainLevel() {
        if (currentLevel <= 0) return;
        currentLevel--;
        animState = AnimState.SHRINKING;
        animStateTime = 0f;
    }

    private float targetPanYForLevel(int level) {
        float t = level / (float) MAX_LEVEL;
        return MathUtils.lerp(EMPTY_PAN_Y, FULL_PAN_Y, t);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stateTime += delta;
        animStateTime += delta;


        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   riseLevel();
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) drainLevel();


        float targetPanY = targetPanYForLevel(currentLevel);
        panY = MathUtils.lerp(panY, targetPanY, Math.min(1f, PAN_EASE_SPEED * delta));


        Animation<TextureRegion> growAnim = animations.get("grow");
        Animation<TextureRegion> shrinkAnim = animations.get("shrink");
        if (animState == AnimState.GROWING && growAnim.isAnimationFinished(animStateTime)) {
            animState = AnimState.IDLE;
        } else if (animState == AnimState.SHRINKING && shrinkAnim.isAnimationFinished(animStateTime)) {
            animState = AnimState.IDLE;
        }

        TextureRegion frame;
        switch (animState) {
            case GROWING:   frame = growAnim.getKeyFrame(animStateTime);   break;
            case SHRINKING: frame = shrinkAnim.getKeyFrame(animStateTime); break;
            case IDLE:
            default:        frame = animations.get("idle").getKeyFrame(stateTime); break;
        }

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();


        batch.setShader(null);
        batch.draw(containerTexture, containerX, containerY, containerW, containerH);


        batch.setShader(circleMaskShader);
        circleMaskShader.setUniformf("u_uvMin", frame.getU(), frame.getV());
        circleMaskShader.setUniformf("u_uvMax", frame.getU2(), frame.getV2());
        circleMaskShader.setUniformf("u_maskCenter", maskCenterX, maskCenterY);
        circleMaskShader.setUniformf("u_radius", maskRadius);
        circleMaskShader.setUniformf("u_panOffset", 0f, panY);
        batch.draw(frame, orbX, orbY, DISPLAY_SIZE, DISPLAY_SIZE);


        if (currentLevel >= HALF_MARK_LEVEL) {
            batch.setShader(null);

            float maskScreenCenterX = orbX + maskCenterX * DISPLAY_SIZE;
            float maskScreenCenterY = orbY + maskCenterY * DISPLAY_SIZE;
            float faceDiameter = maskRadius * DISPLAY_SIZE * 2f * FACE_SCALE_MULTIPLIER;
            float faceHeight = faceDiameter / 2f;

            float faceX = maskScreenCenterX - faceDiameter / 2f + FACE_OFFSET_X;
            float faceY = CENTER_ON_HALF_SHAPE
                ? maskScreenCenterY - faceHeight / 2f + FACE_OFFSET_Y
                : maskScreenCenterY - faceHeight + FACE_OFFSET_Y;

            batch.draw(fullSoulBottomHalf, faceX, faceY, faceDiameter, faceHeight);
        }

        batch.end();
        batch.setShader(null);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        computeHudPosition();
        computeContainerTransform();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (atlas != null) atlas.dispose();
        if (containerTexture != null) containerTexture.dispose();
        if (fullSoulTexture != null) fullSoulTexture.dispose();
        if (circleMaskShader != null) circleMaskShader.dispose();
    }
}
