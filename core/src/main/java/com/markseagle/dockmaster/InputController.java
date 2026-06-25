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
import java.util.ArrayList;
import java.util.List;

public class InputController {
    public boolean forward, reverse, left, right;
    public boolean nextPressed, retryPressed, levelSelectPressed, titlePressed, startPressed, boatSelectPressed;
    public boolean debugToggled = false;

    private final Rectangle btnLeft, btnRight, btnFwd, btnRev;
    private final Rectangle btnNext, btnRetry;
    private final Rectangle btnStart, btnBack, btnLevelSelect, btnBoatSelect;
    private final Rectangle btnLevelSelectResults, btnTitleResults;

    // Grid buttons
    private final List<Rectangle> levelButtons = new ArrayList<>();
    private final List<Rectangle> boatButtons = new ArrayList<>();
    public int selectedLevelIndex = -1;
    public int selectedBoatIndex = -1;

    private final float btnSize = 120f;
    private final float margin = 30f;

    public InputController(float hudWidth, float hudHeight) {
        // Gameplay
        btnLeft = new Rectangle(margin, margin, btnSize, btnSize);
        btnRight = new Rectangle(margin + btnSize + 20, margin, btnSize, btnSize);
        btnRev = new Rectangle(hudWidth - margin - btnSize, margin, btnSize, btnSize);
        btnFwd = new Rectangle(hudWidth - margin - btnSize * 2 - 20, margin, btnSize, btnSize);

        // Title screen
        btnStart = new Rectangle(hudWidth / 2 - 100, 280, 200, 60);
        btnLevelSelect = new Rectangle(hudWidth / 2 - 100, 200, 200, 60);
        btnBoatSelect = new Rectangle(hudWidth / 2 - 100, 120, 200, 60);

        // Results screen
        btnRetry = new Rectangle(hudWidth / 2 - 110, 220, 100, 50);
        btnNext = new Rectangle(hudWidth / 2 + 10, 220, 100, 50);
        btnLevelSelectResults = new Rectangle(hudWidth / 2 - 110, 160, 220, 50);
        btnTitleResults = new Rectangle(hudWidth / 2 - 110, 100, 220, 50);

        // Back button
        btnBack = new Rectangle(20, hudHeight - 80, 100, 50);

        // Level select grid
        for (int i = 0; i < 6; i++) {
            float x = 100 + (i % 2) * 350;
            float y = hudHeight - 200 - (i / 2) * 120;
            levelButtons.add(new Rectangle(x, y, 300, 100));
        }

        // Boat select grid
        for (int i = 0; i < 4; i++) {
            float x = 100 + (i % 2) * 350;
            float y = hudHeight - 200 - (i / 2) * 120;
            boatButtons.add(new Rectangle(x, y, 300, 100));
        }
    }

    public void update(Viewport hudViewport, DockMasterGame.GameState state) {
        nextPressed = false;
        retryPressed = false;
        levelSelectPressed = false;
        boatSelectPressed = false;
        titlePressed = false;
        startPressed = false;
        selectedLevelIndex = -1;
        selectedBoatIndex = -1;

        // Keyboard
        forward = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        reverse = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) retryPressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) nextPressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (state == DockMasterGame.GameState.TITLE) startPressed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) levelSelectPressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (state == DockMasterGame.GameState.TITLE) boatSelectPressed = true;
            else titlePressed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) titlePressed = true;

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugToggled = !debugToggled;
        }

        // Touch
        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                Vector2 touch = new Vector2(Gdx.input.getX(i), Gdx.input.getY(i));
                hudViewport.unproject(touch);

                if (state == DockMasterGame.GameState.PLAYING) {
                    if (btnLeft.contains(touch)) left = true;
                    if (btnRight.contains(touch)) right = true;
                    if (btnFwd.contains(touch)) forward = true;
                    if (btnRev.contains(touch)) reverse = true;
                } else if (Gdx.input.justTouched()) {
                    if (state == DockMasterGame.GameState.TITLE) {
                        if (btnStart.contains(touch)) startPressed = true;
                        if (btnLevelSelect.contains(touch)) levelSelectPressed = true;
                        if (btnBoatSelect.contains(touch)) boatSelectPressed = true;
                    } else if (state == DockMasterGame.GameState.LEVEL_SELECT) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        for (int j = 0; j < levelButtons.size(); j++) {
                            if (levelButtons.get(j).contains(touch)) selectedLevelIndex = j;
                        }
                    } else if (state == DockMasterGame.GameState.BOAT_SELECT) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        for (int j = 0; j < boatButtons.size(); j++) {
                            if (boatButtons.get(j).contains(touch)) selectedBoatIndex = j;
                        }
                    } else if (state == DockMasterGame.GameState.DOCKED || state == DockMasterGame.GameState.FAILED) {
                        if (btnRetry.contains(touch)) retryPressed = true;
                        if (btnNext.contains(touch)) nextPressed = true;
                        if (btnLevelSelectResults.contains(touch)) levelSelectPressed = true;
                        if (btnTitleResults.contains(touch)) titlePressed = true;
                    }
                }
            }
        }
    }

    public void drawShapes(ShapeRenderer shape, DockMasterGame.GameState state, String currentBoatId, BoatCatalog bc) {
        if (state == DockMasterGame.GameState.PLAYING) {
            drawButton(shape, btnLeft, left);
            drawButton(shape, btnRight, right);
            drawButton(shape, btnFwd, forward);
            drawButton(shape, btnRev, reverse);
        } else if (state == DockMasterGame.GameState.TITLE) {
            drawButton(shape, btnStart, false);
            drawButton(shape, btnLevelSelect, false);
            drawButton(shape, btnBoatSelect, false);
        } else if (state == DockMasterGame.GameState.LEVEL_SELECT || state == DockMasterGame.GameState.BOAT_SELECT) {
            drawButton(shape, btnBack, false);
            List<Rectangle> buttons = (state == DockMasterGame.GameState.LEVEL_SELECT) ? levelButtons : boatButtons;
            for (int i = 0; i < buttons.size(); i++) {
                boolean active = false;
                if (state == DockMasterGame.GameState.BOAT_SELECT && i < bc.getBoats().size()) {
                    active = bc.getBoats().get(i).id.equals(currentBoatId);
                }
                drawButton(shape, buttons.get(i), active);
            }
        } else if (state == DockMasterGame.GameState.DOCKED || state == DockMasterGame.GameState.FAILED) {
            drawButton(shape, btnRetry, false);
            drawButton(shape, btnNext, false);
            drawButton(shape, btnLevelSelectResults, false);
            drawButton(shape, btnTitleResults, false);
        }
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font, DockMasterGame.GameState state, LevelManager lm, ProgressManager pm, BoatCatalog bc) {
        font.setColor(Color.WHITE);
        if (state == DockMasterGame.GameState.PLAYING) {
            drawCenteredLabel(batch, font, "LEFT", btnLeft);
            drawCenteredLabel(batch, font, "RIGHT", btnRight);
            drawCenteredLabel(batch, font, "FWD", btnFwd);
            drawCenteredLabel(batch, font, "REV", btnRev);
        } else if (state == DockMasterGame.GameState.TITLE) {
            drawCenteredLabel(batch, font, "START", btnStart);
            drawCenteredLabel(batch, font, "LEVEL SELECT", btnLevelSelect);
            drawCenteredLabel(batch, font, "BOAT SELECT", btnBoatSelect);
        } else if (state == DockMasterGame.GameState.LEVEL_SELECT) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            List<LevelDefinition> levels = lm.getLevels();
            for (int i = 0; i < levels.size(); i++) {
                Rectangle r = levelButtons.get(i);
                LevelDefinition lvl = levels.get(i);
                boolean unlocked = i <= pm.getUnlockedLevel();
                font.setColor(unlocked ? Color.WHITE : Color.GRAY);
                font.draw(batch, (i+1) + ". " + lvl.levelName, r.x + 10, r.y + 80);
                font.draw(batch, lvl.destinationName, r.x + 10, r.y + 50);
                if (!unlocked) font.draw(batch, "LOCKED", r.x + 10, r.y + 20);
            }
        } else if (state == DockMasterGame.GameState.BOAT_SELECT) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            List<BoatDefinition> boats = bc.getBoats();
            for (int i = 0; i < boats.size(); i++) {
                Rectangle r = boatButtons.get(i);
                BoatDefinition b = boats.get(i);
                boolean selected = b.id.equals(pm.getSelectedBoatId());
                font.setColor(selected ? Color.YELLOW : Color.WHITE);
                font.draw(batch, b.displayName, r.x + 10, r.y + 85);
                font.setColor(Color.WHITE);
                font.getData().setScale(0.8f);
                font.draw(batch, b.description, r.x + 10, r.y + 60);
                font.draw(batch, "Value: $" + b.value, r.x + 10, r.y + 40);
                if (b.displayName.contains("Soon")) font.draw(batch, "LOCKED", r.x + 10, r.y + 20);
                else if (selected) font.draw(batch, "SELECTED", r.x + 10, r.y + 20);
                font.getData().setScale(1.2f);
            }
        } else if (state == DockMasterGame.GameState.DOCKED || state == DockMasterGame.GameState.FAILED) {
            drawCenteredLabel(batch, font, "RETRY", btnRetry);
            drawCenteredLabel(batch, font, "NEXT", btnNext);
            drawCenteredLabel(batch, font, "LEVEL SELECT", btnLevelSelectResults);
            drawCenteredLabel(batch, font, "TITLE SCREEN", btnTitleResults);
        }
    }

    private void drawCenteredLabel(SpriteBatch batch, BitmapFont font, String text, Rectangle rect) {
        font.draw(batch, text, rect.x + rect.width / 2 - text.length() * 4, rect.y + rect.height / 2 + 5);
    }

    private void drawButton(ShapeRenderer shape, Rectangle rect, boolean active) {
        if (active) {
            shape.setColor(1, 1, 0, 0.45f); // Yellowish for active/selected
        } else {
            shape.setColor(1, 1, 1, 0.2f);
        }
        shape.rect(rect.x, rect.y, rect.width, rect.height);
    }
}
