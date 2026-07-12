package com.Graphic.views.screens;

import com.Graphic.controllers.MainMenuController;
import com.Graphic.controllers.StartGameController;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.managers.SaveManager;
import com.Graphic.models.GameSaveData;
import com.Graphic.models.enums.GameArea;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import static com.Graphic.utils.Constants.SettingsMenu.TITLE_Y;
import static com.Graphic.utils.Constants.V_HEIGHT;

public class StartGameMenuScreen extends BaseMenuScreen {


    private static final float HIGHER_TITLE_OFFSET   = 150f;
    private static final float NUMBER_COLUMN_PADDING = -90f;
    private static final float SLOT_SPACING          = 210f;
    private static final float STARTING_Y            = V_HEIGHT - 340f;
    private static final float BACK_BUTTON_Y         = 80f;


    private Texture texCrossroads;
    private Texture texCrystalPeak;
    private Texture texEmpty;

    private final GameSaveData[] saveSlots = new GameSaveData[4];


    private float slotDrawWidth  = 600f;
    private float slotDrawHeight = 120f;

    @Override
    protected void onShow() {
        texCrossroads  = new Texture(Gdx.files.internal("ui/start game/crossroads.png"));
        texCrystalPeak = new Texture(Gdx.files.internal("ui/start game/crystalpeak.png"));
        texEmpty       = new Texture(Gdx.files.internal("ui/start game/empty.png"));


        slotDrawWidth  = 1200f;
        slotDrawHeight = 180f;

        for (int i = 0; i < 4; i++) {
            saveSlots[i] = SaveManager.loadSlot(i + 1);
        }
    }

    @Override
    protected String getTitle() {
        return LocalizationManager.get("menu.select_save");
    }

    @Override
    protected int getItemCount() {
        return 5;
    }

    @Override
    protected float getItemY(int index) {
        if (index == 4) return BACK_BUTTON_Y;
        return STARTING_Y - (index * SLOT_SPACING);
    }

    @Override
    protected float getRowStartX(int index) {
        if (index == 4) return centeredStartX(LocalizationManager.get("menu.back"), FontManager.getMenu());
        return (viewport.getWorldWidth() - slotDrawWidth) / 2f;
    }

    @Override
    protected float getRowEndX(int index) {
        if (index == 4) return centeredEndX(LocalizationManager.get("menu.back"), FontManager.getMenu());
        return (viewport.getWorldWidth() + slotDrawWidth) / 2f;
    }

    @Override
    protected void renderHeader() {
        String t = getTitle();
        layout.setText(FontManager.getTitle(), t);
        FontManager.getTitle().setColor(1f, 1f, 1f, 1f);


        float raisedY = TITLE_Y + HIGHER_TITLE_OFFSET;

        FontManager.getTitle().draw(batch, t,
            (viewport.getWorldWidth() - layout.width) / 2f, raisedY);

        float dw = titleBottom.getWidth();
        float dh = titleBottom.getHeight();
        batch.draw(titleBottom, (viewport.getWorldWidth() - dw) / 2f, raisedY - dh - 90f);
    }

    @Override
    protected void renderItems() {
        float centerX = viewport.getWorldWidth() / 2f;
        float startX  = centerX - (slotDrawWidth / 2f);

        BitmapFont titleFont = FontManager.getTitle();
        BitmapFont menuFont  = FontManager.getMenu();


        for (int i = 0; i < 4; i++) {
            GameSaveData slotData = saveSlots[i];
            float yPos = getItemY(i);


            Texture slotBg = texEmpty;
            if (slotData.lastArea == GameArea.CROSSROADS) slotBg = texCrossroads;
            else if (slotData.lastArea == GameArea.CRYSTAL_PEAK) slotBg = texCrystalPeak;


            Color tint = (i == selectedIndex) ? Color.WHITE : new Color(0.6f, 0.6f, 0.6f, 0.9f);
            batch.setColor(tint);


            batch.draw(slotBg, startX, yPos - (slotDrawHeight / 2f), slotDrawWidth, slotDrawHeight);
            batch.setColor(Color.WHITE);


            titleFont.setColor(tint);
            String numberStr = String.valueOf(i + 1);
            layout.setText(titleFont, numberStr);
            titleFont.draw(batch, numberStr, startX - NUMBER_COLUMN_PADDING - layout.width, yPos + (layout.height / 2f));


            if (slotData.lastArea != GameArea.NONE) {
                int totalSeconds = (int) slotData.timePlayed;
                int hours   = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;

                String timeStr = String.format("%dh %02dm", hours, minutes);
                menuFont.setColor(tint);
                layout.setText(menuFont, timeStr);


                float timeX = (startX + slotDrawWidth) - layout.width - 70f;
                float timeY = (yPos - (slotDrawHeight / 2f)) + layout.height + 20f;

                menuFont.draw(batch, timeStr, timeX, timeY);
            }
        }


        int backIndex = 4;
        menuFont.setColor(backIndex == selectedIndex ? Color.WHITE : new Color(0.35f, 0.35f, 0.4f, 0.75f));
        drawCentered(LocalizationManager.get("menu.back"), getItemY(backIndex), menuFont);
        menuFont.setColor(Color.WHITE);
    }

    @Override
    protected void selectCurrent() {
        if (selectedIndex == 4) {
            goBack();
        } else {

            SaveManager.currentSave = saveSlots[selectedIndex];

            if (SaveManager.currentSave.lastArea == GameArea.NONE) {
                SaveManager.currentSave.lastArea = GameArea.CROSSROADS;
            }
            StartGameController.startGame();
        }
    }

    @Override
    protected void goBack() {
        MainMenuController.returnToMainMenu();
    }

    @Override
    protected void onDispose() {
        if (texCrossroads != null) texCrossroads.dispose();
        if (texCrystalPeak != null) texCrystalPeak.dispose();
        if (texEmpty != null) texEmpty.dispose();
    }
}
