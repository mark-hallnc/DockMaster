package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class SoundManager implements Disposable {
    private Map<String, Sound> sounds = new HashMap<>();
    private ProgressManager progressManager;

    private long motorLoopId = -1;
    private float motorVolume = 0f;
    private float motorPitch = 1f;

    public SoundManager(ProgressManager progressManager) {
        this.progressManager = progressManager;

        loadSound("click", "sounds/click.wav");
        loadSound("bump", "sounds/bump.wav");
        loadSound("crash", "sounds/crash.wav");
        loadSound("success", "sounds/success.wav");
        loadSound("fail", "sounds/fail.wav");
        loadSound("cash", "sounds/cash.wav");
        loadSound("repair", "sounds/repair.wav");
        loadSound("motor", "sounds/boat_motor_loop.ogg");
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

    public void updateMotorLoop(boolean shouldRun, float delta, float boatSpeed, boolean forwardThrottle, boolean reverseThrottle) {
        Sound motor = sounds.get("motor");
        if (motor == null) return;

        if (!progressManager.isSoundEnabled() || !shouldRun) {
            stopMotorLoop();
            return;
        }

        if (motorLoopId == -1) {
            motorLoopId = motor.loop(0);
            motorVolume = 0f;
            motorPitch = 0.7f;
        }

        float normalizedSpeed = MathUtils.clamp(boatSpeed / 180f, 0f, 1f);
        float throttleBoost = forwardThrottle ? 0.35f : reverseThrottle ? 0.25f : 0f;
        float targetVolume = MathUtils.clamp(throttleBoost + normalizedSpeed * 0.45f, 0f, 0.7f);

        if (!forwardThrottle && !reverseThrottle && normalizedSpeed < 0.05f) {
            targetVolume = 0f;
        }

        float targetPitch = MathUtils.clamp(0.70f + normalizedSpeed * 0.45f + (forwardThrottle ? 0.10f : 0f), 0.65f, 1.25f);

        // Smooth changes
        motorVolume += (targetVolume - motorVolume) * Math.min(1f, delta * 4f);
        motorPitch += (targetPitch - motorPitch) * Math.min(1f, delta * 3f);

        motor.setVolume(motorLoopId, motorVolume);
        motor.setPitch(motorLoopId, motorPitch);
    }

    public void stopMotorLoop() {
        Sound motor = sounds.get("motor");
        if (motor != null && motorLoopId != -1) {
            motor.stop(motorLoopId);
            motorLoopId = -1;
        }
    }

    public float getMotorVolume() { return motorVolume; }
    public float getMotorPitch() { return motorPitch; }
    public boolean isMotorActive() { return motorLoopId != -1; }

    @Override
    public void dispose() {
        stopMotorLoop();
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
    }
}
