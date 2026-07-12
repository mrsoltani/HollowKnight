package com.Graphic.models;

import com.Graphic.utils.CharmSpawnData;
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
}
