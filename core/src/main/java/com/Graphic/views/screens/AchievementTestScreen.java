package com.Graphic.views.ui;

import com.Graphic.managers.AchievementManager;
import com.Graphic.managers.EventBus;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.models.Achievement;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label; // Added this import
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class AchievementTestScreen implements Screen {

    private final AchievementPopup achievementPopup;
    private Stage stage;
    private Skin skin;

    public AchievementTestScreen() {
        this.achievementPopup = new AchievementPopup();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        initProceduralSkin();

        // Main layout window container
        Table container = new Table();
        container.setFillParent(true);
        container.center();

        // FIX: Explicitly create a Label using a quick LabelStyle
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontManager.getMenuSmall(), new Color(1f, 0.85f, 0.35f, 1f));
        Label titleLabel = new Label("ACHIEVEMENT INJECTOR / TESTER", titleStyle);
        container.add(titleLabel).padBottom(20f).row();

        // Inner grid table for storing the button entries
        Table buttonGrid = new Table();
        buttonGrid.defaults().pad(10f);

        // Fetch real data directly from your Manager
        Array<Achievement> realAchievements = AchievementManager.getAll();

        for (int i = 0; i < realAchievements.size; i++) {
            Achievement achievement = realAchievements.get(i);
            String localizedTitle = LocalizationManager.get(achievement.getTitleKey());

            // Build a row item: [Icon Image] -> [Trigger Button]
            Image iconPreview = new Image(achievement.getIcon());
            TextButton triggerBtn = new TextButton("Trigger: " + localizedTitle, skin);

            triggerBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    EventBus.emit(EventBus.Event.ACHIEVEMENT_UNLOCKED, achievement);
                }
            });

            // Add components to the dynamic grid
            buttonGrid.add(iconPreview).size(48f, 48f);
            buttonGrid.add(triggerBtn).width(320f).height(48f);

            // Two entry columns per row layout
            if ((i + 1) % 2 == 0) {
                buttonGrid.row();
            } else {
                buttonGrid.add().width(20f); // Spacer item between columns
            }
        }

        // Wrap the grid inside a scroll pane so lists of any size don't overflow the screen
        ScrollPane scrollPane = new ScrollPane(buttonGrid, skin);
        scrollPane.setFadeScrollBars(false);

        container.add(scrollPane).expand().fill();
        stage.addActor(container);

        // Map system multiplexer inputs
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.12f, 0.12f, 0.14f, 1f);

        stage.act(delta);
        achievementPopup.update(delta);

        // 1. Draw the test layout UI buttons first
        stage.draw();

        // 2. Use the Stage's internal batch to safely draw the popups cleanly on top
        stage.getBatch().begin();
        achievementPopup.render((SpriteBatch) stage.getBatch());
        stage.getBatch().end();
    }

    private void initProceduralSkin() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        skin = new Skin();
        skin.add("white-pixel", texture);

        // Main Text Button styling setup
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = skin.newDrawable("white-pixel", new Color(0.2f, 0.22f, 0.25f, 1f));
        btnStyle.down = skin.newDrawable("white-pixel", new Color(0.35f, 0.4f, 0.45f, 1f));
        btnStyle.font = FontManager.getMenuSmall();
        btnStyle.fontColor = Color.WHITE;
        skin.add("default", btnStyle);

        // ScrollPane styling setup
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = skin.newDrawable("white-pixel", new Color(0.1f, 0.1f, 0.1f, 0.4f));
        scrollStyle.vScrollKnob = skin.newDrawable("white-pixel", new Color(0.3f, 0.3f, 0.3f, 0.8f));
        skin.add("default", scrollStyle);
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
