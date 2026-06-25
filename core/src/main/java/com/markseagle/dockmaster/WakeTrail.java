package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class WakeTrail {
    private static class Particle {
        float x, y, life, maxLife;
        float size;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> pool = new ArrayList<>();
    private float timer = 0;

    public void update(float delta, float bx, float by, float speed) {
        timer += delta;
        // Spawn rate based on speed
        float spawnRate = 0.05f;
        if (speed > 50 && timer > spawnRate) {
            timer = 0;
            spawn(bx, by, speed);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= delta;
            p.size += delta * 10f; // Expand
            if (p.life <= 0) {
                particles.remove(i);
                pool.add(p);
            }
        }
    }

    private void spawn(float x, float y, float speed) {
        Particle p = pool.isEmpty() ? new Particle() : pool.remove(pool.size() - 1);
        p.x = x;
        p.y = y;
        p.maxLife = 1.0f + (speed / 200f);
        p.life = p.maxLife;
        p.size = 5f;
        particles.add(p);
    }

    public void draw(ShapeRenderer shape) {
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            shape.setColor(1, 1, 1, alpha * 0.3f);
            shape.circle(p.x, p.y, p.size);
        }
    }

    public void clear() {
        pool.addAll(particles);
        particles.clear();
    }
}
