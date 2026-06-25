package com.markseagle.dockmaster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class DockMasterGame extends ApplicationAdapter {
    enum GameState { PLAYING, DOCKED, FAILED }
    private GameState state = GameState.PLAYING;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Viewport viewport;

    private Boat boat;
    private Dock dock;
    private LevelManager levelManager;
    private InputController inputController;

    private int playerCash = 0;
    private float levelTimer = 0;
    private int currentPayout = 0;
    private int damagePenalty = 0;
    private int timeBonus = 0;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);

        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        inputController = new InputController(WORLD_WIDTH, WORLD_HEIGHT);
        levelManager = new LevelManager();

        dock = new Dock();
        loadLevel(levelManager.getCurrentLevel());
    }

    private void loadLevel(LevelDefinition level) {
        if (boat == null) {
            boat = new Boat(level.startPos.x, level.startPos.y, level.startAngle);
        } else {
            boat.reset(level.startPos.x, level.startPos.y, level.startAngle);
        }
        dock.setLevel(level);
        levelTimer = 0;
        state = GameState.PLAYING;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Input
        inputController.update(viewport, state != GameState.PLAYING);

        if (inputController.retryPressed) {
            loadLevel(levelManager.getCurrentLevel());
        }
        if (state == GameState.DOCKED && inputController.nextPressed) {
            levelManager.nextLevel();
            loadLevel(levelManager.getCurrentLevel());
        }

        // Update
        if (state == GameState.PLAYING) {
            levelTimer += delta;
            boat.update(delta, inputController);
            dock.update(boat, delta);

            if (dock.checkCollision(boat)) {
                boat.handleCollision(boat.velocity.len());
            }

            if (!boat.active) {
                state = GameState.FAILED;
                calculateResults();
            } else if (dock.successfullyDocked) {
                state = GameState.DOCKED;
                calculateResults();
            }
        }

        // Draw
        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f);

        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. Shapes (World + Controls)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        dock.draw(shapeRenderer);
        boat.draw(shapeRenderer);
        inputController.drawShapes(shapeRenderer, state != GameState.PLAYING);

        // 2. Overlay for Results
        if (state != GameState.PLAYING) {
            float centerX = WORLD_WIDTH / 2 - 100;
            float centerY = WORLD_HEIGHT / 2 + 50;
            shapeRenderer.setColor(0, 0, 0, 0.7f);
            shapeRenderer.rect(centerX - 50, centerY - 200, 300, 300);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 3. Text (HUD + Results)
        batch.begin();
        inputController.drawLabels(batch, font, state != GameState.PLAYING);
        drawHUD();
        if (state != GameState.PLAYING) {
            drawResultsText();
        }
        batch.end();
    }

    private void calculateResults() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        damagePenalty = (int)(boat.damage * 5);
        timeBonus = (levelTimer < lvl.parTimeSeconds) ? (int)((lvl.parTimeSeconds - levelTimer) * 10) : 0;

        if (state == GameState.DOCKED) {
            currentPayout = Math.max(0, lvl.basePayout + timeBonus - damagePenalty);
            playerCash += currentPayout;
            boat.applyValueLoss();
        } else {
            currentPayout = 0;
        }
    }

    private void drawHUD() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        font.setColor(Color.WHITE);
        font.draw(batch, "Dest: " + lvl.destinationName, 20, WORLD_HEIGHT - 20);
        font.draw(batch, "Level: " + lvl.levelName, 20, WORLD_HEIGHT - 40);
        font.draw(batch, "Cash: $" + playerCash, 20, WORLD_HEIGHT - 60);
        font.draw(batch, "Time: " + (int)levelTimer + "s / Par: " + (int)lvl.parTimeSeconds + "s", 20, WORLD_HEIGHT - 80);

        Color dmgColor = boat.damage > 50 ? Color.RED : (boat.damage > 20 ? Color.YELLOW : Color.WHITE);
        font.setColor(dmgColor);
        font.draw(batch, "Damage: " + (int)boat.damage + "%", 20, WORLD_HEIGHT - 100);
        font.draw(batch, "Boat Val: $" + boat.boatValue, 20, WORLD_HEIGHT - 120);

        if (state == GameState.PLAYING && dock.getDockingProgress() > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "STABILIZING...", WORLD_WIDTH / 2 - 50, WORLD_HEIGHT - 170);
        }
    }

    private void drawResultsText() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        float centerX = WORLD_WIDTH / 2 - 100;
        float centerY = WORLD_HEIGHT / 2 + 50;

        font.setColor(Color.WHITE);
        if (state == GameState.DOCKED) {
            font.draw(batch, "SUCCESS!", centerX + 40, centerY + 80);
            font.draw(batch, "Base Payout: $" + lvl.basePayout, centerX, centerY + 40);
            font.draw(batch, "Time Bonus: $" + timeBonus, centerX, centerY + 20);
            font.draw(batch, "Damage Penalty: -$" + damagePenalty, centerX, centerY);
            font.draw(batch, "------------------", centerX, centerY - 15);
            font.draw(batch, "Final Payout: $" + currentPayout, centerX, centerY - 35);
        } else {
            font.draw(batch, "BOAT TOTALED!", centerX + 20, centerY + 80);
            font.draw(batch, "Damage: 100%", centerX + 40, centerY + 40);
            font.draw(batch, "Payout: $0", centerX + 50, centerY);
        }

        font.draw(batch, "Press R to Retry", centerX + 25, centerY - 80);
        if (state == GameState.DOCKED) {
            font.draw(batch, "Press N for Next", centerX + 25, centerY - 100);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
