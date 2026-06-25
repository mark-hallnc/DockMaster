package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    // Tuning Constants
    public static final float THRUST = 260f;
    public static final float REVERSE_THRUST = 130f;
    public static final float DRAG = 0.988f;
    public static final float BRAKE_DRAG = 0.93f;
    public static final float TURN_RATE = 150f;
    public static final float DRIFT_FACTOR = 0.82f;
    public static final float MAX_SPEED = 400f;

    public float x, y;
    public float angle; // in degrees
    public Vector2 velocity = new Vector2();
    public float damage = 0;
    public boolean active = true;
    public long boatValue = 10000;

    public Polygon bounds;
    private final float length = 40;
    private final float width = 20;

    public Boat(float x, float y, float startAngle) {
        this.x = x;
        this.y = y;
        this.angle = startAngle;

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

        // Turning - slightly more responsive if moving slowly
        float turnModifier = MathUtils.clamp(currentSpeed / 60f, 0.4f, 1.0f);
        if (input.left) angle += TURN_RATE * turnModifier * delta;
        if (input.right) angle -= TURN_RATE * turnModifier * delta;

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

        // Speed cap
        if (velocity.len() > MAX_SPEED) {
            velocity.setLength(MAX_SPEED);
        }

        // Sideways drift (dampen sideways component)
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
        if (impactSpeed > 15f) {
            float dmg = (impactSpeed - 15f) * 0.2f;
            damage = Math.min(100, damage + dmg);
            if (damage >= 100) {
                active = false;
                velocity.set(0, 0);
            }
        }
        velocity.scl(-0.5f); // Bounce
    }

    public void applyValueLoss() {
        boatValue -= (long)(damage * 10);
        if (boatValue < 0) boatValue = 0;
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(damage >= 100 ? Color.GRAY : Color.WHITE);

        // Use local transform for boat shape
        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        shape.rect(-length / 2, -width / 2, length * 0.7f, width);
        shape.triangle(
            length * 0.2f, -width / 2,
            length * 0.2f, width / 2,
            length / 2 + 10, 0
        );

        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    public void reset(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.velocity.set(0, 0);
        this.damage = 0;
        this.active = true;
        updateBounds();
    }
}
