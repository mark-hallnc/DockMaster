package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class BoatCatalog {
    private List<BoatDefinition> boats = new ArrayList<>();

    public BoatCatalog() {
        // Boat 1: Marina Skiff - Balanced starter boat
        boats.add(new BoatDefinition(
            "skiff", "Marina Skiff", "Balanced starter boat", 10000,
            280f, 140f, 0.985f, 0.82f, 160f, 0.4f, 350f, 120f,
            Color.WHITE, 40f, 20f
        ));

        // Boat 2: Sport Runabout - Faster but driftier
        boats.add(new BoatDefinition(
            "runabout", "Sport Runabout", "Fast and agile", 18000,
            380f, 180f, 0.99f, 0.75f, 180f, 0.3f, 500f, 180f,
            Color.CYAN, 35f, 18f
        ));

        // Boat 3: Work Pontoon - Slow and stable
        boats.add(new BoatDefinition(
            "pontoon", "Work Pontoon", "Slow but very stable", 14000,
            220f, 120f, 0.97f, 0.92f, 120f, 0.6f, 250f, 100f,
            Color.LIGHT_GRAY, 50f, 30f
        ));

        // Boat 4: Premium Cruiser - Coming Soon
        boats.add(new BoatDefinition(
            "cruiser", "Premium Cruiser", "Coming Soon", 50000,
            320f, 160f, 0.988f, 0.88f, 140f, 0.5f, 400f, 150f,
            Color.GOLD, 60f, 25f
        ));
    }

    public List<BoatDefinition> getBoats() {
        return boats;
    }

    public BoatDefinition getBoatById(String id) {
        for (BoatDefinition b : boats) {
            if (b.id.equals(id)) return b;
        }
        return boats.get(0);
    }
}
