package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    // Tuning Constants
    public static final float FORWARD_THRUST = 280f;
    public static final float REVERSE_THRUST = 140f;
    public static final float WATER_DRAG = 0.985f; // Slower deceleration
    public static final float SIDE_DRAG = 0.82f;
    public static final float TURN_RATE = 160f;
    public static final float LOW_SPEED_TURN_FACTOR = 0.4f;
    public static final float MAX_FORWARD_SPEED = 350f;
    public static final float MAX_REVERSE_SPEED = 120f;

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

        Vector2 forwardDir = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
        float forwardVelocityMag = velocity.dot(forwardDir);

        // Steering - Works better when moving (simulating water flow over rudder)
        float turnModifier = MathUtils.clamp(Math.abs(forwardVelocityMag) / 80f, LOW_SPEED_TURN_FACTOR, 1.0f);
        if (input.left) angle += TURN_RATE * turnModifier * delta;
        if (input.right) angle -= TURN_RATE * turnModifier * delta;

        // Thrust
        if (input.forward) {
            velocity.add(
                forwardDir.x * FORWARD_THRUST * delta,
                forwardDir.y * FORWARD_THRUST * delta
            );
        }
        if (input.reverse) {
            velocity.add(
                -forwardDir.x * REVERSE_THRUST * delta,
                -forwardDir.y * REVERSE_THRUST * delta
            );
        }

        // Apply Natural Water Drag
        velocity.scl((float) Math.pow(WATER_DRAG, delta * 60f));

        // Speed caps (Forward vs Reverse)
        if (forwardVelocityMag > MAX_FORWARD_SPEED) {
            velocity.setLength(MAX_FORWARD_SPEED);
        } else if (forwardVelocityMag < -MAX_REVERSE_SPEED) {
            velocity.setLength(MAX_REVERSE_SPEED); // Simplified cap for reverse
        }

        // Sideways drift (Dampen sideways component much more than forward)
        if (velocity.len() > 0.1f) {
            Vector2 forwardVel = new Vector2(forwardDir).scl(forwardVelocityMag);
            Vector2 sidewaysVel = new Vector2(velocity).sub(forwardVel);
            sidewaysVel.scl((float) Math.pow(SIDE_DRAG, delta * 60f));
            velocity.set(forwardVel.add(sidewaysVel));
        }

        // Final position update
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
        velocity.scl(-0.4f); // Slight bounce
    }

    public void applyValueLoss() {
        boatValue -= (long)(damage * 10);
        if (boatValue < 0) boatValue = 0;
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(damage >= 100 ? Color.GRAY : Color.WHITE);
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

    public String getThrottleState(InputController input) {
        if (input.forward) return "Forward";
        if (input.reverse) return "Reverse";
        return "Neutral";
    }
}
