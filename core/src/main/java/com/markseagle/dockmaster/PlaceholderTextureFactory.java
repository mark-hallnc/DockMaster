package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class PlaceholderTextureFactory {

    public static Texture generateBoat(String type, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Transparent background
        pixmap.fill();

        if (type.equals("skiff")) {
            pixmap.setColor(Color.WHITE);
            // Hull body
            pixmap.fillRectangle(0, height / 4, (int)(width * 0.7f), height / 2);
            // Pointed bow (right)
            pixmap.fillTriangle(
                (int)(width * 0.7f), height / 4,
                (int)(width * 0.7f), (int)(height * 0.75f),
                width, height / 2
            );
            // Simple cockpit
            pixmap.setColor(Color.DARK_GRAY);
            pixmap.fillRectangle(width / 4, height / 3, width / 4, height / 3);
        } else if (type.equals("runabout")) {
            pixmap.setColor(Color.CYAN);
            // Sleeker hull
            pixmap.fillRectangle(0, height / 3, (int)(width * 0.6f), height / 3);
            pixmap.fillTriangle(
                (int)(width * 0.6f), height / 3,
                (int)(width * 0.6f), (int)(height * 0.66f),
                width, height / 2
            );
            // Racing stripe
            pixmap.setColor(Color.WHITE);
            pixmap.fillRectangle(0, height / 2 - 2, (int)(width * 0.8f), 4);
        } else if (type.equals("pontoon")) {
            pixmap.setColor(Color.LIGHT_GRAY);
            // Two pontoons
            pixmap.fillRectangle(0, 0, width, height / 4);
            pixmap.fillRectangle(0, (int)(height * 0.75f), width, height / 4);
            // Deck
            pixmap.setColor(Color.GRAY);
            pixmap.fillRectangle(width / 10, height / 4, (int)(width * 0.8f), height / 2);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static Texture generateDock(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.45f, 0.28f, 0.15f, 1f);
        pixmap.fill();

        // Plank lines
        pixmap.setColor(0.35f, 0.2f, 0.1f, 1f);
        for (int x = 10; x < width; x += 20) {
            pixmap.fillRectangle(x, 0, 2, height);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static Texture generateWater(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.1f, 0.3f, 0.5f, 1f);
        pixmap.fill();

        // Subtle detail
        pixmap.setColor(1, 1, 1, 0.05f);
        for (int i = 0; i < 20; i++) {
            int rx = (int)(Math.random() * width);
            int ry = (int)(Math.random() * height);
            pixmap.drawCircle(rx, ry, (int)(Math.random() * 10));
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static Texture generateBuoy(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        pixmap.setColor(Color.RED);
        pixmap.fillCircle(width / 2, height / 2, width / 2 - 2);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, height / 2 - 4, width, 8);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
