package com.Graphic;

import com.Graphic.managers.*;
import com.Graphic.models.enums.GameViewScreen;
import com.Graphic.views.screens.*;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Main extends Game {
    public static Main instance;

    public SpriteBatch batch;

    // --- Global Brightness & FBO Variables ---
    private FrameBuffer fbo;
    private SpriteBatch fboBatch;
    private OrthographicCamera fboCamera;
    private ShaderProgram brightnessShader;

    private boolean inGame = false;

    public Main() {
        instance = this;
    }

    public static Main getInstance() {
        if (instance == null) {
            instance = new Main();
        }
        return instance;
    }

    public void setScreen(GameViewScreen screen){
        Gdx.app.log("NAV", "setScreen(" + screen + ") called — was on: " + this.getScreen());
        this.setScreen(screen.getScreen());
        Gdx.app.log("NAV", "after switch, active screen is: " + this.getScreen());
    }
    public boolean isInGame() { return inGame; }
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
        if (!inGame) {
            EventBus.emit(EventBus.Event.ENTER_MENU);
        }
    }

    @Override
    public void create() {
        InputManager.init();
        FontManager.load();
        LocalizationManager.load();
        AudioManager.init();
        VideoManager.init();
        DialogueManager.load();
        CharmManager.init();
        EffectManager.init();
        AchievementManager.init();
        SaveManager.init();


        batch = new SpriteBatch();
        fboBatch = new SpriteBatch();
        fboCamera = new OrthographicCamera();

        // Initialize the Brightness Shader
        ShaderProgram.pedantic = false; // avoid crashes on unused varyings/uniforms across GL drivers

        ShaderProgram defaultShader = SpriteBatch.createDefaultShader();
        String vertexShader = defaultShader.getVertexShaderSource();
        defaultShader.dispose(); // don't leak the throwaway shader

        String fragmentShader = "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform float u_brightness;\n"
            + "void main() {\n"
            + "  vec4 texColor = texture2D(u_texture, v_texCoords);\n"
            + "  vec3 brightColor = texColor.rgb + vec3(u_brightness);\n"
            + "  gl_FragColor = v_color * vec4(clamp(brightColor, 0.0, 1.0), texColor.a);\n"
            + "}";

        brightnessShader = new ShaderProgram(vertexShader, fragmentShader);
        if (!brightnessShader.isCompiled()) {
            Gdx.app.error("Shader Error", brightnessShader.getLog());
            throw new GdxRuntimeException("Brightness shader failed to compile:\n" + brightnessShader.getLog());
        }

        Pixmap cursorImage = new Pixmap(Gdx.files.internal("ui/Cursor.png"));
        Cursor cursor = Gdx.graphics.newCursor(cursorImage, 0, 0);
        Gdx.graphics.setCursor(cursor);
        cursorImage.dispose();

        //SaveManager.currentSave = SaveManager.loadSlot(1);
        Main.getInstance().setScreen(GameViewScreen.MainMenu);

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // Guard against LibGDX's zero-pixel crash on minimize/startup
        if (width <= 0 || height <= 0) return;

        // Use backbuffer size (matters on HiDPI/Retina displays where it can
        // differ from the logical window size passed in here)
        int bbWidth = Gdx.graphics.getBackBufferWidth();
        int bbHeight = Gdx.graphics.getBackBufferHeight();

        // Update the FBO camera to perfectly match the new backbuffer size
        fboCamera.setToOrtho(false, bbWidth, bbHeight);

        // Rebuild the FrameBuffer to match the new screen dimensions
        if (fbo != null) {
            fbo.dispose();
        }
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, bbWidth, bbHeight, false);
    }

    @Override
    public void render() {
        if (fbo == null) return; // Safety check in case render is called before resize finishes

        // 1. Redirect all standard rendering to the FBO
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render(); // This draws your active GameScreen/MenuScreen

        fbo.end();

        // 2. Draw the FBO to the actual screen using the shader
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Extract the texture and flip it (FBOs render upside down by default)
        TextureRegion fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        // Apply the camera matrix and shader to the FBO batch
        fboBatch.setProjectionMatrix(fboCamera.combined );
        fboBatch.setShader(brightnessShader);
        fboBatch.begin();

        brightnessShader.setUniformf("u_brightness", VideoManager.getBrightness());

        // Draw the cleanly flipped texture starting at 0,0 stretching to the window size
        fboBatch.draw(fboRegion, 0, 0, fboCamera.viewportWidth, fboCamera.viewportHeight);

        fboBatch.end();
        fboBatch.setShader(null);
        AudioManager.update(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (fboBatch != null) fboBatch.dispose();
        if (fbo != null) fbo.dispose();
        if (brightnessShader != null) brightnessShader.dispose();
        EffectManager.dispose();
        FontManager.dispose();
        AchievementManager.dispose();
    }
}
