package com.Graphic.utils;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public final class ScreenCapture {

    private static FrameBuffer     fbo;
    private static TextureRegion   region;
    private static boolean         captured = false;

    private ScreenCapture() {}

    public static void init(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (fbo != null) fbo.dispose();
        fbo      = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        fbo.getColorBufferTexture().setFilter(
            Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        region   = null;
        captured = false;
    }


    public static FrameBuffer beginCapture() {
        fbo.begin();
        return fbo;
    }


    public static void endCapture() {
        fbo.end();
        Texture t = fbo.getColorBufferTexture();
        region    = new TextureRegion(t);
        region.flip(false, true);
        captured  = true;
    }

    public static boolean  hasCaptured()  { return captured;  }
    public static TextureRegion getRegion() { return region; }

    public static void dispose() {
        if (fbo != null) { fbo.dispose(); fbo = null; }
        captured = false;
    }
}
