package com.Graphic.views.ui;

import com.Graphic.Main;
import com.Graphic.managers.*;
import com.Graphic.models.Player;
import com.Graphic.models.GameSaveData;
import com.Graphic.models.enums.GameViewScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;

public class GameHUD {
    private final DialogueBox dialogueBox;
    private final SoulOrb soulOrb;
    private final HealthSystem healthSystem;
    private final AchievementPopup achievementPopup;
    private int lastKnownHealth;

    private boolean showingCharmCutscene = false;
    private float charmCutsceneAlpha = 0f;
    private float charmCutsceneTimer = 0f;
    private static final float FLASH_HOLD_DURATION = 1.5f;
    private static final float FADE_OUT_SPEED = 0.5f;

    private boolean showingEndScreen = false;
    private float endScreenTimer = 0f;
    private float endScreenBgAlpha = 0f;
    private static final float END_SCREEN_FADE_SPEED = 0.5f;

    public GameHUD(int playerMaxHealth) {
        dialogueBox = new DialogueBox(new Texture(Gdx.files.internal("ui/menu/Title Bottom.png")));
        soulOrb = new SoulOrb();
        healthSystem = new HealthSystem(playerMaxHealth);
        achievementPopup = new AchievementPopup();
        lastKnownHealth = playerMaxHealth;
    }

    public void update(float delta, Player player) {
        dialogueBox.update(delta);
        dialogueBox.handleInput();
        soulOrb.update(delta);
        healthSystem.update(delta);
        achievementPopup.update(delta);

        syncHud(player);

        if (showingCharmCutscene) {
            charmCutsceneTimer += delta;
            if (charmCutsceneTimer > FLASH_HOLD_DURATION) {
                charmCutsceneAlpha -= delta * FADE_OUT_SPEED;
                if (charmCutsceneAlpha <= 0f) {
                    charmCutsceneAlpha = 0f;
                    showingCharmCutscene = false;
                }
            }
        }

        if (showingEndScreen) {
            endScreenTimer += delta;
            if (endScreenBgAlpha < 1f) {
                endScreenBgAlpha += delta * END_SCREEN_FADE_SPEED;
                if (endScreenBgAlpha > 1f) {
                    endScreenBgAlpha = 1f;
                }
            }
        }
    }

    private void syncHud(Player player) {
        int health = player.getHealth();
        if (health < lastKnownHealth) healthSystem.takeDamage(lastKnownHealth - health);
        else if (health > lastKnownHealth) healthSystem.heal(health - lastKnownHealth);
        lastKnownHealth = health;
        soulOrb.setSoul(player.getSoul());
    }

    public void triggerCharmCutscene() {
        showingCharmCutscene = true;
        charmCutsceneAlpha = 1f;
        charmCutsceneTimer = 0f;
    }

    public void triggerEndScreen() {
        showingEndScreen = true;
        endScreenTimer = 0f;
        endScreenBgAlpha = 0f;
        SaveManager.saveCurrentGame();
    }

    public void showDialogue(String key) {
        dialogueBox.show(DialogueManager.getInitialLines(key));
    }

    public void showDialogueText(String text) {
        dialogueBox.show(new String[]{ text });
    }

    public void closeDialogue() {
        dialogueBox.hide();
    }

    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, Matrix4 projectionMatrix) {
        batch.setProjectionMatrix(projectionMatrix);
        batch.begin();

        if (!showingEndScreen || endScreenBgAlpha < 1f) {
            dialogueBox.render(batch);
            soulOrb.render(batch);
            healthSystem.render(batch);
        }

        if (showingCharmCutscene || charmCutsceneAlpha > 0f) {
            batch.end();
            renderCharmCutscene(batch, shapeRenderer, projectionMatrix);
            batch.begin();
        }

        if (showingEndScreen) {
            batch.end();
            renderEndScreen(batch, shapeRenderer, projectionMatrix);
            batch.begin();
        }

        if (!showingEndScreen || endScreenBgAlpha < 1f) {
            achievementPopup.render(batch);
        }

        batch.end();
    }

    private void renderCharmCutscene(SpriteBatch batch, ShapeRenderer shapeRenderer, Matrix4 projectionMatrix) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, charmCutsceneAlpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        batch.begin();
        BitmapFont titleFont = FontManager.getTitle();
        String text = "no cost too great";
        GlyphLayout layout = new GlyphLayout(titleFont, text);

        float textX = (Gdx.graphics.getWidth() - layout.width) / 2f;
        float textY = (Gdx.graphics.getHeight() + layout.height) / 2f;

        titleFont.setColor(0f, 0f, 0f, charmCutsceneAlpha);
        titleFont.draw(batch, text, textX, textY);
        titleFont.setColor(Color.WHITE);
        batch.end();
    }

    private void renderEndScreen(SpriteBatch batch, ShapeRenderer shapeRenderer, Matrix4 projectionMatrix) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, endScreenBgAlpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        batch.begin();
        BitmapFont titleFont = FontManager.getTitle();
        BitmapFont menuFont = FontManager.getMenu();
        BitmapFont bodyFont = FontManager.getBody();

        float titleAlpha = MathUtils.clamp(endScreenTimer - 1.0f, 0f, 1f);
        float statsAlpha = MathUtils.clamp(endScreenTimer - 2.5f, 0f, 1f);
        float quoteAlpha = MathUtils.clamp(endScreenTimer - 4.5f, 0f, 1f);
        float creditsAlpha = MathUtils.clamp(endScreenTimer - 6.5f, 0f, 1f);
        float buttonsAlpha = MathUtils.clamp(endScreenTimer - 8.0f, 0f, 1f);

        Color originalTitleColor = new Color(titleFont.getColor());
        Color originalMenuColor = new Color(menuFont.getColor());
        Color originalBodyColor = new Color(bodyFont.getColor());

        float cx = Gdx.graphics.getWidth() / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        if (titleAlpha > 0f) {
            titleFont.setColor(1f, 1f, 1f, titleAlpha);
            GlyphLayout titleLayout = new GlyphLayout(titleFont, LocalizationManager.get("end.title"));
            titleFont.draw(batch, titleLayout, cx - (titleLayout.width / 2f), cy + 320);
        }

        if (statsAlpha > 0f) {
            int kills = 0;
            int deaths = 0;
            float time = 0f;

            GameSaveData save = SaveManager.currentSave;
            if (save != null) {
                kills = save.enemiesKilled;
                deaths = save.deaths;
                time = save.timePlayed;
            }

            int hours = (int) (time / 3600);
            int minutes = (int) ((time % 3600) / 60);
            int seconds = (int) (time % 60);
            String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            String statsStr = LocalizationManager.get("end.kills") + ": " + kills + "\n\n" +
                LocalizationManager.get("end.deaths") + ": " + deaths + "\n\n" +
                LocalizationManager.get("end.time") + ": " + timeStr;

            menuFont.setColor(0.9f, 0.9f, 0.9f, statsAlpha);
            GlyphLayout statsLayout = new GlyphLayout(menuFont, statsStr, menuFont.getColor(), 0, Align.center, false);
            menuFont.draw(batch, statsLayout, cx, cy + 160);
        }

        if (quoteAlpha > 0f) {
            bodyFont.setColor(0.8f, 0.8f, 0.8f, quoteAlpha);
            GlyphLayout quoteLayout = new GlyphLayout(bodyFont, LocalizationManager.get("end.quote"), bodyFont.getColor(), 0, Align.center, false);
            bodyFont.draw(batch, quoteLayout, cx, cy - 140);
        }

        if (creditsAlpha > 0f) {
            bodyFont.setColor(0.6f, 0.6f, 0.6f, creditsAlpha);
            GlyphLayout creditsLayout = new GlyphLayout(bodyFont, LocalizationManager.get("end.credits"), bodyFont.getColor(), 0, Align.center, false);
            bodyFont.draw(batch, creditsLayout, cx, cy - 260);
        }

        if (buttonsAlpha > 0f) {
            String restartText = LocalizationManager.get("end.restart");
            String menuText = LocalizationManager.get("end.menu");

            GlyphLayout restartLayout = new GlyphLayout(menuFont, restartText);
            GlyphLayout menuLayout = new GlyphLayout(menuFont, menuText);

            float buttonSpacing = 150f;
            float totalWidth = restartLayout.width + buttonSpacing + menuLayout.width;
            float startX = cx - (totalWidth / 2f);
            float buttonsY = cy - 360f;

            float restartX = startX;
            float menuX = startX + restartLayout.width + buttonSpacing;

            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();

            boolean restartHovered = mx >= restartX && mx <= restartX + restartLayout.width &&
                my >= buttonsY - restartLayout.height && my <= buttonsY;

            boolean menuHovered = mx >= menuX && mx <= menuX + menuLayout.width &&
                my >= buttonsY - menuLayout.height && my <= buttonsY;

            if (buttonsAlpha >= 1f) {
                if (restartHovered) {
                    menuFont.setColor(Color.YELLOW);
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        Main.getInstance().setScreen(GameViewScreen.StartGameMenu);
                    }
                } else {
                    menuFont.setColor(1f, 1f, 1f, buttonsAlpha);
                }
            } else {
                menuFont.setColor(1f, 1f, 1f, buttonsAlpha);
            }
            menuFont.draw(batch, restartLayout, restartX, buttonsY);

            if (buttonsAlpha >= 1f) {
                if (menuHovered) {
                    menuFont.setColor(Color.YELLOW);
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        Main.getInstance().setScreen(GameViewScreen.MainMenu);
                    }
                } else {
                    menuFont.setColor(1f, 1f, 1f, buttonsAlpha);
                }
            } else {
                menuFont.setColor(1f, 1f, 1f, buttonsAlpha);
            }
            menuFont.draw(batch, menuLayout, menuX, buttonsY);
        }

        titleFont.setColor(originalTitleColor);
        menuFont.setColor(originalMenuColor);
        bodyFont.setColor(originalBodyColor);

        batch.end();
        EventBus.emit(EventBus.Event.ENTER_END);
    }

    public void resize(int w, int h) {
        soulOrb.resize(w, h);
        healthSystem.resize(w, h);
    }

    public void dispose() {
        soulOrb.dispose();
        healthSystem.dispose();
    }
}
