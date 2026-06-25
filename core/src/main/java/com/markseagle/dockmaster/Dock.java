package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
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
    private final float DOCKING_TIME_REQUIRED = 1.8f;
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
        reset();
    }

    public void update(Boat boat, float delta) {
        if (successfullyDocked) return;

        if (isInsideSlipZone(boat)) {
            dockingTimer += delta;
            lastFailureReason = "stabilizing";
            if (dockingTimer >= DOCKING_TIME_REQUIRED) {
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

        // Boat center must be inside
        if (!slipZone.contains(boat.x, boat.y)) {
            lastFailureReason = "outside target";
            return false;
        }

        // Speed check
        if (boat.velocity.len() >= 35f) {
            lastFailureReason = "too fast";
            return false;
        }

        // Angle check
        float angleDiff = Math.abs(boat.angle % 360 - targetAngle);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;
        if (angleDiff > 30f) { // More forgiving
            lastFailureReason = "bad angle";
            return false;
        }

        return true;
    }

    public float getDockingProgress() {
        return Math.min(1.0f, dockingTimer / DOCKING_TIME_REQUIRED);
    }

    private float glowTimer = 0;

    public void draw(ShapeRenderer shape) {
        if (slipZone == null) return;
        glowTimer += Gdx.graphics.getDeltaTime();

        shape.set(ShapeRenderer.ShapeType.Filled);
        for (Polygon p : collisionPolys) {
            float x = p.getX();
            float y = p.getY();
            float w = p.getVertices()[2];
            float h = p.getVertices()[5];
            shape.setColor(0.35f, 0.22f, 0.1f, 1f);
            shape.rect(x, y, w, h);
            shape.setColor(0.25f, 0.15f, 0.05f, 1f);
            if (w > h) {
                for (float lx = x + 10; lx < x + w; lx += 20) shape.rect(lx, y, 2, h);
            } else {
                for (float ly = y + 10; ly < y + h; ly += 20) shape.rect(x, ly, w, 2);
            }
            shape.setColor(0.5f, 0.5f, 0.5f, 1f);
            shape.rect(x-2, y-2, w+4, 4);
            shape.rect(x-2, y+h-2, w+4, 4);
            shape.rect(x-2, y-2, 4, h+4);
            shape.rect(x+w-2, y-2, 4, h+4);
        }

        if (successfullyDocked) {
            shape.setColor(0f, 1f, 0f, 0.6f);
        } else {
            float pulse = (float)(Math.sin(glowTimer * 5f) + 1.0f) * 0.2f;
            shape.setColor(1f, 1f, 0.2f, 0.2f + pulse);
        }
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);

        if (dockingTimer > 0 && !successfullyDocked) {
            shape.setColor(Color.WHITE);
            shape.rect(slipZone.x, slipZone.y - 15, slipZone.width * getDockingProgress(), 8);
        }
    }

    public void reset() {
        dockingTimer = 0;
        successfullyDocked = false;
        lastFailureReason = "outside target";
    }
}
