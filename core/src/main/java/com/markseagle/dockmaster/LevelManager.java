package com.markseagle.dockmaster;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    private List<LevelDefinition> levels = new ArrayList<>();
    private int currentLevelIndex = 0;

    public LevelManager() {
        // Level 1: Local Marina - Wide Open Slip
        LevelDefinition lvl1 = new LevelDefinition(
            "Local Marina", "Wide Open Slip",
            400, 100, 90,
            350, 450, 100, 100,
            500, 30f
        );
        lvl1.addDock(0, 550, 800, 50); // North wall
        lvl1.addDock(300, 450, 50, 100); // Left pier
        lvl1.addDock(450, 450, 50, 100); // Right pier
        levels.add(lvl1);

        // Level 2: Local Marina - Narrow Approach
        LevelDefinition lvl2 = new LevelDefinition(
            "Local Marina", "Narrow Approach",
            100, 100, 0,
            650, 300, 100, 100,
            750, 45f
        );
        lvl2.addDock(0, 0, 800, 20); // South wall
        lvl2.addDock(0, 580, 800, 20); // North wall
        lvl2.addDock(600, 200, 20, 300); // Barrier
        lvl2.addDock(750, 200, 50, 300); // Shore
        levels.add(lvl2);

        // Level 3: Lake Resort - Fuel Dock Challenge
        LevelDefinition lvl3 = new LevelDefinition(
            "Lake Resort", "Fuel Dock Challenge",
            700, 500, 180,
            50, 50, 100, 100,
            1000, 60f
        );
        lvl3.addDock(0, 0, 20, 600); // West wall
        lvl3.addDock(0, 0, 800, 20); // South wall
        lvl3.addDock(150, 0, 20, 400); // Obstacle 1
        lvl3.addDock(350, 200, 20, 400); // Obstacle 2
        lvl3.addDock(550, 0, 20, 400); // Obstacle 3
        levels.add(lvl3);
    }

    public LevelDefinition getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    public void nextLevel() {
        currentLevelIndex = (currentLevelIndex + 1) % levels.size();
    }

    public int getCurrentLevelNumber() {
        return currentLevelIndex + 1;
    }
}
