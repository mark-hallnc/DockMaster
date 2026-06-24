package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Dock {
    public Rectangle bounds;
    public Rectangle slipZone;
    public float targetAngle = 90; // The angle the boat should have when docked

    public Dock(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
        // The slip zone is a smaller area where the boat should end up
        slipZone = new Rectangle(x + width * 0.2f, y + height * 0.2f, width * 0.6f, height * 0.6f);
    }

    public void draw(ShapeRenderer shape) {
        // Main dock structure (Brown)
        shape.set(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.4f, 0.26f, 0.13f, 1f);
        shape.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Slip zone (Green/Yellow highlight)
        shape.setColor(0.5f, 1f, 0.5f, 0.5f);
        shape.rect(slipZone.x, slipZone.y, slipZone.width, slipZone.height);
    }

    public boolean isDocked(Boat boat) {
        // Check if boat center is in slip zone
        if (slipZone.contains(boat.x, boat.y)) {
            // Check speed
            if (boat.velocity.len() < 20f) {
                // Check angle (allow some tolerance)
                float angleDiff = Math.abs(boat.angle % 360 - targetAngle);
                if (angleDiff > 180) angleDiff = 360 - angleDiff;
                return angleDiff < 15f;
            }
        }
        return false;
    }
}
