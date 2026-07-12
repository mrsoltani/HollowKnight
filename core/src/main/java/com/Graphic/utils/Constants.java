package com.Graphic.utils;

public final class Constants {
    private Constants() {}

    public static final float V_WIDTH  = 1920f;
    public static final float V_HEIGHT = 1200f;

    public static final class Menu {
        private Menu() {}
        public static final float  POINTER_OFFSET               = 20f;
        public static final String PATH_POINTER                  = "ui/menu/Pointer.png";
        public static final String PATH_LIGHT_BEAM               = "ui/menu/Beam.png";
        public static final String PATH_TITLE_BOTTOM             = "ui/menu/Title Bottom.png";
        public static final String PATH_LOGO                     = "ui/menu/Logo.png";
        public static final String PATH_SLIDER_ATLAS             = "ui/Slider/slider.atlas";
        public static final String PATH_SLIDER_JSON              = "ui/Slider/slider.json";
        public static final String PATH_VOID_HEART_BACKGROUND    = "ui/menu/Void Heart Background.png";
        public static final String PATH_GREEN_PATH_BACKGROUND    = "ui/menu/Green Path Background.png";
        public static final String PATH_CRYSTAL_PEAK_BACKGROUND  = "ui/menu/Crystal Peak Background.png";
        public static final String SLIDER_STYLE                  = "menuSlider";
    }

    public static final class MainMenu {
        private MainMenu() {}
        public static final float FIRST_ITEM_Y = 600f;
        public static final float ITEM_SPACING  = 90f;
    }

    public static final class SettingsMenu {
        private SettingsMenu() {}
        public static final float TITLE_Y        = V_HEIGHT * 0.82f;
        public static final float FIRST_ITEM_Y   = V_HEIGHT * 0.62f;
        public static final float BACK_Y         = V_HEIGHT * 0.12f;
        public static final float ITEM_SPACING   = 90f;
        public static final float LEFT_COL_X     = 550f;
        public static final float RIGHT_VALUE_X  = 1050f;
        public static final float SLIDER_WIDTH   = 350f;
        public static final float RIGHT_COL_END_X = RIGHT_VALUE_X + SLIDER_WIDTH;
    }
}
