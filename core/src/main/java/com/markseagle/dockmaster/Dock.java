package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.math.MathUtils;

public class Dock {
    public Rectangle slipZone;
    public List<Polygon> collisionPolys = new ArrayList<>();
    public float targetAngle = 90;

    // Level-specific tolerances
    public float dockingMaxSpeed = 35f;
    public float dockingAngleTolerance = 30f;
    public float dockingHoldTime = 1.8f;
    public float dockingZonePadding = 0f;

    private float dockingTimer = 0;
    public boolean successfullyDocked = false;

    // Debugging
    public String lastFailureReason = "outside target";

    public Dock() {
    }

    public void setLevel(LevelDefinition level) {
        this.slipZone = new Rectangle(level.targetZone);
        this.collisionPolys.clear();
        for (Rectangle r : level.docks) {
            float[] vertices = new float[] {
                0, 0,
                r.width, 0,
                r.width, r.height,
                0, r.height
            };
            Polygon poly = new Polygon(vertices);
            poly.setPosition(r.x, r.y);
            collisionPolys.add(poly);
        }
        this.targetAngle = level.targetAngle;
        this.dockingMaxSpeed = level.dockingMaxSpeed;
        this.dockingAngleTolerance = level.dockingAngleTolerance;
        this.dockingHoldTime = level.dockingHoldTime;
        this.dockingZonePadding = level.dockingZonePadding;
        reset();
    }

    public void update(Boat boat, float delta) {
        if (successfullyDocked) return;

        if (isInsideSlipZone(boat)) {
            dockingTimer += delta;
            lastFailureReason = "stabilizing";
            if (dockingTimer >= dockingHoldTime) {
                successfullyDocked = true;
                lastFailureReason = "docked";
            }
        } else {
            dockingTimer = 0;
            // Reason is updated inside isInsideSlipZone
        }
    }

    public boolean checkCollision(Boat boat) {
        for (Polygon p : collisionPolys) {
            if (Intersector.overlapConvexPolygons(boat.bounds, p)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInsideSlipZone(Boat boat) {
        if (slipZone == null) return false;

        // More forgiving check with padding
        Rectangle checkZone = new Rectangle(slipZone.x - dockingZonePadding, slipZone.y - dockingZonePadding,
                                          slipZone.width + dockingZonePadding * 2, slipZone.height + dockingZonePadding * 2);

        // Boat center must be inside
        if (!checkZone.contains(boat.x, boat.y)) {
            lastFailureReason = "outside target";
            return false;
        }

        // Speed check
        if (boat.velocity.len() >= dockingMaxSpeed) {
            lastFailureReason = "too fast";
            return false;
        }

        // Angle check
        float angleDiff = Math.abs(boat.angle % 360 - targetAngle);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;
        if (angleDiff > dockingAngleTolerance) {
            lastFailureReason = "bad angle";
            return false;
        }

        return true;
    }

    public float getDockingProgress() {
        return Math.min(1.0f, dockingTimer / dockingHoldTime);
    }

    private float glowTimer = 0;

    public void drawDocks(ShapeRenderer shape) {
        shape.set(ShapeRenderer.ShapeType.Filled);

        // 1. Draw Docks/Obstacles
        for (Polygon p : collisionPolys) {
            float x = p.getX();
            float y = p.getY();
            float w = p.getVertices()[2];
            float h = p.getVertices()[5];

            // Base shadow
            shape.setColor(0, 0, 0, 0.2f);
            shape.rect(x+4, y-4, w, h);

            // Dock body (Brown)
            shape.setColor(0.45f, 0.28f, 0.15f, 1f);
            shape.rect(x, y, w, h);

            // Plank lines
            shape.setColor(0.35f, 0.2f, 0.1f, 1f);
            if (w > h) {
                for (float lx = x + 10; lx < x + w; lx += 25) {
                    shape.rect(lx, y, 2, h);
                }
            } else {
                for (float ly = y + 10; ly < y + h; ly += 25) {
                    shape.rect(x, ly, w, 2);
                }
            }

            // Pilings/Posts at corners
            shape.setColor(0.2f, 0.1f, 0f, 1f);
            float ps = 6f; // piling size
            shape.circle(x, y, ps);
            shape.circle(x + w, y, ps);
            shape.circle(x, y + h, ps);
            shape.circle(x + w, y + h, ps);

            // Edge highlights
            shape.setColor(0.6f, 0.6f, 0.6f, 0.5f);
            shape.rect(x, y + h - 2, w, 2); // Top edge light
        }
    }

    public void drawSlipZoneFilled(ShapeRenderer shape, Boat boat) {
        if (slipZone == null) return;
        glowTimer += Gdx.graphics.getDeltaTime();

        // 2. Slip zone
        if (successfullyDocked) {
            shape.setColor(0f, 1f, 0f, 0.6f);
        } else {
            // Pulsing color based on boat status
            float pulse = (float)(Math.sin(glowTimer * 6f) + 1.0f) * 0.15f;
            Color zoneColor = new Color(0.2f, 0.6f, 1.0f, 0.2f + pulse); // Default blue-ish

            if (slipZone.contains(boat.x, boat.y) ||
               (dockingZonePadding > 0 && new Rectangle(slipZone.x - dockingZonePadding, slipZone.y - dockingZonePadding,
                slipZone.width + dockingZonePadding * 2, slipZone.height + dockingZonePadding * 2).contains(boat.x, boat.y))) {

                if (isInsideSlipZone(boat)) {
                    zoneColor = new Color(0.5f, 1f, 0.5f, 0.4f + pulse); // Valid stabilizing
                } else if (boat.velocity.len() >= dockingMaxSpeed) {
                    zoneColor = new Color(1f, 0.3f, 0.1f, 0.4f + pulse); // Too fast
                } else {
                    zoneColor = new Color(1f, 1f, 0f, 0.4f + pulse); // Bad angle
                }
            }
            shape.setColor(zoneColor);
        }
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);

        // 3. Progress bar
        if (dockingTimer > 0 && !successfullyDocked) {
            shape.setColor(Color.LIME);
            shape.rect(slipZone.x, slipZone.y - 18, slipZone.width * getDockingProgress(), 10);
        }
    }

    public void drawSlipZoneLines(ShapeRenderer shape, Boat boat) {
        if (slipZone == null) return;

        // Target outline
        shape.setColor(1, 1, 1, 0.5f);
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);

        // Directional indicator
        float centerX = slipZone.x + slipZone.width / 2;
        float centerY = slipZone.y + slipZone.height / 2;
        float arrowLen = 30;
        float dx = MathUtils.cosDeg(targetAngle) * arrowLen;
        float dy = MathUtils.sinDeg(targetAngle) * arrowLen;
        shape.line(centerX - dx, centerY - dy, centerX + dx, centerY + dy);

        // 3. Progress bar outline
        if (dockingTimer > 0 && !successfullyDocked) {
            shape.setColor(Color.WHITE);
            shape.rect(slipZone.x, slipZone.y - 18, slipZone.width, 10);
        }
    }

    public void reset() {
        dockingTimer = 0;
        successfullyDocked = false;
        lastFailureReason = "outside target";
    }
}
