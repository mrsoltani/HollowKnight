package com.Graphic.models;

import com.Graphic.utils.CharmSpawnData;
import com.Graphic.utils.PlatformSpawnData;
import com.Graphic.utils.PressurePlateSpawnData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Vector2;
import com.Graphic.models.FallingSpikeData;
import com.badlogic.gdx.utils.ObjectMap;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TiledMapHelper {
    private TiledMap tiledMap;

    public TiledMap loadMap(String path) {
        tiledMap = new TmxMapLoader().load(path);
        return tiledMap;
    }

    public Array<SolidBlock> getSolidRectangles() {
        Array<SolidBlock> solidBlocks = new Array<>();
        MapLayer layer = tiledMap.getLayers().get("logical");
        if (layer == null) return solidBlocks;

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                boolean isDeadly = false;
                boolean slide = true;
                if (object.getProperties().containsKey("deadly")) {
                    isDeadly = object.getProperties().get("deadly", Boolean.class);
                }
                if (object.getProperties().containsKey("slide")) {
                    slide = object.getProperties().get("slide", Boolean.class);
                }
                solidBlocks.add(new SolidBlock(rect.x, rect.y, rect.width, rect.height, isDeadly,slide));
            }
        }
        return solidBlocks;
    }

    public Vector2 findObjectPosition(String layerName, String objectName) {
        MapLayer layer = tiledMap.getLayers().get(layerName);
        if (layer == null) return null;

        for (MapObject object : layer.getObjects()) {
            if (objectName.equalsIgnoreCase(object.getName())) {
                Float x = object.getProperties().get("x", Float.class);
                Float y = object.getProperties().get("y", Float.class);
                if (x == null || y == null) return null;
                return new Vector2(x, y);
            }
        }
        return null;
    }

    public Array<TeleportZone> getTeleportZones() {
        Array<TeleportZone> zones = new Array<>();
        MapLayer layer = tiledMap.getLayers().get("teleport");
        if (layer == null) {
            Gdx.app.log("TiledMapHelper", "No 'teleport' layer found.");
            return zones;
        }

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            String location = obj.getProperties().get("location", String.class);
            String targetSpawn = obj.getProperties().get("targetSpawn", String.class);

            if (location == null || location.isEmpty()) {
                Gdx.app.error("TiledMapHelper", "Teleport zone missing 'location' property.");
                continue;
            }

            zones.add(new TeleportZone(new Rectangle(rect), location, targetSpawn));
        }
        return zones;
    }

    public Array<FallingSpikeData> getFallingSpikes() {
        Array<FallingSpikeData> result = new Array<>();

        MapLayer dangerLayer = tiledMap.getLayers().get("danger");
        MapLayer spawnLayer  = tiledMap.getLayers().get("objectSpawn");

        if (dangerLayer == null) {
            Gdx.app.log("TiledMapHelper", "No 'danger' layer found.");
            return result;
        }
        if (spawnLayer == null) {
            Gdx.app.log("TiledMapHelper", "No 'objectSpawn' layer found.");
            return result;
        }


        Map<String, Vector2> spawnPoints = new HashMap<>();
        for (MapObject obj : spawnLayer.getObjects()) {
            String name = obj.getName();
            if (name == null || name.isEmpty()) continue;


            if (obj instanceof PointMapObject) {
                Vector2 p = ((PointMapObject) obj).getPoint();
                spawnPoints.put(name, new Vector2(p.x, p.y));
            }
        }


        for (MapObject obj : dangerLayer.getObjects()) {
            String type = obj.getProperties().get("type", String.class);
            if (!"falling_spike".equals(type)) continue;
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle trigger    = new Rectangle(((RectangleMapObject) obj).getRectangle());
            String    targetName = obj.getProperties().get("targetSpike", String.class);

            if (targetName == null) {
                Gdx.app.error("TiledMapHelper", "falling_spike missing 'targetSpike' property.");
                continue;
            }

            Vector2 spawn = spawnPoints.get(targetName);
            if (spawn == null) {
                Gdx.app.error("TiledMapHelper",
                    "No spawn point named '" + targetName + "' in objectSpawn layer.");
                continue;
            }

            result.add(new FallingSpikeData(trigger, spawn.x, spawn.y));
        }

        return result;
    }
    public BreakableWallEntity getBreakableWall() {
        MapLayer layer = tiledMap.getLayers().get("breakableWall");
        if (layer == null) return null;

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            Rectangle rect = new Rectangle(((RectangleMapObject) obj).getRectangle());
            return new BreakableWallEntity(rect);
        }
        return null;
    }
    public CharmSpawnData getCharmData() {
        MapLayer layer = tiledMap.getLayers().get("charm");
        if (layer == null) return null;

        Vector2 animPos = null;
        Rectangle triggerBox = null;

        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof PointMapObject) {
                Vector2 p = ((PointMapObject) obj).getPoint();
                animPos = new Vector2(p.x, p.y);
            } else if (obj instanceof RectangleMapObject) {
                triggerBox = new Rectangle(((RectangleMapObject) obj).getRectangle());
            }
        }

        if (animPos != null && triggerBox != null) {
            return new CharmSpawnData(animPos, triggerBox);
        }
        return null;
    }
    public BossDoorData getBossDoor() {
        MapLayer layer = tiledMap.getLayers().get("door");
        if (layer == null) return null;


        MapProperties props  = tiledMap.getProperties();
        float mapPixelHeight = props.get("height",    Integer.class)
            * props.get("tileheight", Integer.class);

        Vector2 open = null, closed = null;

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof PointMapObject)) continue;
            String name = obj.getName();


            float px = obj.getProperties().get("x", Float.class);
            float py = obj.getProperties().get("y", Float.class);
            float wy = mapPixelHeight - py;

            if      ("OPEN_DOOR".equals(name))   open   = new Vector2(px, wy);
            else if ("CLOSED_DOOR".equals(name)) closed = new Vector2(px, wy);
        }

        if (open == null || closed == null) return null;
        return new BossDoorData(open, closed);
    }
    public static class BossDoorData {
        public final Vector2 openPos, closedPos;
        public BossDoorData(Vector2 o, Vector2 c) { openPos = o; closedPos = c; }
    }


    public Array<EnemySpawnData> getEnemySpawns() {
        Array<EnemySpawnData> result = new Array<>();
        MapLayer layer = tiledMap.getLayers().get("enemies");
        if (layer == null) {
            Gdx.app.log("TiledMapHelper", "No 'enemies' layer found.");
            return result;
        }

        for (MapObject obj : layer.getObjects()) {
            String type = obj.getProperties().get("type", String.class);
            if (type == null || type.isEmpty()) {
                Gdx.app.error("TiledMapHelper", "Enemy spawn missing 'type' property.");
                continue;
            }

            float x, y;
            if (obj instanceof PointMapObject) {
                Vector2 p = ((PointMapObject) obj).getPoint();
                x = p.x; y = p.y;
            } else if (obj instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                x = r.x; y = r.y;
            } else {

                Float px = obj.getProperties().get("x", Float.class);
                Float py = obj.getProperties().get("y", Float.class);
                if (px == null || py == null) continue;
                x = px; y = py;
            }

            result.add(new EnemySpawnData(type, x, y));
        }
        return result;
    }
    public Array<Rectangle> getCameraZones() {
        Array<Rectangle> zones = new Array<>();
        MapLayer layer = tiledMap.getLayers().get("camera");
        if (layer == null) return zones;

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                zones.add(new Rectangle(((RectangleMapObject) object).getRectangle()));
            }
        }
        return zones;
    }

    public Rectangle getBossTrigger() {
        MapLayer layer = tiledMap.getLayers().get("boss");
        if (layer == null) return null;

        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                return new Rectangle(((RectangleMapObject) obj).getRectangle());
            }
        }
        return null;
    }

    public static class EnemySpawnData {
        public final String type;
        public final float x, y;
        public EnemySpawnData(String type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Reads the "laserSpawn" object layer and returns one entry per authored
     * spawn point. Each entry holds the spawn position in game-space
     * coordinates (y-up), converted from Tiled's y-down coordinate system.
     *
     * Object types supported:
     *   - PointMapObject (preferred): one laser per point, using its x/y.
     *   - RectangleMapObject: a one-cell laser placed at the rectangle origin.
     *   - Generic MapObject with explicit x/y properties.
     *
     * Optional properties per object:
     *   - "width"  / "height" : beam rectangle size (defaults below).
     *   - "name"             : free-form label; preserved in the data class.
     */
    public Array<LaserSpawnData> getLaserSpawns() {
        Array<LaserSpawnData> result = new Array<>();
        MapLayer layer = tiledMap.getLayers().get("laserSpawn");
        if (layer == null) {
            Gdx.app.log("TiledMapHelper", "No 'laserSpawn' layer found.");
            return result;
        }

        final float defaultWidth  = 26f;
        final float defaultHeight = 16f;

        for (MapObject obj : layer.getObjects()) {
            float x, y;
            if (obj instanceof PointMapObject) {
                Vector2 p = ((PointMapObject) obj).getPoint();
                x = p.x; y = p.y;
            } else if (obj instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                x = r.x; y = r.y;
            } else {
                Float px = obj.getProperties().get("x", Float.class);
                Float py = obj.getProperties().get("y", Float.class);
                if (px == null || py == null) continue;
                x = px; y = py;
            }

            float w = defaultWidth;
            float h = defaultHeight;
            Float wProp = obj.getProperties().get("width", Float.class);
            Float hProp = obj.getProperties().get("height", Float.class);
            if (wProp != null) w = wProp;
            if (hProp != null) h = hProp;

            // No y-flip: the game uses Tiled's native y-down screen-pixel
            // coords throughout (player collision, enemy positions, solid
            // blocks are all read directly from Tiled), so the spawn sits at
            // exactly the same physical location the designer clicked in Tiled.
            result.add(new LaserSpawnData(x, y, w, h));
        }
        return result;
    }
    // === Puzzle platform / pressure plate parsing ===================

    /**
     * Reads every object on the logical layer whose name starts with
     * "platform_" and groups them by puzzle id.
     *
     * Expected name format: platform_<groupId>_<n>
     *   e.g. "platform_A_1", "platform_A_2", "platform_A_3"
     *
     * <n> only needs to be unique within the group; it is discarded, groupId
     * is what links these to their pressure plate.
     */
    public ObjectMap<String, Array<PlatformSpawnData>> getPlatformGroups() {
        ObjectMap<String, Array<PlatformSpawnData>> groups = new ObjectMap<>();

        MapLayer logicalLayer = tiledMap.getLayers().get("puzzle");
        if (logicalLayer == null) {
            Gdx.app.error("TiledMapHelper", "getPlatformGroups: no 'puzzle' layer found");
            return groups;
        }

        int objectCount = logicalLayer.getObjects().getCount();
        Gdx.app.error("TiledMapHelper", "getPlatformGroups: 'puzzle' layer has " + objectCount + " objects total");

        for (MapObject obj : logicalLayer.getObjects()) {
            String name = obj.getName();
            Gdx.app.error("TiledMapHelper", "getPlatformGroups: found object name='" + name
                + "' class=" + obj.getClass().getSimpleName());

            if (name == null || !name.startsWith("platform_")) continue;

            String[] parts = name.split("_");
            if (parts.length < 3) {
                Gdx.app.error("TiledMapHelper", "Malformed platform object name: " + name
                    + " (expected platform_<groupId>_<n>, got " + parts.length + " parts)");
                continue;
            }
            String groupId = parts[1];

            float[] xy = objectCenter(obj);
            if (xy == null) {
                Gdx.app.error("TiledMapHelper", "getPlatformGroups: objectCenter() returned null for '" + name + "'");
                continue;
            }

            Gdx.app.error("TiledMapHelper", "getPlatformGroups: registered platform group='" + groupId
                + "' at (" + xy[0] + ", " + xy[1] + ")");

            PlatformSpawnData data = new PlatformSpawnData(groupId, xy[0], xy[1]);

            Array<PlatformSpawnData> list = groups.get(groupId);
            if (list == null) {
                list = new Array<>();
                groups.put(groupId, list);
            }
            list.add(data);
        }

        Gdx.app.error("TiledMapHelper", "getPlatformGroups: final group count=" + groups.size);
        for (ObjectMap.Entry<String, Array<PlatformSpawnData>> e : groups) {
            Gdx.app.error("TiledMapHelper", "  group '" + e.key + "' -> " + e.value.size + " platform(s)");
        }

        return groups;
    }

    /**
     * Reads every object on the logical layer whose name starts with
     * "pressure_" and keys them by puzzle id.
     *
     * Expected name format: pressure_<groupId>
     *   e.g. "pressure_A"
     */
    public ObjectMap<String, PressurePlateSpawnData> getPressurePlateGroups() {
        ObjectMap<String, PressurePlateSpawnData> plates = new ObjectMap<>();

        MapLayer logicalLayer = tiledMap.getLayers().get("puzzle");
        if (logicalLayer == null) {
            Gdx.app.error("TiledMapHelper", "getPressurePlateGroups: no 'puzzle' layer found");
            return plates;
        }

        for (MapObject obj : logicalLayer.getObjects()) {
            String name = obj.getName();
            if (name == null || !name.startsWith("pressure_")) continue;

            String[] parts = name.split("_");
            if (parts.length < 2) {
                Gdx.app.error("TiledMapHelper", "Malformed pressure plate object name: " + name);
                continue;
            }
            String groupId = parts[1];

            float[] xy = objectCenter(obj);
            if (xy == null) {
                Gdx.app.error("TiledMapHelper", "getPressurePlateGroups: objectCenter() returned null for '" + name + "'");
                continue;
            }

            if (plates.containsKey(groupId)) {
                Gdx.app.error("TiledMapHelper", "Duplicate pressure plate for group: " + groupId);
                continue;
            }
            Gdx.app.error("TiledMapHelper", "getPressurePlateGroups: registered plate group='" + groupId
                + "' at (" + xy[0] + ", " + xy[1] + ")");
            plates.put(groupId, new PressurePlateSpawnData(groupId, xy[0], xy[1]));
        }

        Gdx.app.error("TiledMapHelper", "getPressurePlateGroups: final plate count=" + plates.size);
        return plates;
    }

    /**
     * Returns the center point of a Tiled object in the same y-down pixel
     * space used everywhere else in this project (matches findObjectPosition's
     * coordinate convention — no flipping).
     *
     * Handles either a point object (x/y only) or a rectangle object
     * (x/y + width/height), since Tiled point objects and rectangle objects
     * both surface as MapObject but only RectangleMapObject exposes a
     * Rectangle via getRectangle().
     */
    private float[] objectCenter(MapObject obj) {
        MapProperties props = obj.getProperties();
        if (!props.containsKey("x") || !props.containsKey("y")) {
            Gdx.app.error("TiledMapHelper", "objectCenter: object '" + obj.getName() + "' has no x/y properties");
            return null;
        }

        float x = props.get("x", Float.class);
        float y = props.get("y", Float.class);

        if (obj instanceof RectangleMapObject) {
            Rectangle r = ((RectangleMapObject) obj).getRectangle();
            return new float[] { r.x + r.width / 2f, r.y + r.height / 2f };
        }

        return new float[] { x, y };
    }
}
