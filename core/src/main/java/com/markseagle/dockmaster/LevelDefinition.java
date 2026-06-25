package com.markseagle.dockmaster;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class LevelDefinition {
    public String levelName;
    public String destinationName;
    public Vector2 startPos;
    public float startAngle;
    public Rectangle targetZone;
    public List<Rectangle> docks = new ArrayList<>();
    public int basePayout;
    public float parTimeSeconds;

    public LevelDefinition(String destination, String name, float startX, float startY, float angle,
                           float targetX, float targetY, float targetW, float targetH, int payout, float parTime) {
        this.destinationName = destination;
        this.levelName = name;
        this.startPos = new Vector2(startX, startY);
        this.startAngle = angle;
        this.targetZone = new Rectangle(targetX, targetY, targetW, targetH);
        this.basePayout = payout;
        this.parTimeSeconds = parTime;
    }

    public void addDock(float x, float y, float w, float h) {
        docks.add(new Rectangle(x, y, w, h));
    }
}
