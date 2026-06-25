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
    public boolean forward, reverse, left, right;
    public boolean nextPressed, retryPressed;
    public boolean debugToggled = false;

    private final Rectangle btnLeft, btnRight, btnFwd, btnRev;
    private final Rectangle btnNext, btnRetry;

    private final float btnSize = 120f;
    private final float margin = 30f;

    public InputController(float hudWidth, float hudHeight) {
        // Steering on left side
        btnLeft = new Rectangle(margin, margin, btnSize, btnSize);
        btnRight = new Rectangle(margin + btnSize + 20, margin, btnSize, btnSize);

        // Throttle on right side
        btnRev = new Rectangle(hudWidth - margin - btnSize, margin, btnSize, btnSize);
        btnFwd = new Rectangle(hudWidth - margin - btnSize * 2 - 20, margin, btnSize, btnSize);

        // Results screen buttons
        btnRetry = new Rectangle(hudWidth / 2 - 120, 140, 110, 60);
        btnNext = new Rectangle(hudWidth / 2 + 10, 140, 110, 60);
    }

    public void update(Viewport hudViewport, boolean isResultsScreen) {
        forward = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        reverse = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        retryPressed = Gdx.input.isKeyJustPressed(Input.Keys.R);
        nextPressed = Gdx.input.isKeyJustPressed(Input.Keys.N);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugToggled = !debugToggled;
        }

        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                Vector2 touch = new Vector2(Gdx.input.getX(i), Gdx.input.getY(i));
                hudViewport.unproject(touch);

                if (!isResultsScreen) {
                    if (btnLeft.contains(touch)) left = true;
                    if (btnRight.contains(touch)) right = true;
                    if (btnFwd.contains(touch)) forward = true;
                    if (btnRev.contains(touch)) reverse = true;
                } else {
                    if (Gdx.input.justTouched()) {
                        if (btnRetry.contains(touch)) retryPressed = true;
                        if (btnNext.contains(touch)) nextPressed = true;
                    }
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
        } else {
            drawButton(shape, btnRetry, false);
            drawButton(shape, btnNext, false);
        }
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font, boolean isResultsScreen) {
        if (!isResultsScreen) {
            font.setColor(Color.WHITE);
            drawCenteredLabel(batch, font, "LEFT", btnLeft);
            drawCenteredLabel(batch, font, "RIGHT", btnRight);
            drawCenteredLabel(batch, font, "FWD", btnFwd);
            drawCenteredLabel(batch, font, "REV", btnRev);
        } else {
            font.setColor(Color.WHITE);
            drawCenteredLabel(batch, font, "RETRY", btnRetry);
            drawCenteredLabel(batch, font, "NEXT", btnNext);
        }
    }

    private void drawCenteredLabel(SpriteBatch batch, BitmapFont font, String text, Rectangle rect) {
        font.draw(batch, text, rect.x + rect.width / 2 - text.length() * 4, rect.y + rect.height / 2 + 5);
    }

    private void drawButton(ShapeRenderer shape, Rectangle rect, boolean active) {
        if (active) {
            shape.setColor(1, 1, 1, 0.45f);
        } else {
            shape.setColor(1, 1, 1, 0.2f);
        }
        shape.rect(rect.x, rect.y, rect.width, rect.height);
    }
}
