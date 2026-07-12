package com.Graphic.views.screens;

import com.Graphic.Main;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.InputManager;
import com.Graphic.managers.CharmManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.models.Charm;
import com.Graphic.models.enums.GameViewScreen;
import com.Graphic.utils.GameAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.List;

public class InventoryScreen extends ScreenAdapter {
    private Stage stage;
    private Table mainTable;

    private List<Charm> allCharms;
    private int maxNotches;

    private boolean inEquippedRow = false;
    private int equippedIndex = 0;
    private int inventoryIndex = 0;

    private final String GLOW_PATH = "ui/inventory/Glow.png";
    private final String BACKBOARD_PATH = "ui/inventory/charms/CharmBackboard.png";

    private final int CHARM_SIZE = 128;
    private final int CELL_SIZE = 144;
    private final int BACKBOARD_SIZE = 64;

    private final float DETAIL_TEXT_WIDTH = 300f;

    private float shakeDuration = 0f;
    private float shakeIntensity = 0f;
    private float baseCamX;
    private float baseCamY;


    private boolean back = false;

    public InventoryScreen() {
        stage = new Stage(new ScreenViewport());

        mainTable = new Table();
        mainTable.setFillParent(true);

        Texture bgTex = new Texture(Gdx.files.internal("ui/inventory/background.png"));
        mainTable.setBackground(new TextureRegionDrawable(new TextureRegion(bgTex)));

        stage.addActor(mainTable);

        baseCamX = stage.getCamera().position.x;
        baseCamY = stage.getCamera().position.y;

        allCharms = CharmManager.getAll();
        maxNotches = CharmManager.getMaxNotches();

        findNextUnlockedInventoryCharm(1);
        refreshUI();
    }

    private List<Charm> getEquippedCharms() {
        List<Charm> equipped = new ArrayList<>();
        for (Charm c : allCharms) {
            if (c.equipped) equipped.add(c);
        }
        return equipped;
    }

    private Charm getSelectedCharm() {
        List<Charm> equippedCharms = getEquippedCharms();
        if (inEquippedRow) {
            if (equippedCharms.isEmpty()) return null;
            equippedIndex = Math.min(equippedIndex, equippedCharms.size() - 1);
            return equippedCharms.get(equippedIndex);
        } else {
            if (allCharms.isEmpty()) return null;
            return allCharms.get(inventoryIndex);
        }
    }

    private void refreshUI() {
        mainTable.clear();

        List<Charm> equippedCharms = getEquippedCharms();
        int currentUsedNotches = CharmManager.getUsedNotches();


        Table leftTable = new Table();


        Table equippedTable = new Table();
        for (int i = 0; i < equippedCharms.size(); i++) {
            Charm charm = equippedCharms.get(i);
            boolean isSelected = (inEquippedRow && i == equippedIndex);

            Stack cellStack = createCharmCell(charm, isSelected, false);
            equippedTable.add(cellStack).size(CELL_SIZE, CELL_SIZE);
        }
        leftTable.add(equippedTable).colspan(4).minHeight(CELL_SIZE).row();


        Table notchTable = new Table();
        Texture usedNotchTex = new Texture(Gdx.files.internal("ui/inventory/UsedNotch.png"));
        Texture unusedNotchTex = new Texture(Gdx.files.internal("ui/inventory/UnusedNotch.png"));

        for (int i = 0; i < maxNotches; i++) {
            if (i < currentUsedNotches) {
                notchTable.add(new Image(usedNotchTex)).pad(5);
            } else {
                notchTable.add(new Image(unusedNotchTex)).pad(5);
            }
        }
        leftTable.add(notchTable).colspan(4).padTop(10).padBottom(5).row();

        Texture splitterTex = new Texture(Gdx.files.internal("ui/inventory/Splitter.png"));
        leftTable.add(new Image(splitterTex)).colspan(4).padBottom(10).row();


        for (int i = 0; i < 8; i++) {
            Charm charm = allCharms.get(i);

            if (charm.unlocked) {
                boolean isSelected = (!inEquippedRow && i == inventoryIndex);
                Stack cellStack = createCharmCell(charm, isSelected, true);
                leftTable.add(cellStack).size(CELL_SIZE, CELL_SIZE).pad(5);
            } else {
                Image backboardImg = new Image(new Texture(Gdx.files.internal(BACKBOARD_PATH)));
                Table backboardContainer = new Table();
                backboardContainer.add(backboardImg).size(BACKBOARD_SIZE, BACKBOARD_SIZE).center();

                leftTable.add(backboardContainer).size(CELL_SIZE, CELL_SIZE).pad(5);
            }

            if ((i + 1) % 4 == 0) {
                leftTable.row();
            }
        }


        Table rightTable = buildDetailTable();


        mainTable.add(leftTable).top();
        mainTable.add(rightTable).top().left().padLeft(90).padTop(0);
    }

    private Table buildDetailTable() {
        Table rightTable = new Table();
        rightTable.top();

        Charm selectedCharm = getSelectedCharm();



        Table iconNameRow = new Table();

        if (selectedCharm != null) {
            Image detailIcon = new Image(new Texture(Gdx.files.internal(selectedCharm.imagePath)));
            Table iconContainer = new Table();
            iconContainer.add(detailIcon).size(CHARM_SIZE, CHARM_SIZE).center();

            String nameStr = LocalizationManager.get("charm.name." + selectedCharm.id.name());
            Label.LabelStyle nameStyle = new Label.LabelStyle(FontManager.getMenu(), Color.WHITE);
            Label nameLabel = new Label(nameStr, nameStyle);
            nameLabel.setWrap(true);

            iconNameRow.add(iconContainer).size(CELL_SIZE, CELL_SIZE).padRight(15);
            iconNameRow.add(nameLabel).width(DETAIL_TEXT_WIDTH).left();
        }


        rightTable.add(iconNameRow).top().left().minHeight(CELL_SIZE).row();



        rightTable.add().minHeight(unusedNotchHeightPlaceholder()).padTop(10).padBottom(5).row();
        rightTable.add().minHeight(1f).padBottom(10).row();



        if (selectedCharm != null) {
            String descStr = LocalizationManager.get("charm.desc." + selectedCharm.id.name());
            Label.LabelStyle descStyle = new Label.LabelStyle(FontManager.getBody(), Color.LIGHT_GRAY);
            Label descLabel = new Label(descStr, descStyle);
            descLabel.setWrap(true);
            descLabel.setAlignment(Align.left);

            rightTable.add(descLabel).width(DETAIL_TEXT_WIDTH).top().left().padTop(20);
        }

        return rightTable;
    }


    private float unusedNotchHeightPlaceholder() {
        return 32f;
    }

    private Stack createCharmCell(Charm charm, boolean isSelected, boolean isGridCell) {
        Stack cellStack = new Stack();

        if (isSelected) {
            Image glowImg = new Image(new Texture(Gdx.files.internal(GLOW_PATH)));
            cellStack.add(glowImg);
        }

        Image charmImg = new Image(new Texture(Gdx.files.internal(charm.imagePath)));
        if (isGridCell && charm.equipped && !isSelected) {
            charmImg.setColor(Color.DARK_GRAY);
        }

        Table charmContainer = new Table();
        charmContainer.add(charmImg).size(CHARM_SIZE, CHARM_SIZE).center();
        cellStack.add(charmContainer);

        return cellStack;
    }

    private void findNextUnlockedInventoryCharm(int step) {
        if (allCharms.isEmpty()) return;

        int initialIndex = inventoryIndex;
        int checkedCount = 0;
        int maxItems = 8;

        while (checkedCount < maxItems) {
            inventoryIndex = (inventoryIndex + step + maxItems) % maxItems;
            if (allCharms.get(inventoryIndex).unlocked) {
                return;
            }
            checkedCount++;
        }
        inventoryIndex = initialIndex;
    }

    private void toggleEquip(Charm charm) {
        if (!charm.unlocked) return;

        if (charm.equipped) {
            CharmManager.unequip(charm.id);

            if (getEquippedCharms().isEmpty() && inEquippedRow) {
                inEquippedRow = false;
            } else if (inEquippedRow && equippedIndex >= getEquippedCharms().size()) {
                equippedIndex = Math.max(0, getEquippedCharms().size() - 1);
            }
        } else {
            boolean success = CharmManager.equip(charm.id);
            if (!success) {
                triggerShake(0.25f, 8f);
                return;
            }
        }
        refreshUI();
    }

    private void triggerShake(float duration, float intensity) {
        this.shakeDuration = duration;
        this.shakeIntensity = intensity;
    }

    private void closeInventoryMenu() {
        Main.getInstance().setScreen(GameViewScreen.GameScreen);
    }

    @Override
    public void render(float delta) {
        if (back) {
            back = false;
            closeInventoryMenu();
            return;
        }

        if (shakeDuration > 0) {
            shakeDuration -= delta;
            float currentPower = shakeIntensity * (shakeDuration / 0.25f);
            float xOffset = (MathUtils.random() - 0.5f) * 2f * currentPower;
            float yOffset = (MathUtils.random() - 0.5f) * 2f * currentPower;

            stage.getCamera().position.set(baseCamX + xOffset, baseCamY + yOffset, 0);
            stage.getCamera().update();
        } else {
            if (stage.getCamera().position.x != baseCamX || stage.getCamera().position.y != baseCamY) {
                stage.getCamera().position.set(baseCamX, baseCamY, 0);
                stage.getCamera().update();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        baseCamX = stage.getCamera().position.x;
        baseCamY = stage.getCamera().position.y;
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void show() {
        back = false;
        allCharms = CharmManager.getAll();
        maxNotches = CharmManager.getMaxNotches();
        refreshUI();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == InputManager.getKeyCode(GameAction.INVENTORY)) {
                    back = true;
                    return true;
                }

                List<Charm> equippedCharms = getEquippedCharms();

                if (keycode == InputManager.getKeyCode(GameAction.RIGHT)) {
                    if (inEquippedRow) {
                        equippedIndex = Math.min(equippedCharms.size() - 1, equippedIndex + 1);
                    } else {
                        findNextUnlockedInventoryCharm(1);
                    }
                    refreshUI();
                }
                else if (keycode == InputManager.getKeyCode(GameAction.LEFT)) {
                    if (inEquippedRow) {
                        equippedIndex = Math.max(0, equippedIndex - 1);
                    } else {
                        findNextUnlockedInventoryCharm(-1);
                    }
                    refreshUI();
                }
                else if (keycode == InputManager.getKeyCode(GameAction.DOWN)) {
                    if (inEquippedRow) {
                        inEquippedRow = false;
                    } else {
                        findNextUnlockedInventoryCharm(4);
                    }
                    refreshUI();
                }
                else if (keycode == InputManager.getKeyCode(GameAction.UP)) {
                    if (!inEquippedRow) {
                        int targetedIndex = (inventoryIndex - 4 + 8) % 8;

                        if (inventoryIndex < 4 && !equippedCharms.isEmpty()) {
                            inEquippedRow = true;
                            equippedIndex = Math.min(inventoryIndex, equippedCharms.size() - 1);
                        } else if (allCharms.get(targetedIndex).unlocked) {
                            inventoryIndex = targetedIndex;
                        } else {
                            findNextUnlockedInventoryCharm(-4);
                        }
                    }
                    refreshUI();
                }
                else if (keycode == InputManager.getKeyCode(GameAction.ATTACK) || keycode == InputManager.getKeyCode(GameAction.JUMP)) {
                    if (inEquippedRow) {
                        toggleEquip(equippedCharms.get(equippedIndex));
                    } else {
                        toggleEquip(allCharms.get(inventoryIndex));
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }
}
