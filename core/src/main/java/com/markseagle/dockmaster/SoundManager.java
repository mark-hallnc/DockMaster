package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class SoundManager implements Disposable {
    private Map<String, Sound> sounds = new HashMap<>();
    private ProgressManager progressManager;

    public SoundManager(ProgressManager progressManager) {
        this.progressManager = progressManager;

        // Placeholder sound loading
        // In a real scenario, these would be in the assets folder.
        // We catch exceptions so missing files don't crash the game.
        loadSound("click", "sounds/click.wav");
        loadSound("bump", "sounds/bump.wav");
        loadSound("crash", "sounds/crash.wav");
        loadSound("success", "sounds/success.wav");
        loadSound("fail", "sounds/fail.wav");
        loadSound("cash", "sounds/cash.wav");
        loadSound("repair", "sounds/repair.wav");
    }

    private void loadSound(String name, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                sounds.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
            } else {
                Gdx.app.log("SoundManager", "Sound file missing: " + path);
            }
        } catch (Exception e) {
            Gdx.app.error("SoundManager", "Error loading sound: " + path, e);
        }
    }

    public void play(String name) {
        if (!progressManager.isSoundEnabled()) return;

        Sound s = sounds.get(name);
        if (s != null) {
            s.play();
        }
    }

    public void play(String name, float volume) {
        if (!progressManager.isSoundEnabled()) return;

        Sound s = sounds.get(name);
        if (s != null) {
            s.play(volume);
        }
    }

    @Override
    public void dispose() {
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
    }
}
