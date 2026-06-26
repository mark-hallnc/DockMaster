package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class TextureManager implements Disposable {
    private Map<String, Texture> textures = new HashMap<>();

    public TextureManager() {
        // Boat textures
        loadTextureOrGenerate("boat_skiff", "images/boats/skiff.png", "boat", "skiff");
        loadTextureOrGenerate("boat_runabout", "images/boats/runabout.png", "boat", "runabout");
        loadTextureOrGenerate("boat_pontoon", "images/boats/pontoon.png", "boat", "pontoon");

        // Dock textures
        loadTextureOrGenerate("dock_plank", "images/docks/dock_plank.png", "dock", null);

        // Effects/Decor
        loadTextureOrGenerate("buoy", "images/effects/buoy.png", "buoy", null);

        // Backgrounds
        loadTextureOrGenerate("water_tile", "images/backgrounds/water_tile.png", "water", null);
    }

    private void loadTextureOrGenerate(String name, String path, String type, String subType) {
        if (Gdx.files.internal(path).exists()) {
            loadTexture(name, path);
        } else {
            Gdx.app.log("TextureManager", "Generating fallback for: " + path);
            Texture generated = null;
            if (type.equals("boat")) generated = PlaceholderTextureFactory.generateBoat(subType, 256, 128);
            else if (type.equals("dock")) generated = PlaceholderTextureFactory.generateDock(256, 256);
            else if (type.equals("water")) generated = PlaceholderTextureFactory.generateWater(512, 512);
            else if (type.equals("buoy")) generated = PlaceholderTextureFactory.generateBuoy(64, 64);

            if (generated != null) {
                generated.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures.put(name, generated);
            }
        }
    }

    private void loadTexture(String name, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Texture texture = new Texture(Gdx.files.internal(path));
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures.put(name, texture);
                Gdx.app.log("TextureManager", "Loaded: " + path);
            } else {
                Gdx.app.log("TextureManager", "Optional texture missing: " + path);
            }
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

    @Override
    public void dispose() {
        for (Texture t : textures.values()) {
            t.dispose();
        }
        textures.clear();
    }
}
