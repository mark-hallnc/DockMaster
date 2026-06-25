package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    public float x, y;
    public float angle; // in degrees
    public Vector2 velocity = new Vector2();
    public float damage = 0;
    public boolean active = true;

    private final float THRUST = 240f;
    private final float REVERSE_THRUST = 120f;
    private final float DRAG = 0.985f;
    private final float BRAKE_DRAG = 0.94f;
    private final float TURN_SPEED = 140f;
    private final float DRIFT_FACTOR = 0.85f;

    public Polygon bounds;
    private final float length = 40;
    private final float width = 20;

    public Boat(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 90;

        float[] vertices = new float[] {
            -length/2, -width/2,
            length/2, -width/2,
            length/2 + 10, 0,
            length/2, width/2,
            -length/2, width/2
        };
        bounds = new Polygon(vertices);
        updateBounds();
    }

    public void update(float delta, InputController input) {
        if (!active) return;

        float currentSpeed = velocity.len();

        // Turning
        float turnModifier = MathUtils.clamp(currentSpeed / 80f, 0.3f, 1.0f);
        if (input.left) angle += TURN_SPEED * turnModifier * delta;
        if (input.right) angle -= TURN_SPEED * turnModifier * delta;

        // Thrust
        if (input.forward) {
            velocity.add(
                MathUtils.cosDeg(angle) * THRUST * delta,
                MathUtils.sinDeg(angle) * THRUST * delta
            );
        }
        if (input.reverse) {
            velocity.add(
                -MathUtils.cosDeg(angle) * REVERSE_THRUST * delta,
                -MathUtils.sinDeg(angle) * REVERSE_THRUST * delta
            );
        }

        // Apply drag
        float dragToApply = input.braking ? BRAKE_DRAG : DRAG;
        velocity.scl((float) Math.pow(dragToApply, delta * 60f));

        // Sideways drift
        if (velocity.len() > 0.1f) {
            Vector2 forwardDir = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
            float forwardVelocityMag = velocity.dot(forwardDir);
            Vector2 forwardVelocity = new Vector2(forwardDir).scl(forwardVelocityMag);
            Vector2 sidewaysVelocity = new Vector2(velocity).sub(forwardVelocity);
            sidewaysVelocity.scl((float) Math.pow(DRIFT_FACTOR, delta * 60f));
            velocity.set(forwardVelocity.add(sidewaysVelocity));
        }

        // Update position
        x += velocity.x * delta;
        y += velocity.y * delta;

        updateBounds();
    }

    private void updateBounds() {
        bounds.setPosition(x, y);
        bounds.setRotation(angle);
    }

    public void handleCollision(float impactSpeed) {
        // Damage based on speed. Minimal damage for slow bumps.
        if (impactSpeed > 20f) {
            float dmg = (impactSpeed - 20f) * 0.2f;
            damage = Math.min(100, damage + dmg);
            if (damage >= 100) {
                active = false;
                velocity.set(0, 0);
            }
        }
        // Bounce back slightly
        velocity.scl(-0.5f);
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(damage >= 100 ? Color.GRAY : Color.WHITE);

        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        // Hull
        shape.rect(-length / 2, -width / 2, length * 0.7f, width);
        // Bow
        shape.triangle(
            length * 0.2f, -width / 2,
            length * 0.2f, width / 2,
            length / 2 + 10, 0
        );

        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    public void reset(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 90;
        this.velocity.set(0, 0);
        this.damage = 0;
        this.active = true;
        updateBounds();
    }
}
