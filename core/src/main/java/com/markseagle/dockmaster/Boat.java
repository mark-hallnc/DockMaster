package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    public BoatDefinition profile;

    public float x, y;
    public float angle; // in degrees
    public Vector2 velocity = new Vector2();
    public float damage = 0;
    public boolean active = true;
    public long boatValue;

    public Polygon bounds;
    private float flashTimer = 0;

    public Boat(float x, float y, float startAngle, BoatDefinition profile) {
        this.profile = profile;
        this.x = x;
        this.y = y;
        this.angle = startAngle;

        initBounds();
    }

    private void initBounds() {
        float l = profile.length;
        float w = profile.width;
        float[] vertices = new float[] {
            -l/2, -w/2,
            l/2, -w/2,
            l/2 + 10, 0,
            l/2, w/2,
            -l/2, w/2
        };
        bounds = new Polygon(vertices);
        updateBounds();
    }

    public void update(float delta, InputController input, LevelDefinition level) {
        if (!active) return;
        if (flashTimer > 0) flashTimer -= delta;

        Vector2 forwardDir = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
        float forwardVelocityMag = velocity.dot(forwardDir);

        // Steering
        float turnModifier = MathUtils.clamp(Math.abs(forwardVelocityMag) / 80f, profile.lowSpeedTurnFactor, 1.0f);
        if (input.left) angle += profile.turnRate * turnModifier * delta;
        if (input.right) angle -= profile.turnRate * turnModifier * delta;

        // Thrust
        if (input.forward) {
            velocity.add(
                forwardDir.x * profile.forwardThrust * delta,
                forwardDir.y * profile.forwardThrust * delta
            );
        }
        if (input.reverse) {
            velocity.add(
                -forwardDir.x * profile.reverseThrust * delta,
                -forwardDir.y * profile.reverseThrust * delta
            );
        }

        // Environmental Forces
        if (level != null) {
            Vector2 envForce = new Vector2(level.windForce);
            for (CurrentZone zone : level.currentZones) {
                if (zone.bounds.contains(x, y)) {
                    envForce.add(zone.force);
                }
            }
            velocity.add(envForce.x * delta, envForce.y * delta);
        }

        // Apply Natural Water Drag
        velocity.scl((float) Math.pow(profile.waterDrag, delta * 60f));

        // Speed caps
        if (forwardVelocityMag > profile.maxForwardSpeed) {
            velocity.setLength(profile.maxForwardSpeed);
        } else if (forwardVelocityMag < -profile.maxReverseSpeed) {
            velocity.setLength(profile.maxReverseSpeed);
        }

        // Sideways drift
        if (velocity.len() > 0.1f) {
            Vector2 forwardVel = new Vector2(forwardDir).scl(forwardVelocityMag);
            Vector2 sidewaysVel = new Vector2(velocity).sub(forwardVel);
            sidewaysVel.scl((float) Math.pow(profile.sideDrag, delta * 60f));
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

    public float handleCollision(float impactSpeed) {
        float dmg = 0;
        if (impactSpeed > 15f) {
            dmg = (impactSpeed - 15f) * 0.2f;
            damage = Math.min(100, damage + dmg);
            if (damage >= 100) {
                active = false;
                velocity.set(0, 0);
            }
            flashTimer = 0.2f;
        }
        velocity.scl(-0.4f);
        return dmg;
    }

    public void applyValueLoss() {
        // Just for level end visual, actual persistence handled in DockMasterGame/ProgressManager
        boatValue = profile.value - (long)(damage * (profile.value / 100f));
        if (boatValue < 0) boatValue = 0;
    }

    public void draw(ShapeRenderer shape) {
        Color boatColor = damage >= 100 ? Color.GRAY : profile.color;
        if (flashTimer > 0) {
            boatColor = Color.RED;
        }
        shape.setColor(boatColor);
        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        float l = profile.length;
        float w = profile.width;

        shape.rect(-l / 2, -w / 2, l * 0.7f, w);
        shape.triangle(
            l * 0.2f, -w / 2,
            l * 0.2f, w / 2,
            l / 2 + 10, 0
        );
        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    public void reset(float x, float y, float angle, float startDamage) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.velocity.set(0, 0);
        this.damage = startDamage;
        this.active = startDamage < 100;
        updateBounds();
    }

    public String getThrottleState(InputController input) {
        if (input.forward) return "Forward";
        if (input.reverse) return "Reverse";
        return "Neutral";
    }
}
