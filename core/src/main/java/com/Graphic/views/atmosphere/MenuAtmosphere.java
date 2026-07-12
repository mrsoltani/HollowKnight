package com.Graphic.views.atmosphere;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import static com.Graphic.utils.Constants.V_HEIGHT;
import static com.Graphic.utils.Constants.V_WIDTH;

import static com.Graphic.utils.Constants.Menu.PATH_VOID_HEART_BACKGROUND;
import static com.Graphic.utils.Constants.Menu.PATH_GREEN_PATH_BACKGROUND;
import static com.Graphic.utils.Constants.Menu.PATH_CRYSTAL_PEAK_BACKGROUND;
import static com.Graphic.utils.Constants.Menu.PATH_LIGHT_BEAM;


public class MenuAtmosphere implements Disposable {


    private static MenuAtmosphere instance=null;
    private Texture currentBackground;
    private final Texture beamTexture;
    private final Texture particleTexture;
    private final Texture fogTexture;

    private final Array<VoidParticle> particles;
    private final Array<FogCloud> fogClouds;

    private Theme currentTheme;
    private final float width, height;
    private float beamTimer = 0f;

    private static class VoidParticle {
        float x, y, speedY;
        float driftSpeed, driftAmplitude, driftTimer;
        float maxLifetime, currentLife, size, baseAlpha;
        Color color;
    }

    private static class FogCloud {
        float x, y, speedX, width, height, alpha;
        float waveTimer, waveSpeed, waveAmplitude;
    }
    public static MenuAtmosphere getInstance(){
        if(instance==null)
            instance=new MenuAtmosphere(Theme.VOID, V_WIDTH, V_HEIGHT);
        return instance;
    }

    private MenuAtmosphere(Theme initialTheme, float virtualWidth, float virtualHeight) {
        this.width = virtualWidth;
        this.height = virtualHeight;
        this.particles = new Array<>();
        this.fogClouds = new Array<>();


        this.beamTexture = new Texture(Gdx.files.internal(PATH_LIGHT_BEAM));


        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fillCircle(8, 8, 8);
        this.particleTexture = new Texture(pixmap);
        pixmap.dispose();


        int fogRes = 256;
        Pixmap fogPixmap = new Pixmap(fogRes, fogRes, Pixmap.Format.RGBA8888);
        float center = fogRes / 2f;
        for (int x = 0; x < fogRes; x++) {
            for (int y = 0; y < fogRes; y++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = 1f - (dist / center);
                if (alpha < 0) alpha = 0;
                alpha = (float) Math.sqrt(alpha);
                fogPixmap.setColor(1f, 1f, 1f, alpha);
                fogPixmap.drawPixel(x, y);
            }
        }
        this.fogTexture = new Texture(fogPixmap);
        this.fogTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fogPixmap.dispose();


        applyTheme(initialTheme, true);
    }

    public void applyTheme(Theme theme, boolean preWarm) {
        this.currentTheme = theme;


        if (currentBackground != null) {
            currentBackground.dispose();
        }
        currentBackground = new Texture(Gdx.files.internal(currentTheme.bgPath));

        particles.clear();
        fogClouds.clear();


        for (int i = 0; i < currentTheme.particleCount; i++) {
            VoidParticle p = spawnParticle();
            if (preWarm) p.y = MathUtils.random(0, height);
            particles.add(p);
        }


        for (int i = 0; i < 5; i++) {
            FogCloud fog = new FogCloud();
            fog.width = MathUtils.random(1300f, 1900f);
            fog.height = MathUtils.random(450f, 750f);
            fog.x = MathUtils.random(-600f, width);
            fog.y = MathUtils.random(-150f, 150f);
            fog.speedX = MathUtils.random(40f, 85f);
            fog.alpha = MathUtils.random(currentTheme.minFogAlpha, currentTheme.maxFogAlpha);
            fog.waveTimer = MathUtils.random(0f, 100f);
            fog.waveSpeed = MathUtils.random(0.5f, 1.2f);
            fog.waveAmplitude = MathUtils.random(40f, 80f);
            fogClouds.add(fog);
        }
    }

    public void update(float delta) {
        beamTimer += 0.6f * delta;


        for (FogCloud fog : fogClouds) {
            fog.x += fog.speedX * delta;
            fog.waveTimer += fog.waveSpeed * delta;
            if (fog.x > width) {
                fog.x = -fog.width;
                fog.y = MathUtils.random(-150f, 150f);
            }
        }


        for (int i = particles.size - 1; i >= 0; i--) {
            VoidParticle p = particles.get(i);
            p.currentLife -= delta;

            if (p.currentLife <= 0) {
                particles.removeIndex(i);
                particles.add(spawnParticle());
            } else {
                p.y += p.speedY * delta;
                p.driftTimer += p.driftSpeed * delta;
                p.x += MathUtils.sin(p.driftTimer) * p.driftAmplitude * delta;
            }
        }
    }

    public void render(SpriteBatch batch) {

        batch.setColor(Color.WHITE);
        batch.draw(currentBackground, 0, 0, width, height);


        float beamAlpha = 0.12f + MathUtils.sin(beamTimer) * 0.06f;
        batch.setColor(1f, 1f, 1f, beamAlpha);
        batch.draw(beamTexture, 0, 0, width, height);


        for (FogCloud fog : fogClouds) {
            float dynamicY = fog.y + (MathUtils.sin(fog.waveTimer) * fog.waveAmplitude);
            batch.setColor(currentTheme.fogColor.r, currentTheme.fogColor.g, currentTheme.fogColor.b, fog.alpha);
            batch.draw(fogTexture, fog.x, dynamicY, fog.width, fog.height);
        }


        for (VoidParticle p : particles) {
            float normalizedLife = p.currentLife / p.maxLifetime;
            float alpha = p.baseAlpha * MathUtils.sin(normalizedLife * MathUtils.PI);
            batch.setColor(p.color.r, p.color.g, p.color.b, alpha);
            batch.draw(particleTexture, p.x - p.size/2f, p.y - p.size/2f, p.size, p.size);
        }

        batch.setColor(Color.WHITE);
    }

    private VoidParticle spawnParticle() {
        VoidParticle p = new VoidParticle();
        p.x = MathUtils.random(-20f, width + 20f);
        p.y = MathUtils.random(-30f, 10f);
        p.speedY = MathUtils.random(45f, 120f);
        p.driftSpeed = MathUtils.random(1f, 2.5f);
        p.driftAmplitude = MathUtils.random(20f, 50f);
        p.driftTimer = MathUtils.random(0f, 300f);
        p.maxLifetime = MathUtils.random(6f, 11f);
        p.currentLife = p.maxLifetime;
        p.size = MathUtils.random(8f, 24f);
        p.baseAlpha = MathUtils.random(0.45f, 0.8f);

        float bright = MathUtils.random(currentTheme.minPartBright, currentTheme.maxPartBright);

        if (currentTheme == Theme.VOID) {
            p.color = new Color(bright, bright + 0.01f, bright + 0.04f, 1f);
        } else if (currentTheme == Theme.GREENPATH) {
            p.color = new Color(bright * 0.7f, bright, bright * 0.6f, 1f);
        } else {
            p.color = new Color(bright, bright * 0.4f, bright * 0.9f, 1f);
        }
        return p;
    }

    @Override
    public void dispose() {
        if (currentBackground != null) currentBackground.dispose();
        if (beamTexture != null) beamTexture.dispose();
        if (particleTexture != null) particleTexture.dispose();
        if (fogTexture != null) fogTexture.dispose();
    }
    public Theme getCurrentTheme() {
        return currentTheme;
    }
}
