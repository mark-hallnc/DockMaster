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
        loadTexture("boat_skiff", "images/boats/skiff.png");
        loadTexture("boat_runabout", "images/boats/runabout.png");
        loadTexture("boat_pontoon", "images/boats/pontoon.png");

        // Dock textures
        loadTexture("dock_plank", "images/docks/dock_plank.png");

        // Effects/Decor
        loadTexture("buoy", "images/effects/buoy.png");

        // Backgrounds
        loadTexture("water_tile", "images/backgrounds/water_tile.png");
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
