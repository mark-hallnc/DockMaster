package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TextureManager implements Disposable {
    private Map<String, Texture> textures = new HashMap<>();
    private Map<String, Boolean> realTextureLoaded = new HashMap<>();
    private Map<String, String> alphaStatus = new HashMap<>();
    private static final Set<String> TRANSPARENT_EXPECTED = new HashSet<>();

    static {
        TRANSPARENT_EXPECTED.add("boat_skiff");
        TRANSPARENT_EXPECTED.add("boat_runabout");
        TRANSPARENT_EXPECTED.add("boat_pontoon");
        TRANSPARENT_EXPECTED.add("boat_cruiser");
        TRANSPARENT_EXPECTED.add("buoy");
        TRANSPARENT_EXPECTED.add("marker_buoy");
        TRANSPARENT_EXPECTED.add("channel_marker_green");
        TRANSPARENT_EXPECTED.add("channel_marker_red");
        TRANSPARENT_EXPECTED.add("wake_foam");
        TRANSPARENT_EXPECTED.add("impact_splash");
        TRANSPARENT_EXPECTED.add("dock_piling");
        TRANSPARENT_EXPECTED.add("dock_tire_bumper");
        TRANSPARENT_EXPECTED.add("dock_cleat");
        TRANSPARENT_EXPECTED.add("fuel_pump");
        TRANSPARENT_EXPECTED.add("umbrella");
        TRANSPARENT_EXPECTED.add("ui_star_filled");
        TRANSPARENT_EXPECTED.add("ui_star_empty");
        TRANSPARENT_EXPECTED.add("ui_cash");
        TRANSPARENT_EXPECTED.add("ui_wrench");
        TRANSPARENT_EXPECTED.add("ui_damage");
        TRANSPARENT_EXPECTED.add("ui_lock");
    }

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

            // Alpha Diagnostics
            diagnoseAlpha(name, path);

            Gdx.app.log("TextureManager", "REAL: " + name + " (" + path + ") [" + alphaStatus.get(name) + "]");
        } catch (Exception e) {
            Gdx.app.error("TextureManager", "Error loading texture: " + path, e);
        }
    }

    private void diagnoseAlpha(String name, String path) {
        try {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
            boolean hasAlpha = false;
            int transparentCount = 0;

            // Sample a few pixels (corners and middle edges)
            int[] samples = {
                pixmap.getPixel(0, 0),
                pixmap.getPixel(pixmap.getWidth()-1, 0),
                pixmap.getPixel(0, pixmap.getHeight()-1),
                pixmap.getPixel(pixmap.getWidth()-1, pixmap.getHeight()-1),
                pixmap.getPixel(pixmap.getWidth()/2, 0),
                pixmap.getPixel(0, pixmap.getHeight()/2)
            };

            for (int p : samples) {
                int a = p & 0x000000ff;
                if (a < 255) {
                    hasAlpha = true;
                    transparentCount++;
                }
            }

            String status = hasAlpha ? "Alpha OK" : "OPAQUE";
            if (!hasAlpha && TRANSPARENT_EXPECTED.contains(name)) {
                Gdx.app.error("TextureManager", "WARNING: " + path + " is OPAQUE but expects transparency. Re-export PNG with alpha.");
                status = "OPAQUE (BAD)";
            }

            alphaStatus.put(name, status);
            pixmap.dispose();
        } catch (Exception e) {
            alphaStatus.put(name, "Diag Failed");
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

    public String getAlphaStatus(String name) {
        return alphaStatus.getOrDefault(name, "N/A");
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
