package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class TextureManager implements Disposable {
    private Map<String, Texture> textures = new HashMap<>();
    private Map<String, Boolean> realTextureLoaded = new HashMap<>();

    public TextureManager() {
        Gdx.app.log("TextureManager", "--- Texture Audit Start ---");

        // Boat textures (CORE)
        loadTextureOrGenerate("boat_skiff", "images/boats/skiff.png", "boat", "skiff");
        loadTextureOrGenerate("boat_runabout", "images/boats/runabout.png", "boat", "runabout");
        loadTextureOrGenerate("boat_pontoon", "images/boats/pontoon.png", "boat", "pontoon");
        loadTextureOrGenerate("boat_cruiser", "images/boats/cruiser.png", "boat", "cruiser");

        // Dock textures (CORE / OPTIONAL)
        loadTextureOrGenerate("dock_plank", "images/docks/dock_plank.png", "dock", null);
        loadOptionalTexture("dock_piling", "images/docks/piling.png");
        loadOptionalTexture("dock_tire_bumper", "images/docks/tire_bumper.png");
        loadOptionalTexture("dock_cleat", "images/docks/cleat.png");

        // Effects/Decor (CORE / OPTIONAL)
        loadTextureOrGenerate("buoy", "images/effects/buoy.png", "buoy", null);
        loadOptionalTexture("marker_buoy", "images/effects/marker_buoy.png");
        loadOptionalTexture("channel_marker_green", "images/effects/channel_marker_green.png");
        loadOptionalTexture("channel_marker_red", "images/effects/channel_marker_red.png");
        loadOptionalTexture("wake_foam", "images/effects/wake_foam.png");
        loadOptionalTexture("impact_splash", "images/effects/impact_splash.png");

        // Decor
        loadOptionalTexture("fuel_pump", "images/decor/fuel_pump.png");
        loadOptionalTexture("umbrella", "images/decor/umbrella.png");

        // Backgrounds (CORE / OPTIONAL)
        loadTextureOrGenerate("water_tile", "images/backgrounds/water_tile.png", "water", null);
        loadOptionalTexture("rocks_tile", "images/backgrounds/rocks_tile.png");
        loadOptionalTexture("sand_tile", "images/backgrounds/sand_tile.png");

        // UI (OPTIONAL with simple fallbacks maybe later)
        loadOptionalTexture("ui_star_filled", "images/ui/star_filled.png");
        loadOptionalTexture("ui_star_empty", "images/ui/star_empty.png");
        loadOptionalTexture("ui_cash", "images/ui/cash.png");
        loadOptionalTexture("ui_wrench", "images/ui/wrench.png");
        loadOptionalTexture("ui_damage", "images/ui/damage.png");
        loadOptionalTexture("ui_lock", "images/ui/lock.png");

        Gdx.app.log("TextureManager", "--- Texture Audit End ---");
    }

    private void loadTextureOrGenerate(String name, String path, String type, String subType) {
        if (Gdx.files.internal(path).exists()) {
            loadTexture(name, path);
        } else {
            Gdx.app.log("TextureManager", "GENERATED: " + name + " (Fallback for " + path + ")");
            Texture generated = null;
            if (type.equals("boat")) generated = PlaceholderTextureFactory.generateBoat(subType != null ? subType : "skiff", 256, 128);
            else if (type.equals("dock")) generated = PlaceholderTextureFactory.generateDock(256, 256);
            else if (type.equals("water")) generated = PlaceholderTextureFactory.generateWater(512, 512);
            else if (type.equals("buoy")) generated = PlaceholderTextureFactory.generateBuoy(64, 64);

            if (generated != null) {
                generated.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures.put(name, generated);
                realTextureLoaded.put(name, false);
            }
        }
    }

    private void loadOptionalTexture(String name, String path) {
        if (Gdx.files.internal(path).exists()) {
            loadTexture(name, path);
        } else {
            Gdx.app.log("TextureManager", "MISSING OPTIONAL: " + path);
            realTextureLoaded.put(name, false);
        }
    }

    private void loadTexture(String name, String path) {
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.put(name, texture);
            realTextureLoaded.put(name, true);
            Gdx.app.log("TextureManager", "REAL: " + name + " (" + path + ")");
        } catch (Exception e) {
            Gdx.app.error("TextureManager", "Error loading texture: " + path, e);
        }
    }

    public Texture getTexture(String name) {
        return textures.get(name);
    }

    public boolean hasTexture(String name) {
        return textures.containsKey(name);
    }

    public boolean isRealTexture(String name) {
        return realTextureLoaded.getOrDefault(name, false);
    }

    public String getTextureStatus(String name) {
        if (!textures.containsKey(name)) return "MISSING";
        return isRealTexture(name) ? "REAL" : "GENERATED";
    }

    @Override
    public void dispose() {
        for (Texture t : textures.values()) {
            t.dispose();
        }
        textures.clear();
        realTextureLoaded.clear();
    }
}
