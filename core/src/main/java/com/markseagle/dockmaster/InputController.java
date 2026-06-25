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
    public boolean nextPressed, retryPressed;

    private Rectangle btnLeft, btnRight, btnFwd, btnRev, btnBrake;
    private Rectangle btnNext, btnRetry;

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

        // Results screen buttons
        btnRetry = new Rectangle(worldWidth / 2 - 110, 150, 100, 50);
        btnNext = new Rectangle(worldWidth / 2 + 10, 150, 100, 50);
    }

    public void update(Viewport viewport, boolean isResultsScreen) {
        forward = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        reverse = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        braking = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        retryPressed = Gdx.input.isKeyJustPressed(Input.Keys.R);
        nextPressed = Gdx.input.isKeyJustPressed(Input.Keys.N);

        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                Vector2 touch = new Vector2(Gdx.input.getX(i), Gdx.input.getY(i));
                viewport.unproject(touch);

                if (!isResultsScreen) {
                    if (btnLeft.contains(touch)) left = true;
                    if (btnRight.contains(touch)) right = true;
                    if (btnFwd.contains(touch)) forward = true;
                    if (btnRev.contains(touch)) reverse = true;
                    if (btnBrake.contains(touch)) braking = true;
                } else {
                    if (btnRetry.contains(touch) && Gdx.input.justTouched()) retryPressed = true;
                    if (btnNext.contains(touch) && Gdx.input.justTouched()) nextPressed = true;
                }
            }
        }
    }

    public void drawShapes(ShapeRenderer shape, boolean isResultsScreen) {
        if (!isResultsScreen) {
            drawButton(shape, btnLeft, left);
            drawButton(shape, btnRight, right);
            drawButton(shape, btnFwd, forward);
            drawButton(shape, btnRev, reverse);
            drawButton(shape, btnBrake, braking);
        } else {
            drawButton(shape, btnRetry, false);
            drawButton(shape, btnNext, false);
        }
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font, boolean isResultsScreen) {
        font.setColor(Color.WHITE);
        if (!isResultsScreen) {
            font.draw(batch, "L", btnLeft.x + 40, btnLeft.y + 60);
            font.draw(batch, "R", btnRight.x + 40, btnRight.y + 60);
            font.draw(batch, "FWD", btnFwd.x + 25, btnFwd.y + 60);
            font.draw(batch, "REV", btnRev.x + 25, btnRev.y + 60);
            font.draw(batch, "BRK", btnBrake.x + 25, btnBrake.y + 60);
        } else {
            font.draw(batch, "RETRY", btnRetry.x + 20, btnRetry.y + 35);
            font.draw(batch, "NEXT", btnNext.x + 25, btnNext.y + 35);
        }
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
