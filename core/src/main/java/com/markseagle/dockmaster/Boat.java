package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    public BoatDefinition profile;

    public float x, y;
    public float previousX, previousY, previousAngle;
    public float angle; // in degrees
    public Vector2 velocity = new Vector2();
    public float damage = 0;
    public boolean active = true;
    public long boatValue;

    public Polygon bounds;
    private float flashTimer = 0;

    // Upgrade Levels
    public int engineLevel = 0;
    public int steeringLevel = 0;
    public int hullLevel = 0;
    public int reverseLevel = 0;

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

    public Vector2 getSternPos() {
        float l = profile.length;
        return new Vector2(
            x - MathUtils.cosDeg(angle) * (l / 2),
            y - MathUtils.sinDeg(angle) * (l / 2)
        );
    }

    public Vector2 getBowPos() {
        float l = profile.length;
        return new Vector2(
            x + MathUtils.cosDeg(angle) * (l / 2 + 10),
            y + MathUtils.sinDeg(angle) * (l / 2 + 10)
        );
    }

    public void update(float delta, InputController input, LevelDefinition level) {
        if (!active) return;
        if (flashTimer > 0) flashTimer -= delta;

        previousX = x;
        previousY = y;
        previousAngle = angle;

        Vector2 forwardDir = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
        float forwardVelocityMag = velocity.dot(forwardDir);

        // Effective Stats with Upgrades
        float effectiveThrust = profile.forwardThrust * (1.0f + engineLevel * 0.05f);
        float effectiveMaxSpeed = profile.maxForwardSpeed * (1.0f + engineLevel * 0.05f);
        float effectiveTurnRate = profile.turnRate * (1.0f + steeringLevel * 0.05f);
        float effectiveReverseThrust = profile.reverseThrust * (1.0f + reverseLevel * 0.07f);

        // Steering - No steerage at dead stop
        boolean hasSteerage = input.forward || input.reverse || Math.abs(forwardVelocityMag) > 8f || velocity.len() > 15f;
        if (hasSteerage) {
            float turnModifier = MathUtils.clamp(Math.abs(forwardVelocityMag) / 80f, profile.lowSpeedTurnFactor, 1.0f);
            if (input.left) angle += effectiveTurnRate * turnModifier * delta;
            if (input.right) angle -= effectiveTurnRate * turnModifier * delta;
        }

        // Thrust
        if (input.forward) {
            velocity.add(
                forwardDir.x * effectiveThrust * delta,
                forwardDir.y * effectiveThrust * delta
            );
        }
        if (input.reverse) {
            velocity.add(
                -forwardDir.x * effectiveReverseThrust * delta,
                -forwardDir.y * effectiveReverseThrust * delta
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

        // Re-calculate forward velocity for accurate capping
        forwardVelocityMag = velocity.dot(forwardDir);

        // Speed caps
        if (forwardVelocityMag > effectiveMaxSpeed) {
            velocity.setLength(effectiveMaxSpeed);
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

    public void updateBounds() {
        bounds.setPosition(x, y);
        bounds.setRotation(angle);
    }

    public float handleCollision(float impactSpeed) {
        float dmg = 0;
        if (impactSpeed > 15f) {
            dmg = (impactSpeed - 15f) * 0.2f;
            // Hull Upgrade Reduction
            dmg *= (1.0f - hullLevel * 0.1f);

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
        float retentionFactor = 1.0f - hullLevel * 0.05f; // Keep 5% more value per hull level
        boatValue = profile.value - (long)(damage * (profile.value / 100f) * retentionFactor);
        if (boatValue < 0) boatValue = 0;
    }

    public void draw(ShapeRenderer shape) {
        Color boatColor = damage >= 100 ? Color.GRAY : profile.color;
        if (flashTimer > 0) {
            boatColor = Color.RED;
        }

        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        float l = profile.length * profile.visualScale;
        float w = profile.width * profile.visualScale;

        // 1. Hull Shadow/Outline (slightly larger)
        shape.setColor(0, 0, 0, 0.3f);
        shape.rect(-l / 2 - 2, -w / 2 - 2, l * 0.7f + 4, w + 4);
        shape.triangle(l * 0.2f - 2, -w / 2 - 2, l * 0.2f - 2, w / 2 + 2, l / 2 + 12, 0);

        // 2. Main Hull
        shape.setColor(boatColor);
        shape.rect(-l / 2, -w / 2, l * 0.7f, w);
        shape.triangle(l * 0.2f, -w / 2, l * 0.2f, w / 2, l / 2 + 10 * profile.visualScale, 0);

        // 3. Deck/Cabin Details
        shape.setColor(0.9f, 0.9f, 0.9f, 1f); // Off-white deck
        shape.rect(-l * 0.3f, -w * 0.3f, l * 0.4f, w * 0.6f);

        // Window/Windshield
        shape.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shape.rect(0, -w * 0.3f, 4 * profile.visualScale, w * 0.6f);

        // Stern details
        shape.setColor(0.3f, 0.3f, 0.3f, 1f);
        shape.rect(-l/2, -w/4, 4 * profile.visualScale, w/2);

        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    public void reset(float x, float y, float angle, float startDamage) {
        this.x = x;
        this.previousX = x;
        this.y = y;
        this.previousY = y;
        this.angle = angle;
        this.previousAngle = angle;
        this.velocity.set(0, 0);
        this.damage = startDamage;
        this.active = startDamage < 100;
        updateBounds();
    }

    public void revertPosition() {
        this.x = previousX;
        this.y = previousY;
        this.angle = previousAngle;
        updateBounds();
    }

    public void nudgeAway(Vector2 force, float amount) {
        Vector2 dir = new Vector2(force).nor().scl(-amount);
        this.x += dir.x;
        this.y += dir.y;
        updateBounds();
    }

    public String getThrottleState(InputController input) {
        if (input.forward) return "Forward";
        if (input.reverse) return "Reverse";
        return "Neutral";
    }
}
