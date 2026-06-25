package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class Dock {
    public Rectangle bounds;
    public Rectangle slipZone;
    public Polygon collisionPoly;
    public float targetAngle = 90;

    private float dockingTimer = 0;
    private final float DOCKING_TIME_REQUIRED = 1.5f;
    public boolean successfullyDocked = false;

    public Dock(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
        slipZone = new Rectangle(x + width * 0.2f, y + height * 0.2f, width * 0.6f, height * 0.6f);

        float[] vertices = new float[] {
            0, 0,
            width, 0,
            width, height,
            0, height
        };
        collisionPoly = new Polygon(vertices);
        collisionPoly.setPosition(x, y);
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
        if (Intersector.overlapConvexPolygons(boat.bounds, collisionPoly)) {
            return true;
        }
        return false;
    }

    public boolean isInsideSlipZone(Boat boat) {
        // Check if boat center is in slip zone
        if (slipZone.contains(boat.x, boat.y)) {
            // Check speed
            if (boat.velocity.len() < 25f) {
                // Check angle
                float angleDiff = Math.abs(boat.angle % 360 - targetAngle);
                if (angleDiff > 180) angleDiff = 360 - angleDiff;
                return angleDiff < 20f;
            }
        }
        return false;
    }

    public float getDockingProgress() {
        return Math.min(1.0f, dockingTimer / DOCKING_TIME_REQUIRED);
    }

    public void draw(ShapeRenderer shape) {
        // Main dock structure (Brown)
        shape.set(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.4f, 0.26f, 0.13f, 1f);
        shape.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Slip zone
        if (successfullyDocked) {
            shape.setColor(0f, 1f, 0f, 0.5f); // Solid green
        } else {
            shape.setColor(1f, 1f, 0f, 0.3f); // Yellowish
        }
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);

        // Progress bar if docking
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
