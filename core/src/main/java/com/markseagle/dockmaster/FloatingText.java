package com.markseagle.dockmaster;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class FloatingText {
    private static class Message {
        String text;
        float x, y;
        float life, maxLife;
        Color color;
    }

    private final List<Message> messages = new ArrayList<>();
    private final List<Message> pool = new ArrayList<>();

    public void spawn(String text, float x, float y, Color color) {
        Message m = pool.isEmpty() ? new Message() : pool.remove(pool.size() - 1);
        m.text = text;
        m.x = x;
        m.y = y;
        m.maxLife = 1.5f;
        m.life = m.maxLife;
        m.color = new Color(color);
        messages.add(m);
    }

    public void update(float delta) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            m.life -= delta;
            m.y += delta * 40f; // Float up
            if (m.life <= 0) {
                messages.remove(i);
                pool.add(m);
            }
        }
    }

    public void draw(SpriteBatch batch, BitmapFont font) {
        for (Message m : messages) {
            float alpha = m.life / m.maxLife;
            font.setColor(m.color.r, m.color.g, m.color.b, alpha);
            font.draw(batch, m.text, m.x, m.y);
        }
        font.setColor(Color.WHITE);
    }

    public void clear() {
        pool.addAll(messages);
        messages.clear();
    }
}
