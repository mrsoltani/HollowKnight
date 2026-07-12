package com.Graphic.views.ui;

import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

import static com.Graphic.utils.Constants.V_WIDTH;

public class DialogueBox {

    public enum State { HIDDEN, TYPING, WAITING, DONE }

    // ── Layout — matches the screenshot exactly ───────────────────────────
    private static final float BOX_X          = 0f;
    private static final float BOX_Y          = 40f;
    private static final float BOX_W          = V_WIDTH;
    private static final float BOX_H          = 280f;
    private static final float TEXT_PAD_X     = 80f;
    private static final float TEXT_PAD_TOP   = 50f;
    private static final float ORNAMENT_SCALE = 0.8f;
    private static final float CHARS_PER_SEC  = 45f;

    // ── Assets ────────────────────────────────────────────────────────────
    private final Texture background;
    private final Texture ornament; // titleBottom.png — used top and bottom

    // ── State ─────────────────────────────────────────────────────────────
    private State    state        = State.HIDDEN;
    private String[] lines;
    private int      lineIndex    = 0;
    private float    charProgress = 0f;
    private float    blinkTimer   = 0f;
    private boolean  blinkVisible = true;

    private final GlyphLayout layout = new GlyphLayout();

    public DialogueBox(Texture ornament) {
        this.ornament = ornament;

        // Pure dark background — nearly black with slight blue tint like screenshot
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.02f, 0.02f, 0.06f, 0.96f);
        pm.fill();
        background = new Texture(pm);
        pm.dispose();
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void show(String[] dialogueLines) {
        lines        = dialogueLines;
        lineIndex    = 0;
        charProgress = 0f;
        blinkTimer   = 0f;
        blinkVisible = true;
        state        = State.TYPING;
    }

    public void update(float delta) {
        if (state == State.TYPING) {
            charProgress += CHARS_PER_SEC * delta;
            if (charProgress >= currentLine().length()) {
                charProgress = currentLine().length();
                state        = State.WAITING;
            }
        }

        if (state == State.WAITING) {
            blinkTimer += delta;
            if (blinkTimer >= 0.5f) {
                blinkTimer   = 0f;
                blinkVisible = !blinkVisible;
            }
        }
    }

    public boolean handleInput() {
        if (state == State.HIDDEN || state == State.DONE) return false;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.Z)) {

            if (state == State.TYPING) {
                // Skip typewriter — show full line
                charProgress = currentLine().length();
                state        = State.WAITING;
                return true;
            }

            // Advance to next line
            lineIndex++;
            if (lineIndex >= lines.length) {
                state = State.DONE;
                return true;
            }

            charProgress = 0f;
            blinkTimer   = 0f;
            blinkVisible = true;
            state        = State.TYPING;
            return true;
        }

        return true; // consume all input while visible
    }

    public void render(SpriteBatch batch) {
        if (state == State.HIDDEN) return;

        float ow = ornament.getWidth()  * ORNAMENT_SCALE;
        float oh = ornament.getHeight() * ORNAMENT_SCALE;
        float ox = (V_WIDTH - ow) / 2f;

        // ── 1. Dark background ────────────────────────────────────────────
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(background, BOX_X, BOX_Y, BOX_W, BOX_H);

        // ── 2. Top ornament — centered, sits ON TOP edge of box ──────────
        // Flipped vertically so it hangs downward like the screenshot
        batch.draw(
            ornament,
            ox,
            BOX_Y + BOX_H - oh * 0.5f, // half overhangs above box
            0, 0,
            ornament.getWidth(), ornament.getHeight(),
            ORNAMENT_SCALE, ORNAMENT_SCALE,
            0,
            0, 0,
            ornament.getWidth(), ornament.getHeight(),
            false, true // flipX=false, flipY=true → hangs from top
        );

        // ── 3. Bottom ornament — centered, sits ON BOTTOM edge ───────────
        batch.draw(
            ornament,
            ox,
            BOX_Y - oh * 0.5f,
            0, 0,
            ornament.getWidth(), ornament.getHeight(),
            ORNAMENT_SCALE, ORNAMENT_SCALE,
            0,
            0, 0,
            ornament.getWidth(), ornament.getHeight(),
            false, false // normal orientation
        );

        // ── 4. Dialogue text ──────────────────────────────────────────────
        if (lines != null && lineIndex < lines.length) {
            String full    = currentLine();
            int    visible = Math.min((int) charProgress, full.length());
            String shown   = full.substring(0, visible);

            float textX = BOX_X + TEXT_PAD_X;
            float textY = BOX_Y + BOX_H - TEXT_PAD_TOP;
            float textW = BOX_W - TEXT_PAD_X * 2f;

            FontManager.getBody().setColor(Color.WHITE);
            FontManager.getBody().draw(
                batch,
                shown,
                textX,
                textY,
                textW,
                Align.left,
                true // wrap
            );
        }

        // ── 5. Blinking continue prompt (bottom-right of box) ────────────
        if (state == State.WAITING && blinkVisible) {
            String prompt = LocalizationManager.get("npc.dialogue.continue");
            layout.setText(FontManager.getBody(), prompt);
            FontManager.getBody().setColor(0.6f, 0.6f, 0.6f, 1f);
            FontManager.getBody().draw(
                batch,
                prompt,
                BOX_X + BOX_W - layout.width - TEXT_PAD_X,
                BOX_Y + TEXT_PAD_TOP
            );
            FontManager.getBody().setColor(Color.WHITE);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public boolean isDone()    { return state == State.DONE;   }
    public boolean isVisible() { return state != State.HIDDEN; }
    public void    hide()      { state = State.HIDDEN;         }

    private String currentLine() {
        return (lines != null && lineIndex < lines.length) ? lines[lineIndex] : "";
    }

    public void dispose() {
        background.dispose();
    }
}
