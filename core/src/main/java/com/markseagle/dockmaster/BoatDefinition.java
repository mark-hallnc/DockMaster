package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;

public class BoatDefinition {
    public String id;
    public String displayName;
    public String description;
    public long value;

    // Physics
    public float forwardThrust;
    public float reverseThrust;
    public float waterDrag;
    public float sideDrag;
    public float turnRate;
    public float lowSpeedTurnFactor;
    public float maxForwardSpeed;
    public float maxReverseSpeed;

    // Visuals
    public Color color;
    public float length;
    public float width;
    public float visualScale = 1.0f;

    public BoatDefinition(String id, String displayName, String description, long value,
                          float forwardThrust, float reverseThrust, float waterDrag, float sideDrag,
                          float turnRate, float lowSpeedTurnFactor, float maxForwardSpeed, float maxReverseSpeed,
                          Color color, float length, float width) {
        this(id, displayName, description, value, forwardThrust, reverseThrust, waterDrag, sideDrag, turnRate, lowSpeedTurnFactor, maxForwardSpeed, maxReverseSpeed, color, length, width, 1.0f);
    }

    public BoatDefinition(String id, String displayName, String description, long value,
                          float forwardThrust, float reverseThrust, float waterDrag, float sideDrag,
                          float turnRate, float lowSpeedTurnFactor, float maxForwardSpeed, float maxReverseSpeed,
                          Color color, float length, float width, float visualScale) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.value = value;
        this.forwardThrust = forwardThrust;
        this.reverseThrust = reverseThrust;
        this.waterDrag = waterDrag;
        this.sideDrag = sideDrag;
        this.turnRate = turnRate;
        this.lowSpeedTurnFactor = lowSpeedTurnFactor;
        this.maxForwardSpeed = maxForwardSpeed;
        this.maxReverseSpeed = maxReverseSpeed;
        this.color = color;
        this.length = length;
        this.width = width;
        this.visualScale = visualScale;
    }
}
