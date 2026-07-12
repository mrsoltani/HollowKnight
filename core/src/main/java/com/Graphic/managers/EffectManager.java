package com.Graphic.managers;

import com.Graphic.utils.EffectSpawnData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Plays one-shot visual effects triggered by EventBus events. Player has
 * zero knowledge of this class — it just emits events; this class decides
 * what to draw and where.
 *
 * Call EffectManager.init() once (Main.create()). Call update(delta) once
 * per real frame from GameScreen, and render(batch) inside the same
 * batch.begin()/end() block as player.render() (world camera projection).
 *
 * ── Trim-correct anchoring ── every frame draws relative to originalWidth/
 * originalHeight/offsetX/offsetY (its position within the ORIGINAL untrimmed
 * canvas), not its own trimmed pixel size, since trimmed sizes vary wildly
 * frame-to-frame (see Blast: 20x95 up to 233x270) and centering on trimmed
 * size alone causes visible drift.
 *
 * ── Mirroring ── offsetX is an asymmetric trim, so flipping pixels alone
 * (negative-width trick) without also mirroring offsetX around canvas
 * center leaves the trimmed box anchored to the same side in both facing
 * directions. mirroredOffsetX = originalWidth - offsetX - packedWidth
 * fixes that for facingRight. invertFlip additionally lets a specific
 * effect (e.g. dash) use the OPPOSITE of the player's actual facing — for
 * effects whose base art orientation doesn't match Player's own
 * left-is-unflipped convention.
 */
public class EffectManager {

    private static TextureAtlas atlas;
    private static final Map<EventBus.Event, EffectDef> effectDefs =
        new EnumMap<>(EventBus.Event.class);
    private static final Array<ActiveEffect> activeEffects = new Array<>();

    private static class EffectDef {
        final List<Animation<TextureAtlas.AtlasRegion>> variants = new ArrayList<>();
        float extraForwardOffset;
        float extraOffsetY;
        boolean invertFlip;
    }

    private static class ActiveEffect {
        Animation<TextureAtlas.AtlasRegion> animation;
        float extraForwardOffset;
        float extraOffsetY;
        boolean invertFlip;
        float anchorX, anchorY;
        float stateTime;
        boolean facingRight;
    }

    public static void init() {
        atlas = new TextureAtlas(Gdx.files.internal("effects/effects.atlas"));

        // register(event, frameDuration, extraForwardOffset, extraOffsetY, invertFlip, regionName...)

        // ── Movement Effects ──────────────────────────────────────────────
        register(EventBus.Event.PLAYER_DASH,         1f / 16f,  0f, -50f, true,  "Dash Effect");
        register(EventBus.Event.PLAYER_SHADOW_DASH,  1f / 16f,  0f, -50f, true,  "Dash Effect"); // Can swap to a shadow sprite later

        // ── Combat Effects ────────────────────────────────────────────────
        register(EventBus.Event.PLAYER_ATTACK,       1f / 60f, 30f,   0f, false, "SlashEffect");
        register(EventBus.Event.PLAYER_ATTACK_ALT,   1f / 60f, 24f,   0f, false, "SlashEffectAlt");
        register(EventBus.Event.PLAYER_DOWN_SLASH,   1f / 20f,  0f, -120f, false, "DownSlashEffect");

        // ── Spell Effects ─────────────────────────────────────────────────
        // Passing multiple strings will randomly pick between them!
        register(EventBus.Event.PLAYER_FIREBALL,     1f / 20f,  -120f,   -100f, true, "Blast");
        //register(EventBus.Event.PLAYER_SCREAM_SPELL, 1f / 14f,  0f,   0f, false, "SoulScream", "ShadowScream");

        // ── Subscribe to Unified Events ───────────────────────────────────
        EventBus.subscribe(EventBus.Event.PLAYER_DASH,         data -> spawn(EventBus.Event.PLAYER_DASH, data));
        EventBus.subscribe(EventBus.Event.PLAYER_SHADOW_DASH,  data -> spawn(EventBus.Event.PLAYER_SHADOW_DASH, data));

        EventBus.subscribe(EventBus.Event.PLAYER_ATTACK,       data -> spawn(EventBus.Event.PLAYER_ATTACK, data));
        EventBus.subscribe(EventBus.Event.PLAYER_ATTACK_ALT,   data -> spawn(EventBus.Event.PLAYER_ATTACK_ALT, data));
        EventBus.subscribe(EventBus.Event.PLAYER_DOWN_SLASH,   data -> spawn(EventBus.Event.PLAYER_DOWN_SLASH, data));

        EventBus.subscribe(EventBus.Event.PLAYER_FIREBALL,     data -> spawn(EventBus.Event.PLAYER_FIREBALL, data));
        //EventBus.subscribe(EventBus.Event.PLAYER_SCREAM_SPELL, data -> spawn(EventBus.Event.PLAYER_SCREAM_SPELL, data));
    }

    private static void register(EventBus.Event event, float frameDuration,
                                 float extraForwardOffset, float extraOffsetY, boolean invertFlip,
                                 String... regionNames) {
        EffectDef def = effectDefs.computeIfAbsent(event, k -> new EffectDef());
        def.extraForwardOffset = extraForwardOffset;
        def.extraOffsetY = extraOffsetY;
        def.invertFlip = invertFlip;

        for (String regionName : regionNames) {
            Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(regionName);
            if (frames.size == 0) {
                Gdx.app.error("EffectManager",
                    "Atlas is missing frames for region: '" + regionName + "' (event " + event + ")");
                continue;
            }
            def.variants.add(new Animation<>(frameDuration, frames, Animation.PlayMode.NORMAL));
        }
    }

    private static void spawn(EventBus.Event event, Object data) {
        // Safe cast: If something emits this event WITHOUT payload, it just ignores the visual
        if (!(data instanceof EffectSpawnData spawnData)) {
            // Optional: Only log if it's a critical missing visual, otherwise fail silently
            // since AudioManager is also using these events without sending data sometimes!
            return;
        }

        EffectDef def = effectDefs.get(event);
        if (def == null || def.variants.isEmpty()) return;

        ActiveEffect effect = new ActiveEffect();
        effect.animation          = def.variants.get(MathUtils.random(def.variants.size() - 1));
        effect.extraForwardOffset = def.extraForwardOffset;
        effect.extraOffsetY       = def.extraOffsetY;
        effect.invertFlip         = def.invertFlip;
        effect.anchorX            = spawnData.x();
        effect.anchorY            = spawnData.y();
        effect.facingRight        = spawnData.facingRight();
        effect.stateTime          = 0f;
        activeEffects.add(effect);
    }

    public static void update(float delta) {
        for (int i = activeEffects.size - 1; i >= 0; i--) {
            ActiveEffect e = activeEffects.get(i);
            e.stateTime += delta;
            if (e.animation.isAnimationFinished(e.stateTime)) {
                activeEffects.removeIndex(i);
            }
        }
    }

    /** Call inside the same batch.begin()/end() block using the WORLD camera's projection. */
    public static void render(SpriteBatch batch) {
        for (ActiveEffect e : activeEffects) {
            TextureAtlas.AtlasRegion frame = e.animation.getKeyFrame(e.stateTime);

            float origW   = frame.originalWidth;
            float packedW = frame.packedWidth;
            float packedH = frame.packedHeight;

            boolean flip = e.invertFlip ? !e.facingRight : e.facingRight;

            float offsetX = flip
                ? (origW - frame.offsetX - packedW)
                : frame.offsetX;

            float forward = flip ? e.extraForwardOffset : -e.extraForwardOffset;
            float baseX = e.anchorX - origW / 2f + offsetX + forward;
            float baseY = e.anchorY + frame.offsetY + e.extraOffsetY;

            if (flip) {
                batch.draw(frame, baseX + packedW, baseY, -packedW, packedH);
            } else {
                batch.draw(frame, baseX, baseY, packedW, packedH);
            }
        }
    }

    public static void dispose() {
        if (atlas != null) atlas.dispose();
        activeEffects.clear();
        effectDefs.clear();
    }
}
