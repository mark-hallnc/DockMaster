package com.markseagle.dockmaster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class DockMasterGame extends ApplicationAdapter {
    public enum GameState { TITLE, LEVEL_SELECT, BOAT_SELECT, PLAYING, DOCKED, FAILED }
    private GameState state = GameState.TITLE;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont titleFont;

    private Viewport worldViewport;
    private Viewport hudViewport;
    private OrthographicCamera worldCamera;
    private OrthographicCamera hudCamera;

    private Boat boat;
    private Dock dock;
    private LevelManager levelManager;
    private InputController inputController;
    private ProgressManager progressManager;
    private BoatCatalog boatCatalog;

    private float levelTimer = 0;
    private int currentPayout = 0;
    private int damagePenalty = 0;
    private int timeBonus = 0;

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;
    private static final float HUD_WIDTH = 800;
    private static final float HUD_HEIGHT = 600;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.0f);

        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, worldCamera);

        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(HUD_WIDTH, HUD_HEIGHT, hudCamera);

        inputController = new InputController(HUD_WIDTH, HUD_HEIGHT);
        levelManager = new LevelManager();
        progressManager = new ProgressManager();
        boatCatalog = new BoatCatalog();

        dock = new Dock();
    }

    private void loadLevel(int index) {
        levelManager.setCurrentLevel(index);
        LevelDefinition level = levelManager.getCurrentLevel();

        BoatDefinition selectedBoat = boatCatalog.getBoatById(progressManager.getSelectedBoatId());

        if (boat == null) {
            boat = new Boat(level.startPos.x, level.startPos.y, level.startAngle, selectedBoat);
        } else {
            boat.profile = selectedBoat;
            boat.reset(level.startPos.x, level.startPos.y, level.startAngle);
            // Re-init bounds as boat size might have changed
            float l = boat.profile.length;
            float w = boat.profile.width;
            float[] vertices = new float[] {
                -l/2, -w/2,
                l/2, -w/2,
                l/2 + 10, 0,
                l/2, w/2,
                -l/2, w/2
            };
            boat.bounds.setVertices(vertices);
        }
        boat.boatValue = selectedBoat.value; // Reset value for level foundation
        dock.setLevel(level);
        levelTimer = 0;
        state = GameState.PLAYING;

        worldCamera.position.set(boat.x, boat.y, 0);
        worldCamera.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        inputController.update(hudViewport, state);

        handleTransitions();

        if (state == GameState.PLAYING) {
            levelTimer += delta;
            boat.update(delta, inputController, levelManager.getCurrentLevel());
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
                progressManager.unlockNextLevel(levelManager.getCurrentLevelIndex());
                progressManager.addCash(currentPayout);
                // In this pass, we just reset value foundation, but could persist if needed
            }
            updateCamera(delta);
        }

        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f);

        if (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED) {
            renderWorld();
        }

        renderHud();
    }

    private void handleTransitions() {
        if (inputController.startPressed) {
            if (state == GameState.TITLE) loadLevel(0);
        }
        if (inputController.levelSelectPressed) {
            state = GameState.LEVEL_SELECT;
        }
        if (inputController.boatSelectPressed) {
            state = GameState.BOAT_SELECT;
        }
        if (inputController.titlePressed) {
            state = GameState.TITLE;
        }
        if (inputController.retryPressed && (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED)) {
            loadLevel(levelManager.getCurrentLevelIndex());
        }
        if (inputController.nextPressed && state == GameState.DOCKED) {
            if (levelManager.hasNextLevel()) {
                levelManager.nextLevel();
                loadLevel(levelManager.getCurrentLevelIndex());
            } else {
                state = GameState.LEVEL_SELECT;
            }
        }
        if (state == GameState.LEVEL_SELECT && inputController.selectedLevelIndex != -1) {
            if (inputController.selectedLevelIndex <= progressManager.getUnlockedLevel() && inputController.selectedLevelIndex < levelManager.getLevels().size()) {
                loadLevel(inputController.selectedLevelIndex);
            }
        }
        if (state == GameState.BOAT_SELECT && inputController.selectedBoatIndex != -1) {
            if (inputController.selectedBoatIndex < boatCatalog.getBoats().size()) {
                BoatDefinition b = boatCatalog.getBoats().get(inputController.selectedBoatIndex);
                if (!b.displayName.contains("Soon")) {
                    progressManager.setSelectedBoatId(b.id);
                }
            }
        }
    }

    private void renderWorld() {
        worldViewport.apply();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        dock.draw(shapeRenderer);
        drawEnvironmentalIndicators(shapeRenderer, levelManager.getCurrentLevel());
        boat.draw(shapeRenderer);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderHud() {
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        inputController.drawShapes(shapeRenderer, state, progressManager.getSelectedBoatId(), boatCatalog);

        if (state == GameState.DOCKED || state == GameState.FAILED) {
            drawResultsBackdrop();
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        inputController.drawLabels(batch, font, state, levelManager, progressManager, boatCatalog);

        if (state == GameState.TITLE) {
            drawTitleScreen();
        } else if (state == GameState.LEVEL_SELECT) {
            drawLevelSelectScreen();
        } else if (state == GameState.BOAT_SELECT) {
            drawBoatSelectScreen();
        } else if (state == GameState.PLAYING) {
            drawHUDText();
        } else if (state == GameState.DOCKED || state == GameState.FAILED) {
            drawHUDText();
            drawResultsText();
        }

        if (inputController.debugToggled) {
            drawDebugOverlay();
        }
        batch.end();
    }

    private void drawTitleScreen() {
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "DockMaster", HUD_WIDTH / 2 - 160, HUD_HEIGHT - 80);
        font.draw(batch, "Boat Docking Challenge", HUD_WIDTH / 2 - 120, HUD_HEIGHT - 140);

        font.draw(batch, "Total Cash: $" + progressManager.getPlayerCash(), 20, 100);
        font.draw(batch, "Selected Boat: " + boatCatalog.getBoatById(progressManager.getSelectedBoatId()).displayName, 20, 70);
    }

    private void drawLevelSelectScreen() {
        font.setColor(Color.WHITE);
        font.draw(batch, "SELECT LEVEL", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
    }

    private void drawBoatSelectScreen() {
        font.setColor(Color.WHITE);
        font.draw(batch, "SELECT BOAT", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
    }

    private void drawEnvironmentalIndicators(ShapeRenderer shape, LevelDefinition level) {
        shape.setColor(0.3f, 0.7f, 1f, 0.3f);
        for (CurrentZone zone : level.currentZones) {
            shape.rect(zone.bounds.x, zone.bounds.y, zone.bounds.width, zone.bounds.height);
            float spacing = 60f;
            for (float ix = zone.bounds.x + 20; ix < zone.bounds.x + zone.bounds.width; ix += spacing) {
                for (float iy = zone.bounds.y + 20; iy < zone.bounds.y + zone.bounds.height; iy += spacing) {
                    drawArrow(shape, ix, iy, zone.force, 20);
                }
            }
        }
    }

    private void drawArrow(ShapeRenderer shape, float x, float y, Vector2 force, float size) {
        if (force.len() < 1) return;
        shape.setColor(1, 1, 1, 0.4f);
        float angle = force.angleDeg();
        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();
        shape.rect(0, -2, size, 4);
        shape.triangle(size, -6, size, 6, size + 8, 0);
        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    private void updateCamera(float delta) {
        float lerp = 4f;
        worldCamera.position.x += (boat.x - worldCamera.position.x) * lerp * delta;
        worldCamera.position.y += (boat.y - worldCamera.position.y) * lerp * delta;
        worldCamera.update();
    }

    private void calculateResults() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        damagePenalty = (int)(boat.damage * 5);
        timeBonus = (levelTimer < lvl.parTimeSeconds) ? (int)((lvl.parTimeSeconds - levelTimer) * 10) : 0;

        if (state == GameState.DOCKED) {
            currentPayout = Math.max(0, lvl.basePayout + timeBonus - damagePenalty);
            boat.applyValueLoss();
        } else {
            currentPayout = 0;
        }
    }

    private void drawHUDText() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        font.setColor(Color.WHITE);
        float top = HUD_HEIGHT - 20;

        font.draw(batch, "Boat: " + boat.profile.displayName, 20, top);
        font.draw(batch, "Dest: " + lvl.destinationName, 20, top - 20);
        font.draw(batch, "Level: " + lvl.levelName, 20, top - 40);
        font.draw(batch, "Cash: $" + progressManager.getPlayerCash(), 20, top - 60);
        font.draw(batch, "Time: " + (int)levelTimer + "s / Par: " + (int)lvl.parTimeSeconds + "s", 20, top - 80);

        Color dmgColor = boat.damage > 50 ? Color.RED : (boat.damage > 20 ? Color.YELLOW : Color.WHITE);
        font.setColor(dmgColor);
        font.draw(batch, "Damage: " + (int)boat.damage + "%", 20, top - 100);

        font.setColor(Color.WHITE);
        font.draw(batch, "Throttle: " + boat.getThrottleState(inputController), 20, top - 120);
        font.draw(batch, "Boat Val: $" + boat.boatValue, 20, top - 140);

        String windInfo = getForceDescription(lvl.windForce);
        font.draw(batch, "Wind: " + windInfo, 20, top - 160);

        if (state == GameState.PLAYING && dock.getDockingProgress() > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "STABILIZING...", HUD_WIDTH / 2 - 50, HUD_HEIGHT - 50);
        }
    }

    private String getForceDescription(Vector2 force) {
        if (force.len() == 0) return "Calm";
        String dir = "";
        if (Math.abs(force.x) > Math.abs(force.y)) {
            dir = force.x > 0 ? "East" : "West";
        } else {
            dir = force.y > 0 ? "North" : "South";
        }
        String strength = "Light";
        if (force.len() >= LevelManager.MEDIUM_FORCE) strength = "Medium";
        if (force.len() >= LevelManager.STRONG_FORCE) strength = "Strong";
        return dir + " " + strength;
    }

    private void drawResultsBackdrop() {
        float centerX = HUD_WIDTH / 2;
        float centerY = HUD_HEIGHT / 2 + 100;
        shapeRenderer.setColor(0, 0, 0, 0.75f);
        shapeRenderer.rect(centerX - 150, centerY - 250, 300, 350);
    }

    private void drawResultsText() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        float centerX = HUD_WIDTH / 2 - 100;
        float centerY = HUD_HEIGHT / 2 + 100;

        font.setColor(Color.WHITE);
        if (state == GameState.DOCKED) {
            font.draw(batch, "SUCCESS!", centerX + 40, centerY + 80);
            font.draw(batch, "Boat: " + boat.profile.displayName, centerX, centerY + 60);
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
    }

    private void drawDebugOverlay() {
        font.setColor(Color.LIME);
        float x = HUD_WIDTH - 200;
        float y = HUD_HEIGHT - 20;
        font.draw(batch, "DEBUG (F1 to toggle)", x, y);
        font.draw(batch, "Pos: " + (int)boat.x + ", " + (int)boat.y, x, y - 20);
        font.draw(batch, "Angle: " + (int)boat.angle, x, y - 40);
        font.draw(batch, "Vel: " + (int)boat.velocity.len(), x, y - 60);
        font.draw(batch, "Boat: " + boat.profile.id, x, y - 80);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        hudViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        titleFont.dispose();
    }
}
