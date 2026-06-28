package com.markseagle.dockmaster;

public class ControlTuning {
    public final float throttleRampUp;
    public final float throttleRampDown;
    public final float reverseRamp;
    public final float steeringResponse;
    public final float steeringSmoothing;
    public final float minSteerageSpeed;
    public final float fullSteerageSpeed;
    public final float reverseSteeringMultiplier;
    public final float coastSteeringMultiplier;

    // Stern Pivot Tuning
    public final float lowSpeedSternPivotBlend;
    public final float highSpeedSternPivotBlend;
    public final float sternPivotFullSpeed;

    public ControlTuning(float throttleRampUp, float throttleRampDown, float reverseRamp,
                         float steeringResponse, float steeringSmoothing,
                         float minSteerageSpeed, float fullSteerageSpeed,
                         float reverseSteeringMultiplier, float coastSteeringMultiplier,
                         float lowSpeedSternPivotBlend, float highSpeedSternPivotBlend, float sternPivotFullSpeed) {
        this.throttleRampUp = throttleRampUp;
        this.throttleRampDown = throttleRampDown;
        this.reverseRamp = reverseRamp;
        this.steeringResponse = steeringResponse;
        this.steeringSmoothing = steeringSmoothing;
        this.minSteerageSpeed = minSteerageSpeed;
        this.fullSteerageSpeed = fullSteerageSpeed;
        this.reverseSteeringMultiplier = reverseSteeringMultiplier;
        this.coastSteeringMultiplier = coastSteeringMultiplier;
        this.lowSpeedSternPivotBlend = lowSpeedSternPivotBlend;
        this.highSpeedSternPivotBlend = highSpeedSternPivotBlend;
        this.sternPivotFullSpeed = sternPivotFullSpeed;
    }

    public static ControlTuning getPreset(String name) {
        if ("arcade".equalsIgnoreCase(name)) {
            // Arcade: Snappy, center-biased steering
            return new ControlTuning(5.0f, 6.0f, 4.0f, 1.15f, 14.0f, 4f, 55f, 0.85f, 0.65f, 0.35f, 0.1f, 120f);
        } else if ("realistic".equalsIgnoreCase(name)) {
            // Realistic: Heavier, heavy stern bias
            return new ControlTuning(1.8f, 2.5f, 1.5f, 0.85f, 7.0f, 12f, 100f, 0.5f, 0.3f, 0.75f, 0.25f, 100f);
        } else {
            // Default to balanced
            return new ControlTuning(3.0f, 4.0f, 2.5f, 1.0f, 10.0f, 8f, 80f, 0.65f, 0.45f, 0.55f, 0.2f, 100f);
        }
    }
}
