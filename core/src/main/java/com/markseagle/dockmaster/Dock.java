package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Dock {
    public Rectangle slipZone;
    public List<Polygon> collisionPolys = new ArrayList<>();
    public float targetAngle = 90;

    private float dockingTimer = 0;
    private final float DOCKING_TIME_REQUIRED = 1.5f;
    public boolean successfullyDocked = false;

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
        this.targetAngle = level.startAngle; // Use level's orientation or default
        // Actually targetAngle should probably be defined in LevelDefinition but 90 is fine for now
        reset();
    }

    public void update(Boat boat, float delta) {
        if (successfullyDocked) return;

        if (isInsideSlipZone(boat)) {
            dockingTimer += delta;
            if (dockingTimer >= DOCKING_TIME_REQUIRED) {
                successfullyDocked = true;
            }
        } else {
            dockingTimer = 0;
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
        if (slipZone.contains(boat.x, boat.y)) {
            if (boat.velocity.len() < 30f) {
                float angleDiff = Math.abs(boat.angle % 360 - targetAngle);
                if (angleDiff > 180) angleDiff = 360 - angleDiff;
                return angleDiff < 25f; // More forgiving angle
            }
        }
        return false;
    }

    public float getDockingProgress() {
        return Math.min(1.0f, dockingTimer / DOCKING_TIME_REQUIRED);
    }

    public void draw(ShapeRenderer shape) {
        if (slipZone == null) return;

        // Draw collision obstacles
        shape.set(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.4f, 0.26f, 0.13f, 1f);
        for (Polygon p : collisionPolys) {
            shape.rect(p.getX(), p.getY(), p.getVertices()[2], p.getVertices()[5]);
        }

        // Slip zone
        if (successfullyDocked) {
            shape.setColor(0f, 1f, 0f, 0.5f);
        } else {
            shape.setColor(1f, 1f, 0f, 0.3f);
        }
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);

        // Progress bar
        if (dockingTimer > 0 && !successfullyDocked) {
            shape.setColor(Color.WHITE);
            shape.rect(slipZone.x, slipZone.y - 10, slipZone.width * getDockingProgress(), 5);
        }
    }

    public void reset() {
        dockingTimer = 0;
        successfullyDocked = false;
    }
}
