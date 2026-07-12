package com.Graphic.models;

import com.Graphic.Main;
import com.Graphic.managers.*;
import com.Graphic.models.boss.FalseKnight;
import com.Graphic.models.enemies.*;
import com.Graphic.models.enums.GameArea;
import com.Graphic.models.enums.GameMap;
import com.Graphic.models.enums.GameViewScreen;
import com.Graphic.models.spells.Damageable;
import com.Graphic.models.spells.Fireball;
import com.Graphic.models.spells.Scream;
import com.Graphic.utils.CharmSpawnData;
import com.Graphic.utils.GameAction;
import com.Graphic.utils.ScreenCapture;
import com.Graphic.views.ui.GameHUD;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Arrays;

import static com.Graphic.utils.Constants.V_HEIGHT;
import static com.Graphic.utils.Constants.V_WIDTH;

public class GameScreen implements Screen {


    private OrthographicCamera camera;
    private OrthographicCamera screenCam;
    private Viewport           viewport;
    private SpriteBatch        batch;
    private ShapeRenderer      shapeRenderer;


    private TiledMap           tiledMap;
    private TiledMapRenderer   mapRenderer;
    private int[]              backgroundLayers;
    private int[]              foregroundLayers;
    private float              mapPixelWidth;
    private float              mapPixelHeight;


    private TextureAtlas       playerAtlas;
    private Player             player;
    private ZoteNPC            zote;
    private CharmEntity        charmEntity;
    private Array<SolidBlock>  solidBlocks;
    private Array<TeleportZone> teleportZones;
    private Array<FallingSpikeEntity> fallingSpikes;


    private final Array<BaseEnemy> enemies = new Array<>();
    private final Array<Rectangle> enemyCollisionRects = new Array<>();




    private final com.badlogic.gdx.utils.ObjectSet<Damageable> hitThisSwing =
        new com.badlogic.gdx.utils.ObjectSet<>();
    private PlayerState prevPlayerState = PlayerState.IDLE;

    private final Array<Rectangle> mapBoundaryWalls = new Array<>();
    private Texture            fallingSpikeTexture;
    private AmbientParticleSystem bgParticleSystem;
    private AmbientParticleSystem fgParticleSystem;
    private TextureRegion      singleParticleDot;


    private SpellManager       spellManager;



    private final Array<Damageable> spellTargets = new Array<>();


    private GameHUD            gameHUD;
    private boolean            initialized = false;
    private boolean            pause = false;
    private boolean            showHitboxes = false;


    private enum TransitionState { NONE, FADING_OUT, FADING_IN }
    private TransitionState    transitionState = TransitionState.NONE;
    private float              transitionAlpha = 0f;
    private static final float TRANSITION_SPEED = 2.5f;
    private GameMap            pendingTargetMap;
    private String             pendingSpawnName;
    private BreakableWallEntity breakableWall;
    private TiledMapTileLayer breakableWallLayer;

    private BossDoor bossDoor;
    private Texture  bossDoorTexture;
    private FalseKnight falseKnight;
    private Damageable falseKnightDamageable;

    private Rectangle bossTrigger;
    private boolean bossActive = false;
    private Vector2 bossSpawnPoint;
    private Rectangle bossRoomBounds;

    @Override
    public void show() {
        pause = false;
        if (initialized) return;
        initialized = true;

        camera    = new OrthographicCamera();
        viewport  = new ExtendViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply();

        screenCam = new OrthographicCamera();
        screenCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        playerAtlas   = new TextureAtlas(Gdx.files.internal("sprites/knight.atlas"));
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        spellManager  = new SpellManager();
        spawn();
        gameHUD = new GameHUD(player.getMaxHealth());
        ScreenCapture.init(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void spawn(){
        if(SaveManager.currentSave.lastArea==GameArea.CROSSROADS){
            loadRoom(GameMap.CROSSROADS_01, "SpawnPlayer");
        }

        if(SaveManager.currentSave.lastArea==GameArea.CRYSTAL_PEAK){
            loadRoom(GameMap.CRYSTAL_PEAK, "SpawnPlayer");
        }
        player.reset();

    }

    private void loadRoom(GameMap targetMap, String spawnPointName) {
        if (tiledMap != null) tiledMap.dispose();

        TiledMapHelper helper = new TiledMapHelper();
        tiledMap = helper.loadMap(targetMap.getFilePath());
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        MapProperties props = tiledMap.getProperties();
        mapPixelWidth  = props.get("width", Integer.class) * props.get("tilewidth", Integer.class);
        mapPixelHeight = props.get("height", Integer.class) * props.get("tileheight", Integer.class);
        Mosquito.setMapBounds(new Rectangle(0, 0, mapPixelWidth, mapPixelHeight));

        CameraManager.init(camera, new Vector2(mapPixelWidth, mapPixelHeight));
        Array<Rectangle> cameraZones = helper.getCameraZones();
        CameraManager.setCameraZones(cameraZones);

        bossRoomBounds = (cameraZones != null && cameraZones.size > 0) ? cameraZones.first() : null;
        bossTrigger = helper.getBossTrigger();
        bossActive = false;
        solidBlocks   = helper.getSolidRectangles();
        breakableWall = helper.getBreakableWall();
        breakableWallLayer = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer)
            tiledMap.getLayers().get("BreakableWall");
        if (breakableWallLayer != null) breakableWallLayer.setVisible(true);
        if (SaveManager.currentSave.wallBroken && breakableWall != null) {
            breakableWallLayer.setVisible(false);
            breakableWall = null;
        }
        if (breakableWall != null) {
            SolidBlock s = new SolidBlock(0, 0, 0, 0, false, false);
            s.bounds = breakableWall.getBounds();
            solidBlocks.add(s);
        }
        if (bossDoorTexture == null)
            bossDoorTexture = new Texture(Gdx.files.internal("maps/dynamic/door.png"));

        TiledMapHelper.BossDoorData doorData = helper.getBossDoor();
        bossDoor = (doorData != null)
            ? new BossDoor(bossDoorTexture, doorData.openPos, doorData.closedPos)
            : null;
        teleportZones = helper.getTeleportZones();
        splitLayersAroundMain("Main");

        initSpikes(helper);
        initParticles();
        spawnPlayer(helper, spawnPointName);
        spawnEnemies(helper);

        if(!SaveManager.currentSave.charmAcquired) {
            if (charmEntity != null) charmEntity.dispose();
            CharmSpawnData charmData = helper.getCharmData();
            charmEntity = (charmData != null) ? new CharmEntity(charmData) : null;
        } else {
            charmEntity = null;
        }

        Vector2 zoteSpawn = helper.findObjectPosition("logical", "SPAWN_ZOTE");
        if (zote != null) zote.dispose();
        zote = (zoteSpawn != null) ? new ZoteNPC(zoteSpawn) : null;
        falseKnight = null;
        falseKnightDamageable = null;
        bossActive = false;


        bossSpawnPoint = helper.findObjectPosition("logical", "BOSS_SPAWN");


        bossTrigger = helper.getBossTrigger();
        hitThisSwing.clear();
        if (player != null) {
            Rectangle pb = player.getBounds();
            camera.position.set(pb.x + pb.width / 2f, pb.y + pb.height / 2f, 0);
            updateCamera(999f);
        }

        if ("CROSSROADS".equals(targetMap.getAreaTag())) {
            SaveManager.currentSave.lastArea= GameArea.CROSSROADS;
            EventBus.emit(EventBus.Event.ENTER_CROSSROADS);
        }
        if ("CRYSTAL_PEAK".equals(targetMap.getAreaTag())) {
            SaveManager.currentSave.lastArea= GameArea.CRYSTAL_PEAK;
            EventBus.emit(EventBus.Event.ENTER_CRYSTAL_PEAK);
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        if(bossActive&& falseKnight.isDead()){
            bossActive=false;
            triggerEndGame();
        }
        handleTransitions(delta);
        handleInput();

        if (pause) { enterPause(); pause = false; return; }

        if (transitionState != TransitionState.FADING_OUT) {
            SaveManager.currentSave.timePlayed += delta;
            Rectangle wallBounds = (breakableWall != null) ? breakableWall.getBounds() : null;

            player.update(delta, solidBlocks, wallBounds, enemies);
            handleSpellCasts();
            updateSpellTargets();
            spellManager.update(delta, solidBlocks, spellTargets);
            handleNailDamage();

            updateFallingSpikes(delta);
            updateBreakableWall(delta);
            updateEnemies(delta);

            if (charmEntity != null) charmEntity.update(delta, player, gameHUD);
            if (zote != null) zote.update(delta, player, gameHUD, solidBlocks);
            if (!bossActive && bossTrigger != null && bossSpawnPoint != null) {
                if (bossTrigger.overlaps(player.getBounds())) {
                    bossActive = true;
                    EventBus.emit(EventBus.Event.ENTER_BOSS);


                    falseKnight = new FalseKnight(bossSpawnPoint.x, bossSpawnPoint.y);
                    AudioManager.subscribeToBossEvents();
                    falseKnight.setSolidBlocks(solidBlocks);
                    falseKnight.setRoomBounds(bossRoomBounds);


                    final FalseKnight fk = falseKnight;
                    falseKnightDamageable = new Damageable() {
                        @Override public Rectangle getBounds() { return fk.getVulnerableBox(); }
                        @Override public void takeDamage(float amount, boolean fromRight) { fk.takeDamage((int) amount); }
                    };


                    if (bossDoor != null) bossDoor.trigger();


                    EventBus.emit(EventBus.Event.ENTER_BOSS);
                }
            }
            checkRoomTransitions();
        }
        if (bossDoor != null) {
            bossDoor.update(delta);

            if ((bossDoor.isDropping() || bossDoor.isClosed()) && !player.isInvincible()) {
                if (bossDoor.getBounds().overlaps(player.getBounds())) {
                    player.takeDamage(1, true);
                }
            }
        }
        if (falseKnight != null && !falseKnight.isDead()) {
            if (bossActive) {
                falseKnight.update(delta, player.getBounds());

                if (!player.isInvincible()) {
                    Rectangle weapon = falseKnight.getWeaponBox();
                    boolean bodyHit  = falseKnight.getBodyBox().overlaps(player.getBounds());
                    boolean weapHit  = weapon != null && weapon.overlaps(player.getBounds());
                    boolean shockHit = false;
                    for (Rectangle swBox : falseKnight.getShockwaveHitboxes()) {
                        if (swBox.overlaps(player.getBounds())) { shockHit = true; break; }
                    }
                    if (bodyHit || weapHit || shockHit) {
                        player.takeDamage(1, falseKnight.getBounds().x < player.getBounds().x);
                    }
                }
            }
        }
        bgParticleSystem.update(delta, mapPixelWidth, mapPixelHeight);
        fgParticleSystem.update(delta, mapPixelWidth, mapPixelHeight);
        gameHUD.update(delta, player);


        updateCamera(delta);

        EffectManager.update(delta);
        AudioManager.update(delta);

        renderGameWorld();
        gameHUD.render(batch, shapeRenderer, screenCam.combined);
        renderTransitionOverlay();

        if (showHitboxes) drawHitboxes();
        drawSpellHitboxes();

        if(player.isReadyToRespawn()) respawn();

    }

    private void respawn(){
        player.reset();
        loadRoom(GameMap.CROSSROADS_01, "SpawnPlayer");
        SaveManager.currentSave.deaths++;
    }

    private void handleSpellCasts() {
        if (player.consumeFireballCastTrigger()) {
            Rectangle pb = player.getBounds();
            float spawnX = pb.x + pb.width / 2f;
            float spawnY = pb.y + pb.height / 2f;
            boolean voidHeartActive = CharmManager.getStats().voidHeartActive;
            spellManager.spawnFireball(spawnX, spawnY, player.isFacingRight(), voidHeartActive);
        }

        if (player.consumeScreamCastTrigger()) {
            Rectangle pb = player.getBounds();
            float spawnX = pb.x + pb.width / 2f;
            float spawnY = pb.y + pb.height / 2f;
            boolean voidHeartActive = CharmManager.getStats().voidHeartActive;
            spellManager.spawnScream(spawnX, spawnY, voidHeartActive);
        }
    }

    private void updateSpellTargets() {
        spellTargets.clear();

        for (BaseEnemy e : enemies) {
            if (e.isAlive()) spellTargets.add(e);
        }

        if (falseKnight != null && !falseKnight.isDead() && falseKnightDamageable != null) {
            spellTargets.add(falseKnightDamageable);
        }
    }

    private void handleNailDamage() {
        PlayerState ps = player.getState();
        boolean enteringSwing = ps != prevPlayerState
            && (ps == PlayerState.ATTACK || ps == PlayerState.ATTACK_ALT || ps == PlayerState.DOWN_SLASH);
        if (enteringSwing) hitThisSwing.clear();
        prevPlayerState = ps;

        Rectangle attackBox = null;
        if (ps == PlayerState.ATTACK || ps == PlayerState.ATTACK_ALT) {
            attackBox = player.getForwardAttackBox();
        } else if (ps == PlayerState.DOWN_SLASH) {
            attackBox = player.getDownwardAttackBox();
        }
        if (attackBox == null) return;

        Rectangle pb = player.getBounds();
        float playerCX = pb.x + pb.width / 2f;

        for (Damageable target : spellTargets) {
            if (hitThisSwing.contains(target)) continue;

            Rectangle tb = target.getBounds();
            if (tb == null || !attackBox.overlaps(tb)) continue;

            float targetCX = tb.x + tb.width / 2f;
            boolean fromRight = playerCX > targetCX;
            target.takeDamage(CharmManager.getStats().nailDamage, fromRight);
            hitThisSwing.add(target);


            player.gainSoul();
            EventBus.emit(EventBus.Event.PLAYER_SOUL_GAIN);
        }
    }

    private void renderGameWorld() {
        ScreenUtils.clear(0.1f, 0.1f, 0.3f, 1f);

        mapRenderer.setView(camera);
        mapRenderer.render(backgroundLayers);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (FallingSpikeEntity spike : fallingSpikes) spike.render(batch);
        bgParticleSystem.draw(batch);

        if (zote != null) zote.render(batch);

        spellManager.render(batch);
        for (BaseEnemy e : enemies) e.render(batch);
        player.render(batch);
        EffectManager.render(batch);
        if (charmEntity != null) charmEntity.render(batch);

        if (bossDoor != null) bossDoor.render(batch);
        if (falseKnight != null) falseKnight.render(batch);

        batch.end();

        mapRenderer.render(foregroundLayers);

        batch.begin();
        fgParticleSystem.draw(batch);
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) showHitboxes = !showHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) pause = true;
        if (Gdx.input.isKeyJustPressed(InputManager.getKeyCode(GameAction.INVENTORY))) enterInventory();

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) gameHUD.showDialogue("zote");

        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        if (shift && Gdx.input.isKeyJustPressed(Input.Keys.F1)) player.cheatFillSoul();
        if (shift && Gdx.input.isKeyJustPressed(Input.Keys.F2)) player.cheatRestoreOneMask();
        if (shift && Gdx.input.isKeyJustPressed(Input.Keys.F3)) loadRoom(GameMap.CROSSROADS_03, "SPAWN_EAST");

        if (shift && Gdx.input.isKeyJustPressed(Input.Keys.F5)) player.toggleGodMode();
        if (shift && Gdx.input.isKeyJustPressed(Input.Keys.F6)) CharmManager.toggleOneHitMode();
    }

    private void handleTransitions(float delta) {
        if (transitionState == TransitionState.FADING_OUT) {
            transitionAlpha += delta * TRANSITION_SPEED;
            if (transitionAlpha >= 1f) {
                transitionAlpha = 1f;
                loadRoom(pendingTargetMap, pendingSpawnName);
                transitionState = TransitionState.FADING_IN;
            }
        } else if (transitionState == TransitionState.FADING_IN) {
            transitionAlpha -= delta * TRANSITION_SPEED;
            if (transitionAlpha <= 0f) {
                transitionAlpha = 0f;
                transitionState = TransitionState.NONE;
            }
        }
    }

    private void updateFallingSpikes(float delta) {
        Rectangle pb = player.getBounds();
        for (FallingSpikeEntity spike : fallingSpikes) {
            if (spike.update(delta, pb, solidBlocks) && !player.isInvincible()) {
                boolean fromRight = spike.getBounds().x + spike.getBounds().width / 2f < pb.x + pb.width / 2f;
                player.takeDamage(1, fromRight);
            }
        }
    }

    private void updateBreakableWall(float delta) {
        if (breakableWall == null || breakableWall.isBroken()) return;

        breakableWall.update(delta);

        if (player.getHitWall()) {
            boolean justBroke = breakableWall.onHit();
            EventBus.emit(EventBus.Event.BREAKABLE_WALL_HIT);

            if (justBroke) {
                SaveManager.currentSave.wallBroken=true;
                if (breakableWallLayer != null) breakableWallLayer.setVisible(false);

                for (int i = solidBlocks.size - 1; i >= 0; i--) {
                    if (solidBlocks.get(i).bounds == breakableWall.getBounds()) {
                        solidBlocks.removeIndex(i);
                        break;
                    }
                }

                EventBus.emit(EventBus.Event.BREAKABLE_WALL_BROKEN);
            }
        }
    }

    private void checkRoomTransitions() {
        if (transitionState != TransitionState.NONE) return;
        Rectangle pb = player.getBounds();
        for (TeleportZone zone : teleportZones) {
            if (!zone.bounds.overlaps(pb)) continue;
            GameMap nextMap = GameMap.fromString(zone.targetMapName);
            if (nextMap != null) {
                pendingTargetMap = nextMap;
                pendingSpawnName = zone.targetSpawnName;
                transitionState  = TransitionState.FADING_OUT;
            }
            break;
        }
    }

    private void updateCamera(float delta) {
        Rectangle pb = player.getBounds();
        float targetX = pb.x + pb.width / 2f;
        float targetY = pb.y + pb.height / 2f;


        CameraManager.update(targetX, targetY, delta);
    }

    private void initSpikes(TiledMapHelper helper) {
        if (fallingSpikeTexture == null) fallingSpikeTexture = new Texture(Gdx.files.internal("maps/dynamic/falling_spike.png"));
        fallingSpikes = new Array<>();
        for (FallingSpikeData data : helper.getFallingSpikes()) fallingSpikes.add(new FallingSpikeEntity(data, fallingSpikeTexture));
    }

    private void initParticles() {
        if (singleParticleDot == null) singleParticleDot = new TextureRegion(new Texture(Gdx.files.internal("sprites/dust_dot.png")));
        bgParticleSystem = new AmbientParticleSystem(singleParticleDot, 120, 0.2f, 0.5f);
        fgParticleSystem = new AmbientParticleSystem(singleParticleDot, 60, 0.6f, 1.2f);
        bgParticleSystem.populateRegion(mapPixelWidth, mapPixelHeight);
        fgParticleSystem.populateRegion(mapPixelWidth, mapPixelHeight);
    }

    private void spawnPlayer(TiledMapHelper helper, String spawnPointName) {
        Vector2 spawn = helper.findObjectPosition("logical", spawnPointName);
        if (spawn == null) spawn = helper.findObjectPosition("logical", "SpawnPlayer");
        if (spawn == null) spawn = new Vector2(100, 200);

        if (player == null) player = new Player(playerAtlas, spawn.x, spawn.y);
        else {
            player.getBounds().setPosition(spawn.x, spawn.y);
            player.velocity.set(0, 0);
        }
    }

    private void spawnEnemies(TiledMapHelper helper) {
        for (BaseEnemy e : enemies) e.dispose();
        enemies.clear();

        for (TiledMapHelper.EnemySpawnData spawn : helper.getEnemySpawns()) {
            BaseEnemy enemy = createEnemy(spawn.type, spawn.x, spawn.y);
            if (enemy != null) enemies.add(enemy);
        }
    }

    private BaseEnemy createEnemy(String type, float x, float y) {
        String key = type.toLowerCase().replaceAll("[\\s_]", "");
        BaseEnemy enemy;
        switch (key) {
            case "mosquito":
                enemy = new Mosquito(x, y, false);
                placeCentered(enemy, x, y, false);
                break;
            case "crystalcrawler":
                enemy = new CrystalCrawler(x, y, true);
                placeCentered(enemy, x, y, true);
                break;
            case "huskhornhead":
                enemy = new HuskHornhead(x, y, true);
                placeCentered(enemy, x, y, true);
                break;
            case "crystalguardian":
                enemy = new CrystalGuardian(x, y, true);
                placeCentered(enemy, x, y, true);
                break;
            default:
                Gdx.app.error("GameScreen", "Unknown enemy type: " + type);
                return null;
        }
        return enemy;
    }

    private void placeCentered(BaseEnemy e, float x, float y, boolean feetOnPoint) {
        Rectangle hb = e.getHitbox();
        hb.x = x - hb.width / 2f;
        hb.y = feetOnPoint ? y : y - hb.height / 2f;
    }

    private void updateEnemies(float delta) {
        if (enemies.isEmpty()) return;

        enemyCollisionRects.clear();
        for (SolidBlock b : solidBlocks) enemyCollisionRects.add(b.bounds);

        Rectangle pb = player.getBounds();
        float playerCX = pb.x + pb.width / 2f;

        for (BaseEnemy e : enemies) {
            e.update(delta, pb, enemyCollisionRects);

            if (!e.isAlive()) continue;

            Rectangle hb = e.getHitbox();
            float enemyCX = hb.x + hb.width / 2f;

            if (!player.isInvincible() && hb.overlaps(pb)) {
                if(player.isShadowDashing()){
                    e.takeDamage(5, playerCX > enemyCX);
                }
                player.takeDamage(1, enemyCX > playerCX);
            }
            if(e instanceof CrystalGuardian){
                CrystalGuardian s=(CrystalGuardian) e;
                s.setPlatforms(enemyCollisionRects);
                if (s.isPlayerInBeam(player.getBounds()) && !player.isInvincible()) {
                    player.takeDamage(1, true);
                }

            }
        }
    }

    private void splitLayersAroundMain(String name) {
        int total = tiledMap.getLayers().getCount();
        int main  = tiledMap.getLayers().getIndex(name);
        if (main == -1) main = total;

        backgroundLayers = new int[main];
        for (int i = 0; i < main; i++) backgroundLayers[i] = i;

        foregroundLayers = new int[total - main];
        for (int i = main; i < total; i++) foregroundLayers[i - main] = i;
    }

    private void enterPause() {
        ScreenCapture.beginCapture(); renderGameWorld(); ScreenCapture.endCapture();
        Main.getInstance().setInGame(true);
        Main.getInstance().setScreen(GameViewScreen.PauseMenu);
    }

    private void enterInventory() {
        ScreenCapture.beginCapture(); renderGameWorld(); ScreenCapture.endCapture();
        Main.getInstance().setInGame(true);
        Main.getInstance().setScreen(GameViewScreen.InventoryScreen);
    }

    public void triggerEndGame() {
        ScreenCapture.beginCapture();
        renderGameWorld();
        ScreenCapture.endCapture();
        EventBus.emit(EventBus.Event.ENTER_END);
        EventBus.emit(EventBus.Event.BOSS_FKNIGHT_DEATH);
        EventBus.emit(EventBus.Event.GAME_COMPLETED,SaveManager.currentSave.timePlayed);
        Main.getInstance().setInGame(false);
        gameHUD.triggerEndScreen();
    }

    private void renderTransitionOverlay() {
        if (transitionAlpha <= 0f) return;
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(screenCam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, transitionAlpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private void drawHitboxes() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (SolidBlock b : solidBlocks) {
            shapeRenderer.setColor(b.isDeadly ? Color.RED : Color.GREEN);
            shapeRenderer.rect(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height);
        }
        for (TeleportZone z : teleportZones) {
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.rect(z.bounds.x, z.bounds.y, z.bounds.width, z.bounds.height);
        }
        for (FallingSpikeEntity spike : fallingSpikes) {
            shapeRenderer.setColor(Color.PURPLE);
            Rectangle tz = spike.getTriggerZone();
            shapeRenderer.rect(tz.x, tz.y, tz.width, tz.height);
            shapeRenderer.setColor(Color.ORANGE);
            Rectangle sb = spike.getBounds();
            shapeRenderer.rect(sb.x, sb.y, sb.width, sb.height);
        }
        if (breakableWall != null && breakableWall.isIntact()) {
            shapeRenderer.setColor(Color.CYAN);
            Rectangle wb = breakableWall.getBounds();
            shapeRenderer.rect(wb.x, wb.y, wb.width, wb.height);
        }
        if (zote != null) {
            shapeRenderer.setColor(Color.CORAL);
            Rectangle zb = zote.getBounds();
            shapeRenderer.rect(zb.x, zb.y, zb.width, zb.height);
        }
        if (falseKnight != null) {
            falseKnight.drawHitboxes(shapeRenderer);
        }
        for (BaseEnemy e : enemies) e.renderDebug(shapeRenderer);

        Rectangle fw = player.getForwardAttackBox();
        Rectangle dw = player.getDownwardAttackBox();
        if (fw != null) { shapeRenderer.setColor(Color.ORANGE); shapeRenderer.rect(fw.x, fw.y, fw.width, fw.height); }
        if (dw != null) { shapeRenderer.setColor(Color.MAGENTA); shapeRenderer.rect(dw.x, dw.y, dw.width, dw.height); }

        Rectangle pb = player.getBounds();
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(pb.x, pb.y, pb.width, pb.height);

        for (Fireball fb : spellManager.getFireballs()) {
            shapeRenderer.setColor(fb.isVoidHeartActive() ? Color.RED : Color.YELLOW);
            Rectangle r = fb.getHitbox();
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        for (Scream sc : spellManager.getScreams()) {
            if (!sc.isHitboxActive()) continue;
            shapeRenderer.setColor(sc.isVoidHeartActive() ? Color.RED : Color.YELLOW);
            float[] v = sc.getHitboxPolygon().getTransformedVertices();
            shapeRenderer.triangle(v[0], v[1], v[2], v[3], v[4], v[5]);
        }

        shapeRenderer.end();
    }

    private void drawSpellHitboxes() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Fireball fb : spellManager.getFireballs()) {
            shapeRenderer.setColor(fb.isVoidHeartActive() ? Color.RED : Color.YELLOW);
            Rectangle r = fb.getHitbox();
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        for (Scream sc : spellManager.getScreams()) {
            if (!sc.isHitboxActive()) continue;
            shapeRenderer.setColor(sc.isVoidHeartActive() ? Color.RED : Color.YELLOW);
            float[] v = sc.getHitboxPolygon().getTransformedVertices();
            shapeRenderer.triangle(v[0], v[1], v[2], v[3], v[4], v[5]);
        }
        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int w_height) {
        if (w <= 0 || w_height <= 0 || !initialized) return;
        viewport.update(w, w_height);
        screenCam.setToOrtho(false, w, w_height);
        if (gameHUD != null) gameHUD.resize(w, w_height);
        ScreenCapture.init(w, w_height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (tiledMap != null) tiledMap.dispose();
        if (playerAtlas != null) playerAtlas.dispose();
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (fallingSpikeTexture != null) fallingSpikeTexture.dispose();
        if (charmEntity != null) charmEntity.dispose();
        if (gameHUD != null) gameHUD.dispose();
        if (zote != null) zote.dispose();
        for (BaseEnemy e : enemies) e.dispose();
        enemies.clear();
        if (spellManager != null) spellManager.dispose();
        if (bossDoorTexture != null) bossDoorTexture.dispose();
        if (falseKnight != null) falseKnight.dispose();

        ScreenCapture.dispose();
    }
}
