package com.markseagle.dockmaster;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    private List<LevelDefinition> levels = new ArrayList<>();
    private int currentLevelIndex = 0;

    public static final float LIGHT_FORCE = 15f;
    public static final float MEDIUM_FORCE = 30f;
    public static final float STRONG_FORCE = 50f;

    public LevelManager() {
        // --- DESTINATION 1: LOCAL MARINA ---

        // Level 1: Wide Open Slip
        LevelDefinition lvl1 = new LevelDefinition(
            "Local Marina", "Wide Open Slip",
            400, 100, 90,
            350, 450, 100, 100,
            500, 45f
        );
        lvl1.targetAngle = 90f;
        lvl1.dockingMaxSpeed = 55f;
        lvl1.dockingAngleTolerance = 60f;
        lvl1.dockingHoldTime = 1.0f;
        lvl1.dockingZonePadding = 20f;
        lvl1.addDock(0, 550, 800, 50);
        lvl1.addDock(300, 450, 50, 100);
        lvl1.addDock(450, 450, 50, 100);
        levels.add(lvl1);

        // Level 2: Narrow Approach
        LevelDefinition lvl2 = new LevelDefinition(
            "Local Marina", "Narrow Approach",
            100, 100, 0,
            635, 300, 100, 100,
            750, 50f
        );
        lvl2.targetAngle = 90f; // Parallel to docks
        lvl2.dockingMaxSpeed = 50f;
        lvl2.dockingAngleTolerance = 55f;
        lvl2.dockingHoldTime = 1.1f;
        lvl2.dockingZonePadding = 20f;
        lvl2.addDock(0, 0, 800, 20);
        lvl2.addDock(0, 580, 800, 20);
        lvl2.addDock(600, 200, 20, 300);
        lvl2.addDock(750, 200, 50, 300);
        lvl2.setWind(LIGHT_FORCE, 0);
        levels.add(lvl2);

        // Level 3: Fuel Dock Challenge
        LevelDefinition lvl3 = new LevelDefinition(
            "Local Marina", "Fuel Dock Challenge",
            700, 500, 180,
            50, 50, 100, 100,
            1000, 60f
        );
        lvl3.targetAngle = 180f;
        lvl3.dockingMaxSpeed = 45f;
        lvl3.dockingAngleTolerance = 50f;
        lvl3.dockingHoldTime = 1.2f;
        lvl3.dockingZonePadding = 10f;
        lvl3.addDock(0, 0, 20, 600);
        lvl3.addDock(0, 0, 800, 20);
        lvl3.addDock(150, 0, 20, 400);
        lvl3.addDock(350, 200, 20, 400);
        lvl3.addDock(550, 0, 20, 400);
        lvl3.addCurrentZone(0, 0, 300, 300, 0, -MEDIUM_FORCE);
        levels.add(lvl3);

        // Level 4: Evening Crosswind
        LevelDefinition lvl4 = new LevelDefinition(
            "Local Marina", "Evening Crosswind",
            100, 500, -45,
            600, 100, 120, 120,
            1250, 65f
        );
        lvl4.targetAngle = -90f;
        lvl4.dockingMaxSpeed = 40f;
        lvl4.dockingAngleTolerance = 40f;
        lvl4.dockingHoldTime = 1.5f;
        lvl4.addDock(550, 0, 250, 50);
        lvl4.addDock(550, 50, 50, 200);
        lvl4.addDock(750, 50, 50, 200);
        lvl4.setWind(-MEDIUM_FORCE, 0);
        levels.add(lvl4);

        // --- DESTINATION 2: LAKE RESORT ---

        // Level 5: Resort Guest Dock
        LevelDefinition lvl5 = new LevelDefinition(
            "Lake Resort", "Resort Guest Dock",
            400, 50, 90,
            380, 500, 140, 80,
            1500, 70f
        );
        lvl5.targetAngle = 90f;
        lvl5.addDock(0, 580, 800, 20);
        lvl5.addDock(250, 450, 20, 150);
        lvl5.addDock(530, 450, 20, 150);
        levels.add(lvl5);

        // Level 6: Pontoon Alley
        LevelDefinition lvl6 = new LevelDefinition(
            "Lake Resort", "Pontoon Alley",
            50, 300, 0,
            700, 280, 80, 140,
            1800, 75f
        );
        lvl6.targetAngle = 0f;
        lvl6.addDock(200, 0, 40, 200);
        lvl6.addDock(200, 400, 40, 200);
        lvl6.addDock(450, 100, 40, 400);
        lvl6.addCurrentZone(400, 0, 200, 600, 0, LIGHT_FORCE);
        levels.add(lvl6);

        // Level 7: Buoy Lane
        LevelDefinition lvl7 = new LevelDefinition(
            "Lake Resort", "Buoy Lane",
            700, 100, 135,
            100, 100, 120, 120,
            2200, 80f
        );
        lvl7.targetAngle = 180f;
        lvl7.addDock(0, 0, 800, 20);
        lvl7.addDock(0, 0, 20, 600);
        lvl7.addDock(250, 250, 50, 50);
        lvl7.addDock(500, 400, 50, 50);
        lvl7.setWind(0, LIGHT_FORCE);
        levels.add(lvl7);

        // Level 8: Busy Fuel Pier
        LevelDefinition lvl8 = new LevelDefinition(
            "Lake Resort", "Busy Fuel Pier",
            100, 500, -90,
            600, 450, 150, 100,
            2500, 90f
        );
        lvl8.targetAngle = 90f;
        lvl8.addDock(500, 450, 20, 150);
        lvl8.addDock(780, 450, 20, 150);
        lvl8.addDock(300, 200, 200, 20);
        lvl8.addCurrentZone(500, 300, 300, 300, MEDIUM_FORCE, 0);
        levels.add(lvl8);

        // --- DESTINATION 3: COASTAL HARBOR ---

        // Level 9: Harbor Entrance
        LevelDefinition lvl9 = new LevelDefinition(
            "Coastal Harbor", "Harbor Entrance",
            400, 50, 90,
            50, 450, 100, 100,
            3000, 90f
        );
        lvl9.targetAngle = 180f;
        lvl9.addDock(0, 550, 400, 50);
        lvl9.addDock(0, 400, 50, 150);
        lvl9.addDock(150, 400, 50, 150);
        lvl9.setWind(-MEDIUM_FORCE, 0);
        levels.add(lvl9);

        // Level 10: Cross Current Slip
        LevelDefinition lvl10 = new LevelDefinition(
            "Coastal Harbor", "Cross Current Slip",
            700, 100, 180,
            100, 300, 120, 100,
            3500, 100f
        );
        lvl10.targetAngle = 180f;
        lvl10.addDock(50, 250, 200, 20);
        lvl10.addDock(50, 400, 200, 20);
        lvl10.addCurrentZone(300, 0, 200, 600, 0, STRONG_FORCE);
        levels.add(lvl10);

        // Level 11: Tight Turn Basin
        LevelDefinition lvl11 = new LevelDefinition(
            "Coastal Harbor", "Tight Turn Basin",
            100, 100, 0,
            100, 450, 100, 120,
            4000, 110f
        );
        lvl11.targetAngle = 90f;
        lvl11.addDock(0, 300, 600, 20);
        lvl11.addDock(250, 450, 20, 150);
        lvl11.setWind(LIGHT_FORCE, LIGHT_FORCE);
        levels.add(lvl11);

        // Level 12: Windy Yacht Dock
        LevelDefinition lvl12 = new LevelDefinition(
            "Coastal Harbor", "Windy Yacht Dock",
            700, 500, -135,
            50, 50, 180, 120,
            4500, 120f
        );
        lvl12.targetAngle = -180f;
        lvl12.addDock(0, 0, 800, 20);
        lvl12.addDock(0, 0, 20, 300);
        lvl12.addDock(230, 0, 20, 200);
        lvl12.setWind(0, -STRONG_FORCE);
        levels.add(lvl12);
    }

    public List<LevelDefinition> getLevels() {
        return levels;
    }

    public LevelDefinition getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    public void setCurrentLevel(int index) {
        if (index >= 0 && index < levels.size()) {
            currentLevelIndex = index;
        }
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public void nextLevel() {
        currentLevelIndex = (currentLevelIndex + 1) % levels.size();
    }

    public int getCurrentLevelNumber() {
        return currentLevelIndex + 1;
    }

    public boolean hasNextLevel() {
        return currentLevelIndex < levels.size() - 1;
    }
}
