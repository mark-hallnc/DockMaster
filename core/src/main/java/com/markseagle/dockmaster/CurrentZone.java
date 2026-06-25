package com.markseagle.dockmaster;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class CurrentZone {
    public Rectangle bounds;
    public Vector2 force;

    public CurrentZone(float x, float y, float w, float h, float forceX, float forceY) {
        this.bounds = new Rectangle(x, y, w, h);
        this.force = new Vector2(forceX, forceY);
    }
}
