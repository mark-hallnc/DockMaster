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

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class DockMasterGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Viewport viewport;

    private Boat boat;
    private Dock dock;
    private boolean docked = false;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);

        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);

        boat = new Boat(WORLD_WIDTH / 2, 100);
        dock = new Dock(WORLD_WIDTH / 2 - 50, WORLD_HEIGHT - 150, 100, 100);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Update
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            boat.reset(WORLD_WIDTH / 2, 100);
            docked = false;
        }

        if (!docked) {
            boat.update(delta);
            if (dock.isDocked(boat)) {
                docked = true;
            }
        }

        // Draw
        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f); // Water blue

        viewport.apply();

        // Enable transparency for the dock's slip zone
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        dock.draw(shapeRenderer);
        boat.draw(shapeRenderer);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        if (docked) {
            font.draw(batch, "DOCKED!", WORLD_WIDTH / 2 - 40, WORLD_HEIGHT / 2 + 50);
            font.draw(batch, "Press R to Reset", WORLD_WIDTH / 2 - 60, WORLD_HEIGHT / 2);
        } else {
            font.draw(batch, "Controls: WASD/Arrows to Move, Space to Brake, R to Reset", 20, 30);
        }
        batch.end();
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
