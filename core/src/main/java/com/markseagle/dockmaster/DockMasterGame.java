package com.markseagle.dockmaster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class DockMasterGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Viewport viewport;

    private Boat boat;
    private Dock dock;
    private InputController inputController;

    private int playerCash = 0;
    private boolean awardGiven = false;
    private String statusMessage = "";

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);

        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        inputController = new InputController(WORLD_WIDTH, WORLD_HEIGHT);

        boat = new Boat(WORLD_WIDTH / 2, 100);
        dock = new Dock(WORLD_WIDTH / 2 - 50, WORLD_HEIGHT - 150, 100, 100);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Input & Update
        inputController.update(viewport);

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetLevel();
        }

        if (boat.active && !dock.successfullyDocked) {
            boat.update(delta, inputController);
            dock.update(boat, delta);

            // Collision detection
            if (dock.checkCollision(boat)) {
                boat.handleCollision(boat.velocity.len());
            }
        }

        // Logic for awarding money
        if (dock.successfullyDocked && !awardGiven) {
            int payout = Math.max(0, 500 - (int)(boat.damage * 5));
            playerCash += payout;
            awardGiven = true;
            statusMessage = "DOCKED! Payout: $" + payout;
        }

        if (!boat.active) {
            statusMessage = "BOAT TOTALED! Press R to Restart";
        }

        // Draw
        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f); // Water

        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Draw Shapes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        dock.draw(shapeRenderer);
        boat.draw(shapeRenderer);
        inputController.drawShapes(shapeRenderer);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw Text / UI
        batch.begin();
        inputController.drawLabels(batch, font);

        // HUD Stats
        font.setColor(Color.WHITE);
        font.draw(batch, "Cash: $" + playerCash, 20, WORLD_HEIGHT - 20);

        Color dmgColor = boat.damage > 50 ? Color.RED : (boat.damage > 20 ? Color.YELLOW : Color.WHITE);
        font.setColor(dmgColor);
        font.draw(batch, "Damage: " + (int)boat.damage + "%", 20, WORLD_HEIGHT - 45);

        font.setColor(Color.WHITE);
        font.draw(batch, "Speed: " + (int)boat.velocity.len(), 20, WORLD_HEIGHT - 70);

        // Center: Status
        if (!statusMessage.isEmpty()) {
            font.draw(batch, statusMessage, WORLD_WIDTH / 2 - 100, WORLD_HEIGHT / 2 + 100);
        }

        // Docking progress
        if (dock.getDockingProgress() > 0 && !dock.successfullyDocked) {
            font.draw(batch, "DOCKING...", WORLD_WIDTH / 2 - 40, WORLD_HEIGHT - 170);
        }

        batch.end();
    }

    private void resetLevel() {
        boat.reset(WORLD_WIDTH / 2, 100);
        dock.reset();
        awardGiven = false;
        statusMessage = "";
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
