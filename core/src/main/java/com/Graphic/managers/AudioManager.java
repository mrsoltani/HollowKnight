package com.Graphic.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {

    private static final String PREFS_NAME = "audio_settings";
    private static Preferences prefs;


    private static float   masterVolume = 0.75f;
    private static boolean musicEnabled = true;
    private static boolean sfxEnabled   = true;
    private static float   musicVolume  = 0.35f;


    private static final Map<String, Sound> sounds   = new HashMap<>();
    private static final Map<String, Music> musicMap = new HashMap<>();


    private static Music  currentMusic;
    private static String currentMusicKey = "";


    private static boolean isFading     = false;
    private static float   fadeTimer    = 0f;
    private static float   fadeDuration = 2.0f;
    private static Music   fadingOutMusic;
    private static Music   fadingInMusic;
    private static String  pendingMusicKey = "";


    private static class ChannelState {
        Sound sound;
        long id;
    }
    private static final Map<String, ChannelState> channels = new HashMap<>();





    public static void init() {
        prefs        = Gdx.app.getPreferences(PREFS_NAME);
        masterVolume = prefs.getFloat("masterVolume", 0.75f);
        musicEnabled = prefs.getBoolean("musicEnabled", true);
        sfxEnabled   = prefs.getBoolean("sfxEnabled", true);

        loadAllAssets();
        subscribeToPlayerEvents();
        subscribeToAreaEvents();
    }

    private static void loadAllAssets() {
        loadMusic("menu",                 "audio/music/Menu.mp3");
        loadMusic("false knight",         "audio/music/Boss.mp3");
        loadMusic("forgotten crossroads", "audio/music/Forgotten Crossroads.mp3");
        loadMusic("crystal peak",         "audio/music/Crystal Peak.mp3");
        loadMusic("end screen",         "audio/music/Sealed Vessel.mp3");


        loadSFX("ui_hover",       "audio/sfx/ui/Ui Change Selection.mp3");
        loadSFX("ui_select",      "audio/sfx/ui/Ui Button Confirm.mp3");
        loadSFX("ui_confirm",     "audio/sfx/ui/Ui Button Confirm.mp3");
        loadSFX("ui_save",        "audio/sfx/ui/Ui Save.mp3");



        loadSFX("hero_walk",        "audio/sfx/player/Hero Walk Footsteps Stone.mp3");
        loadSFX("hero_run",         "audio/sfx/player/Hero Run Footsteps Stone.mp3");
        loadSFX("hero_jump",        "audio/sfx/player/Hero Jump.mp3");
        loadSFX("hero_wings",       "audio/sfx/player/Hero Wings.mp3");
        loadSFX("hero_land_soft",   "audio/sfx/player/Hero Land Soft.mp3");
        loadSFX("hero_land_hard",   "audio/sfx/player/Hero Land Hard.mp3");
        loadSFX("hero_dash",        "audio/sfx/player/Hero Dash.mp3");
        loadSFX("hero_shade_dash_1","audio/sfx/player/Hero Shade Dash 1.mp3");
        loadSFX("hero_shade_dash_2","audio/sfx/player/Hero Shade Dash 2.mp3");
        loadSFX("hero_wall_jump",   "audio/sfx/player/Hero Wall Jump.mp3");
        loadSFX("hero_wall_slide",  "audio/sfx/player/Hero Wall Slide.mp3");


        loadSFX("hero_slash",       "audio/sfx/player/Hero Slash.mp3");
        loadSFX("hero_slash_alt",   "audio/sfx/player/Hero SlashAlt.mp3");
        loadSFX("hero_down_slash",  "audio/sfx/player/Hero Down Slash.mp3");


        loadSFX("sword_hit_wall_1", "audio/sfx/player/sword hit window 1.mp3");
        loadSFX("sword_hit_wall_2", "audio/sfx/player/sword hit window 2.mp3");
        loadSFX("sword_hit_wall_3", "audio/sfx/player/sword hit window 3.mp3");
        loadSFX("sword_hit_wall_4", "audio/sfx/player/sword hit window 4.mp3");


        loadSFX("hero_fireball",    "audio/sfx/player/Hero Fireball.mp3");
        loadSFX("hero_scream",      "audio/sfx/player/Hero Scream Spell.mp3");
        loadSFX("hero_heal",        "audio/sfx/player/Focus Health Heal.mp3");


        loadSFX("hero_damage_1",    "audio/sfx/player/Hero Damage Less Harsh.mp3");
        loadSFX("hero_damage_2",    "audio/sfx/player/Hero Damage.mp3");
        loadSFX("hero_death",       "audio/sfx/player/Hero Death V2.mp3");

        loadSFX("hero_damage_1",    "audio/sfx/player/Hero Damage Less Harsh.mp3");
        loadSFX("hero_damage_2",    "audio/sfx/player/Hero Damage.mp3");
        loadSFX("hero_death",       "audio/sfx/player/Hero Death V2.mp3");


        loadSFX("soul_1",    "audio/sfx/player/Soul Pickup 1.mp3");
        loadSFX("soul_2",    "audio/sfx/player/Soul Pickup 2.mp3");
        loadSFX("soul_3",       "audio/sfx/player/Soul Pickup 3.mp3");
        loadSFX("soul_4",       "audio/sfx/player/Soul Pickup 4.mp3");


        loadSFX("zote_grunt_1",     "audio/sfx/zote/Zote 01.mp3");
        loadSFX("zote_grunt_2",     "audio/sfx/zote/Zote 02.mp3");
        loadSFX("zote_grunt_3",     "audio/sfx/zote/Zote 03.mp3");
        loadSFX("zote_grunt_4",     "audio/sfx/zote/Zote 04.mp3");
        loadSFX("zote_grunt_5",     "audio/sfx/zote/Zote 05.mp3");
        loadSFX("zote_roar",        "audio/sfx/zote/Zote Battle Roar.mp3");
        loadSFX("zote_run_loop",    "audio/sfx/zote/Zote Runloop 02.mp3");
        loadSFX("zote_fall_air",    "audio/sfx/zote/Zote Battle Fall 01.mp3");
        loadSFX("zote_land_floor",  "audio/sfx/zote/Zote Land.mp3");
        loadSFX("zote_get_up",      "audio/sfx/zote/Zote Get Up.mp3");
        loadSFX("zote_impact_tonk", "audio/sfx/zote/Zote Tonk.mp3");

        loadSFX("wall_break_hit_1", "audio/sfx/area/Breakable Wall Hit 1.mp3");
        loadSFX("wall_break_hit_2", "audio/sfx/area/Breakable Wall Hit 2.mp3");
        loadSFX("wall_break_death", "audio/sfx/area/Breakable Wall Death.mp3");

        loadSFX("charm_pickup",     "audio/sfx/area/Dream Orb Pickup.mp3");
        loadSFX("charm_pickup_2",   "audio/sfx/area/Dream Enter Pt 2.mp3");


        loadSFX("stalactite_break",  "audio/sfx/area/Stalactite Break.mp3");
        loadSFX("stalactite_impact", "audio/sfx/area/Stalactite Impact.mp3");
        loadSFX("stalactite_death",  "audio/sfx/area/Stalactite Death.mp3");


        loadSFX("enemy_hit", "audio/sfx/enemies/Enemy Damage.mp3");
        loadSFX("enemy_death",  "audio/sfx/enemies/Enemy Death Sword.mp3");


        loadSFX("fk_attack_1", "audio/sfx/false knight/False Knight Attack New 01.mp3");
        loadSFX("fk_attack_2", "audio/sfx/false knight/False Knight Attack New 02.mp3");
        loadSFX("fk_attack_3", "audio/sfx/false knight/False Knight Attack New 03.mp3");
        loadSFX("fk_attack_4", "audio/sfx/false knight/False Knight Attack New 04.mp3");
        loadSFX("fk_attack_5", "audio/sfx/false knight/False Knight Attack New 05.mp3");
        loadSFX("fk_ceiling_break",     "audio/sfx/false knight/False Knight Ceiling Break.mp3");
        loadSFX("fk_damage_armour",     "audio/sfx/false knight/False Knight Damage Armour.mp3");
        loadSFX("fk_damage_armour_final","audio/sfx/false knight/False Knight Damage Armour Final.mp3");
        loadSFX("fk_head_damage_2",     "audio/sfx/false knight/False Knight Head Damage 2.mp3");
        loadSFX("fk_jump",              "audio/sfx/false knight/False Knight Jump.mp3");
        loadSFX("fk_land_first",        "audio/sfx/false knight/False Knight Land 1st Time.mp3");
        loadSFX("fk_land",              "audio/sfx/false knight/False Knight Land.mp3");
        loadSFX("fk_roll",              "audio/sfx/false knight/False Knight Roll.mp3");
        loadSFX("fk_strike_ground",     "audio/sfx/false knight/False Knight Strike Ground.mp3");
        loadSFX("fk_swing",             "audio/sfx/false knight/False Knight Swing.mp3");
        loadSFX("fk_death",             "audio/sfx/false knight/Fknight Death.mp3");
        loadSFX("fk_hit_1",             "audio/sfx/false knight/Fknight Hit 01.mp3");
        loadSFX("fk_hit_2",             "audio/sfx/false knight/Fknight Hit 02.mp3");
        loadSFX("fk_rage",              "audio/sfx/false knight/Fknight Rage.mp3");
    }

    private static void subscribeToAreaEvents() {

        EventBus.subscribe(EventBus.Event.ENTER_MENU, e -> fadeToMusic("menu"));
        EventBus.subscribe(EventBus.Event.ENTER_CROSSROADS, e -> fadeToMusic("forgotten crossroads"));
        EventBus.subscribe(EventBus.Event.ENTER_CRYSTAL_PEAK, e -> fadeToMusic("crystal peak"));
        EventBus.subscribe(EventBus.Event.ENTER_BOSS, e -> fadeToMusic("false knight"));
        EventBus.subscribe(EventBus.Event.ENTER_END, e -> fadeToMusic("end screen"));
    }

    private static void subscribeToPlayerEvents() {
        EventBus.subscribe(EventBus.Event.MENU_NAVIGATE, e -> playSFX("ui_hover"));
        EventBus.subscribe(EventBus.Event.MENU_SELECT,   e -> playSFX("ui_select"));

        EventBus.subscribe(EventBus.Event.PLAYER_WALK, e -> playChannelSFX("footsteps", 0.9f, 1.1f, 0.45f, "hero_walk"));
        EventBus.subscribe(EventBus.Event.PLAYER_RUN, e -> playChannelSFX("footsteps", 0.92f, 1.08f, 0.45f, "hero_run"));
        EventBus.subscribe(EventBus.Event.PLAYER_STOP_WALK, e -> stopChannel("footsteps"));
        EventBus.subscribe(EventBus.Event.PLAYER_JUMP,        e -> playSFX("hero_jump"));
        EventBus.subscribe(EventBus.Event.PLAYER_DOUBLE_JUMP, e -> playSFX("hero_wings"));
        EventBus.subscribe(EventBus.Event.PLAYER_LAND_SOFT,   e -> playSFX("hero_land_soft"));
        EventBus.subscribe(EventBus.Event.PLAYER_LAND_HARD,   e -> playSFX("hero_land_hard"));
        EventBus.subscribe(EventBus.Event.PLAYER_WALL_JUMP,   e -> playSFX("hero_wall_jump"));

        EventBus.subscribe(EventBus.Event.PLAYER_WALL_SLIDE,  e -> playChannelSFX("wall_slide", 0.95f, 1.05f, 0.5f, "hero_wall_slide"));

        EventBus.subscribe(EventBus.Event.PLAYER_DASH,         e -> playSFX("hero_dash"));
        EventBus.subscribe(EventBus.Event.PLAYER_SHADOW_DASH,  e -> playRandomSFX("hero_shade_dash_1", "hero_shade_dash_2"));

        EventBus.subscribe(EventBus.Event.PLAYER_ATTACK,       e -> playPitchedSFX(0.9f, 1.1f, "hero_slash"));
        EventBus.subscribe(EventBus.Event.PLAYER_ATTACK_ALT,   e -> playPitchedSFX(0.9f, 1.1f, "hero_slash_alt"));
        EventBus.subscribe(EventBus.Event.PLAYER_DOWN_SLASH,   e -> playPitchedSFX(0.9f, 1.1f, "hero_down_slash"));

        EventBus.subscribe(EventBus.Event.PLAYER_HIT_WALL,     e -> playRandomSFX("sword_hit_wall_1", "sword_hit_wall_2", "sword_hit_wall_3", "sword_hit_wall_4"));


        EventBus.subscribe(EventBus.Event.PLAYER_FIREBALL,     e -> playSFX("hero_fireball"));
        EventBus.subscribe(EventBus.Event.PLAYER_SCREAM_SPELL, e -> playSFX("hero_scream"));
        EventBus.subscribe(EventBus.Event.PLAYER_HEAL,         e -> playSFX("hero_heal"));


        EventBus.subscribe(EventBus.Event.PLAYER_DAMAGED,      e -> playRandomSFX("hero_damage_1", "hero_damage_2"));
        EventBus.subscribe(EventBus.Event.PLAYER_DEATH,        e -> playSFX("hero_death"));

        EventBus.subscribe(EventBus.Event.ENEMY_HIT,      e -> playSFX("enemy_hit"));
        EventBus.subscribe(EventBus.Event.ENEMY_KILLED,        e -> playSFX("enemy_death"));
        EventBus.subscribe(EventBus.Event.PLAYER_SOUL_GAIN,        e ->playRandomSFX("soul_1","soul_2","soul_3","soul_4"));

    }
    public static void subscribeToBossEvents() {
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_ATTACK_WINDUP,
            e -> playRandomSFX("fk_attack_1", "fk_attack_2", "fk_attack_3", "fk_attack_4", "fk_attack_5"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_CHARGE_SWING, e -> playSFX("fk_swing"));

        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_JUMP_TAKEOFF, e -> playSFX("fk_jump"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_LAND_FIRST,   e -> playSFX("fk_land_first"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_JUMP_LAND,    e -> playSFX("fk_land"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_SLAM_IMPACT,  e -> playSFX("fk_strike_ground"));

        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_CEILING_BREAK, e -> playSFX("fk_ceiling_break"));

        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_HIT,
            e -> playRandomSFX("fk_damage_armour", "fk_hit_1", "fk_hit_2"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_HIT_PHASE2,
            e -> playRandomSFX("fk_damage_armour_final", "fk_head_damage_2", "fk_hit_1", "fk_hit_2"));

        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_STUN_RECOVER, e -> playSFX("fk_roll"));
        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_RAGE,         e -> playSFX("fk_rage"));

        EventBus.subscribe(EventBus.Event.FALSE_KNIGHT_DEATH, e -> playSFX("fk_death"));
    }





    public static void playSFX(String name) {
        playSFXInternal(name, 1.0f, 1.0f, null);
    }

    public static void playSFX(String name, float volumeScale) {
        playSFXInternal(name, 1.0f, volumeScale, null);
    }

    public static void playPitchedSFX(float minPitch, float maxPitch, String name) {
        playSFXInternal(name, MathUtils.random(minPitch, maxPitch), 1.0f, null);
    }

    public static void playPitchedSFX(float minPitch, float maxPitch, float volumeScale, String name) {
        playSFXInternal(name, MathUtils.random(minPitch, maxPitch), volumeScale, null);
    }

    public static void playRandomSFX(String... names) {
        if (names.length == 0) return;
        String chosenName = names[MathUtils.random(names.length - 1)];
        playSFXInternal(chosenName, 1.0f, 1.0f, null);
    }

    public static void playChannelSFX(String channel, float minPitch, float maxPitch, float volumeScale, String name) {
        playSFXInternal(name, MathUtils.random(minPitch, maxPitch), volumeScale, channel);
    }

    private static void playSFXInternal(String name, float pitch, float volumeScale, String channel) {
        if (!sfxEnabled) return;
        Sound s = sounds.get(name);
        if (s == null) {
            Gdx.app.error("AudioManager", "SFX asset not found: " + name);
            return;
        }

        if (channel != null) {
            ChannelState prev = channels.get(channel);
            if (prev != null) {
                prev.sound.stop(prev.id);
            }
        }

        long id = s.play(masterVolume * volumeScale, pitch, 0f);

        if (channel != null) {
            ChannelState state = new ChannelState();
            state.sound = s;
            state.id = id;
            channels.put(channel, state);
        }
    }

    public static void stopChannel(String channel) {
        ChannelState prev = channels.get(channel);
        if (prev != null) {
            prev.sound.stop(prev.id);
            channels.remove(channel);
        }
    }

    public static void playMusic(String name) {
        if (keyMatchesCurrent(name) && !isFading) return;


        if (isFading) clearFades();
        if (currentMusic != null) currentMusic.stop();

        currentMusic = musicMap.get(name);
        currentMusicKey = name;

        if (currentMusic != null && musicEnabled) {
            currentMusic.setVolume(masterVolume * musicVolume);
            currentMusic.play();
        } else if (currentMusic == null) {
            Gdx.app.error("AudioManager", "Music asset not found: " + name);
        }
    }

    public static void fadeToMusic(String name) {
        if (keyMatchesCurrent(name) && !isFading) return;
        if (pendingMusicKey.equals(name) && isFading) return;

        Music nextTrack = musicMap.get(name);
        if (nextTrack == null) {
            Gdx.app.error("AudioManager", "Music asset not found: " + name);
            return;
        }

        pendingMusicKey = name;
        isFading = true;
        fadeTimer = 0f;


        if (currentMusic != null && currentMusic.isPlaying()) {
            fadingOutMusic = currentMusic;
        } else if (fadingInMusic != null) {

            fadingOutMusic = fadingInMusic;
        } else {
            fadingOutMusic = null;
        }

        fadingInMusic = nextTrack;

        if (musicEnabled) {
            fadingInMusic.setVolume(0f);
            fadingInMusic.play();
        }
    }

    public static void update(float delta) {

        if (!isFading) return;

        fadeTimer += delta;


        float progress = MathUtils.clamp(fadeTimer / fadeDuration, 0f, 1f);
        float maxVol = masterVolume * musicVolume;


        if (fadingOutMusic != null) {
            fadingOutMusic.setVolume(maxVol * (1f - progress));
        }

        if (fadingInMusic != null && musicEnabled) {
            fadingInMusic.setVolume(maxVol * progress);
        }


        if (progress >= 1f) {
            if (fadingOutMusic != null) {
                fadingOutMusic.stop();
            }


            currentMusic = fadingInMusic;
            currentMusicKey = pendingMusicKey;
            clearFades();
        }
    }

    private static void clearFades() {
        fadingOutMusic = null;
        fadingInMusic = null;
        pendingMusicKey = "";
        isFading = false;
        fadeTimer = 0f;
    }

    public static void stopMusic() {
        if (currentMusic != null) currentMusic.stop();
        if (fadingOutMusic != null) fadingOutMusic.stop();
        if (fadingInMusic != null) fadingInMusic.stop();

        currentMusicKey = "";
        clearFades();
    }

    private static boolean keyMatchesCurrent(String incomingKey) {
        return incomingKey != null && incomingKey.equals(currentMusicKey);
    }

    public static void loadSFX(String name, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                sounds.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
            } else {
                Gdx.app.error("AudioManager", "Missing file on disk: " + path);
            }
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Exception loading SFX: " + path);
        }
    }

    public static void loadMusic(String name, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
                m.setLooping(true);

                m.setVolume(masterVolume * musicVolume);
                musicMap.put(name, m);
            } else {
                Gdx.app.error("AudioManager", "Missing file on disk: " + path);
            }
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Exception loading music: " + path);
        }
    }

    public static void setMasterVolume(float v) {
        masterVolume = Math.max(0f, Math.min(1f, v));
        updateActiveVolumes();
        save();
    }

    public static void setMusicVolume(float v) {
        musicVolume = Math.max(0f, Math.min(1f, v));
        updateActiveVolumes();
    }

    private static void updateActiveVolumes() {

        if (!isFading && currentMusic != null) {
            currentMusic.setVolume(masterVolume * musicVolume);
        }

    }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (currentMusic != null) {
            if (enabled) currentMusic.play();
            else         currentMusic.pause();
        }


        if (!enabled && isFading) {
            if (fadingInMusic != null) fadingInMusic.pause();
            if (fadingOutMusic != null) fadingOutMusic.pause();
        }
        save();
    }

    public static void setSFXEnabled(boolean enabled) {
        sfxEnabled = enabled;
        save();
    }

    public static void resetDefaults() {
        setMasterVolume(0.75f);
        setMusicEnabled(true);
        setSFXEnabled(true);
    }

    public static float   getMasterVolume()  { return masterVolume;  }
    public static boolean isMusicEnabled()   { return musicEnabled;  }
    public static boolean isSFXEnabled()     { return sfxEnabled;    }

    private static void save() {
        if (prefs == null) return;
        prefs.putFloat("masterVolume", masterVolume);
        prefs.putBoolean("musicEnabled", musicEnabled);
        prefs.putBoolean("sfxEnabled", sfxEnabled);
        prefs.flush();
    }

    public static void dispose() {
        sounds.values().forEach(Sound::dispose);
        musicMap.values().forEach(Music::dispose);
        sounds.clear();
        musicMap.clear();
        channels.clear();
        currentMusic = null;
        currentMusicKey = "";
        clearFades();
    }
}
