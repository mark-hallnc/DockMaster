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
        // Level 1: Local Marina - Wide Open Slip
        LevelDefinition lvl1 = new LevelDefinition(
            "Local Marina", "Wide Open Slip",
            400, 100, 90,
            350, 450, 100, 100,
            500, 30f
        );
        lvl1.targetAngle = 90f;
        lvl1.addDock(0, 550, 800, 50);
        lvl1.addDock(300, 450, 50, 100);
        lvl1.addDock(450, 450, 50, 100);
        levels.add(lvl1);

        // Level 2: Local Marina - Narrow Approach
        LevelDefinition lvl2 = new LevelDefinition(
            "Local Marina", "Narrow Approach",
            100, 100, 0,
            650, 300, 100, 100,
            750, 45f
        );
        lvl2.targetAngle = 0f; // Aligned with the slip direction
        lvl2.addDock(0, 0, 800, 20);
        lvl2.addDock(0, 580, 800, 20);
        lvl2.addDock(600, 200, 20, 300);
        lvl2.addDock(750, 200, 50, 300);
        lvl2.setWind(LIGHT_FORCE, 0);
        levels.add(lvl2);

        // Level 3: Lake Resort - Fuel Dock Challenge
        LevelDefinition lvl3 = new LevelDefinition(
            "Lake Resort", "Fuel Dock Challenge",
            700, 500, 180,
            50, 50, 100, 100,
            1000, 60f
        );
        lvl3.targetAngle = 180f;
        lvl3.addDock(0, 0, 20, 600);
        lvl3.addDock(0, 0, 800, 20);
        lvl3.addDock(150, 0, 20, 400);
        lvl3.addDock(350, 200, 20, 400);
        lvl3.addDock(550, 0, 20, 400);
        lvl3.addCurrentZone(0, 0, 300, 300, 0, -MEDIUM_FORCE);
        levels.add(lvl3);

        // Level 4: Coastal Marina - Crosswind Slip
        LevelDefinition lvl4 = new LevelDefinition(
            "Coastal Marina", "Crosswind Slip",
            100, 500, -45,
            600, 100, 120, 120,
            1250, 50f
        );
        lvl4.targetAngle = -90f; // Facing down
        lvl4.addDock(550, 0, 250, 50);
        lvl4.addDock(550, 50, 50, 200);
        lvl4.addDock(750, 50, 50, 200);
        lvl4.setWind(-MEDIUM_FORCE, 0);
        lvl4.addCurrentZone(400, 0, 400, 300, 0, LIGHT_FORCE);
        levels.add(lvl4);
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
