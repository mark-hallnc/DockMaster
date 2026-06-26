package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

public class WakeTrail {
    private static class Particle {
        float x, y, life, maxLife;
        float size;
        float vx, vy;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> pool = new ArrayList<>();
    private float timer = 0;

    public void update(float delta, float x, float y, float speed) {
        timer += delta;
        // Adjust spawn rate based on speed
        float spawnRate = MathUtils.clamp(0.2f - (speed / 1000f), 0.02f, 0.2f);

        if (speed > 20 && timer > spawnRate) {
            timer = 0;
            spawn(x, y, speed);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= delta;
            p.size += delta * 15f; // Expand wake
            p.x += p.vx * delta;
            p.y += p.vy * delta;

            if (p.life <= 0) {
                particles.remove(i);
                pool.add(p);
            }
        }
    }

    public void spawn(float x, float y, float speed) {
        Particle p = pool.isEmpty() ? new Particle() : pool.remove(pool.size() - 1);
        p.x = x;
        p.y = y;
        p.maxLife = 0.8f + (speed / 300f);
        p.life = p.maxLife;
        p.size = 3f;
        // Add a tiny bit of random drift
        p.vx = (MathUtils.random() - 0.5f) * 5f;
        p.vy = (MathUtils.random() - 0.5f) * 5f;
        particles.add(p);
    }

    public void draw(ShapeRenderer shape) {
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            // Use different colors for better look? Light blue/white
            shape.setColor(0.8f, 0.9f, 1f, alpha * 0.4f);
            shape.circle(p.x, p.y, p.size);
        }
    }

    public void clear() {
        pool.addAll(particles);
        particles.clear();
    }
}
