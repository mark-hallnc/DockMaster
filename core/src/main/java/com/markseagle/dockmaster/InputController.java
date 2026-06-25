package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class InputController {
    public boolean forward, reverse, left, right, braking;

    private Rectangle btnLeft, btnRight, btnFwd, btnRev, btnBrake;
    private final float btnSize = 100f;
    private final float margin = 20f;

    public InputController(float worldWidth, float worldHeight) {
        // Steering on left
        btnLeft = new Rectangle(margin, margin, btnSize, btnSize);
        btnRight = new Rectangle(margin + btnSize + margin, margin, btnSize, btnSize);

        // Throttle on right
        btnBrake = new Rectangle(worldWidth - margin - btnSize, margin, btnSize, btnSize);
        btnRev = new Rectangle(worldWidth - (margin + btnSize) * 2, margin, btnSize, btnSize);
        btnFwd = new Rectangle(worldWidth - (margin + btnSize) * 3, margin, btnSize, btnSize);
    }

    public void update(Viewport viewport) {
        forward = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        reverse = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        braking = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                Vector2 touch = new Vector2(Gdx.input.getX(i), Gdx.input.getY(i));
                viewport.unproject(touch);

                if (btnLeft.contains(touch)) left = true;
                if (btnRight.contains(touch)) right = true;
                if (btnFwd.contains(touch)) forward = true;
                if (btnRev.contains(touch)) reverse = true;
                if (btnBrake.contains(touch)) braking = true;
            }
        }
    }

    public void drawShapes(ShapeRenderer shape) {
        drawButton(shape, btnLeft, left);
        drawButton(shape, btnRight, right);
        drawButton(shape, btnFwd, forward);
        drawButton(shape, btnRev, reverse);
        drawButton(shape, btnBrake, braking);
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);
        font.draw(batch, "L", btnLeft.x + 40, btnLeft.y + 60);
        font.draw(batch, "R", btnRight.x + 40, btnRight.y + 60);
        font.draw(batch, "FWD", btnFwd.x + 25, btnFwd.y + 60);
        font.draw(batch, "REV", btnRev.x + 25, btnRev.y + 60);
        font.draw(batch, "BRK", btnBrake.x + 25, btnBrake.y + 60);
    }

    private void drawButton(ShapeRenderer shape, Rectangle rect, boolean active) {
        if (active) {
            shape.setColor(1, 1, 1, 0.4f);
        } else {
            shape.setColor(1, 1, 1, 0.15f);
        }
        shape.rect(rect.x, rect.y, rect.width, rect.height);
    }
}
