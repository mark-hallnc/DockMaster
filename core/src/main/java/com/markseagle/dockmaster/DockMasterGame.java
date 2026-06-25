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
    public enum GameState { TITLE, LEVEL_SELECT, BOAT_SELECT, GARAGE, SETTINGS, PLAYING, DOCKED, FAILED }
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
    private SoundManager soundManager;

    private float levelTimer = 0;
    private int currentPayout = 0;
    private int damagePenalty = 0;
    private int timeBonus = 0;
    private float levelStartDamage = 0;

    private int currentStars = 0;
    private int bestStarsBefore = 0;
    private int starBonus = 0;

    private String statusMessage = "";
    private float statusTimer = 0;

    // Polish effects
    private WakeTrail wakeTrail;
    private FloatingText floatingText;
    private float introTimer = 0;
    private float shakeTimer = 0;
    private float collisionFeedbackTimer = 0;

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
        soundManager = new SoundManager(progressManager);

        dock = new Dock();
        wakeTrail = new WakeTrail();
        floatingText = new FloatingText();
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTimer = 3.0f;
    }

    private void loadLevel(int index) {
        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        float currentDamage = progressManager.getBoatDamage(profile.id);

        if (currentDamage >= 100) {
            showStatus("Repair this boat before launching.");
            state = GameState.GARAGE;
            soundManager.play("fail");
            return;
        }

        levelManager.setCurrentLevel(index);
        LevelDefinition level = levelManager.getCurrentLevel();
        levelStartDamage = currentDamage;

        if (boat == null) {
            boat = new Boat(level.startPos.x, level.startPos.y, level.startAngle, profile);
        } else {
            boat.profile = profile;
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
        boat.reset(level.startPos.x, level.startPos.y, level.startAngle, levelStartDamage);
        boat.boatValue = progressManager.getBoatValue(profile.id, profile.value);

        dock.setLevel(level);
        levelTimer = 0;
        currentStars = 0;
        starBonus = 0;
        introTimer = 3.0f;
        shakeTimer = 0;
        collisionFeedbackTimer = 0;
        wakeTrail.clear();
        floatingText.clear();
        state = GameState.PLAYING;

        worldCamera.position.set(boat.x, boat.y, 0);
        worldCamera.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        boolean boatTotaled = false;
        if (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED) {
            if (boat != null && boat.damage >= 100) boatTotaled = true;
        } else {
            BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
            if (progressManager.getBoatDamage(profile.id) >= 100) boatTotaled = true;
        }

        inputController.update(hudViewport, state, boatTotaled);
        handleTransitions();

        if (statusTimer > 0) statusTimer -= delta;

        if (state == GameState.PLAYING) {
            levelTimer += delta;
            if (introTimer > 0) introTimer -= delta;
            if (shakeTimer > 0) shakeTimer -= delta;
            if (collisionFeedbackTimer > 0) collisionFeedbackTimer -= delta;

            boat.update(delta, inputController, levelManager.getCurrentLevel());
            dock.update(boat, delta);
            wakeTrail.update(delta, boat.x, boat.y, boat.velocity.len());
            floatingText.update(delta);

            if (dock.checkCollision(boat)) {
                if (collisionFeedbackTimer <= 0) {
                    float impact = boat.velocity.len();
                    float dmg = boat.handleCollision(impact);
                    if (dmg > 0) {
                        floatingText.spawn("-" + (int)dmg + "%", boat.x, boat.y + 20, Color.RED);
                        shakeTimer = 0.2f;
                        collisionFeedbackTimer = 0.4f;

                        if (impact > 100f) {
                            soundManager.play("crash");
                            vibrate(150);
                        } else {
                            soundManager.play("bump");
                            vibrate(40);
                        }
                    }
                } else {
                    boat.handleCollision(boat.velocity.len()); // Still apply physics
                }
            }

            if (!boat.active) {
                state = GameState.FAILED;
                calculateResults();
                saveBoatState();
                soundManager.play("fail");
                vibrate(400);
            } else if (dock.successfullyDocked) {
                state = GameState.DOCKED;
                bestStarsBefore = progressManager.getBestStars(levelManager.getCurrentLevelIndex());
                calculateResults();
                if (currentStars >= 1) progressManager.unlockNextLevel(levelManager.getCurrentLevelIndex());
                if (currentStars > bestStarsBefore) {
                    starBonus = (currentStars - bestStarsBefore) * 200;
                    progressManager.setBestStars(levelManager.getCurrentLevelIndex(), currentStars);
                }
                progressManager.addCash(currentPayout + starBonus);
                saveBoatState();
                soundManager.play("success");
                vibrate(200);
            }
            updateCamera(delta);
        }

        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f);

        if (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED) {
            renderWorld();
        }
        renderHud(boatTotaled);
    }

    private void vibrate(int milliseconds) {
        if (progressManager.isVibrationEnabled()) {
            Gdx.input.vibrate(milliseconds);
        }
    }

    private void saveBoatState() {
        progressManager.setBoatDamage(boat.profile.id, boat.damage);
        boat.applyValueLoss();
        progressManager.setBoatValue(boat.profile.id, boat.boatValue);
    }

    private void handleTransitions() {
        if (inputController.startPressed) {
            soundManager.play("click");
            if (state == GameState.TITLE) loadLevel(levelManager.getCurrentLevelIndex());
        }
        if (inputController.levelSelectPressed) {
            soundManager.play("click");
            state = GameState.LEVEL_SELECT;
        }
        if (inputController.boatSelectPressed) {
            soundManager.play("click");
            state = GameState.BOAT_SELECT;
        }
        if (inputController.garagePressed) {
            soundManager.play("click");
            state = GameState.GARAGE;
        }
        if (inputController.settingsPressed) {
            soundManager.play("click");
            state = GameState.SETTINGS;
        }
        if (inputController.titlePressed) {
            soundManager.play("click");
            state = GameState.TITLE;
        }
        if (inputController.soundToggled) {
            progressManager.setSoundEnabled(!progressManager.isSoundEnabled());
            soundManager.play("click");
        }
        if (inputController.vibrateToggled) {
            progressManager.setVibrationEnabled(!progressManager.isVibrationEnabled());
            soundManager.play("click");
            vibrate(100);
        }

        if (inputController.retryPressed && (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED)) {
            soundManager.play("click");
            loadLevel(levelManager.getCurrentLevelIndex());
        }
        if (inputController.nextPressed && state == GameState.DOCKED) {
            soundManager.play("click");
            if (levelManager.hasNextLevel()) {
                levelManager.nextLevel();
                loadLevel(levelManager.getCurrentLevelIndex());
            } else {
                state = GameState.LEVEL_SELECT;
            }
        }
        if (state == GameState.LEVEL_SELECT && inputController.selectedLevelIndex != -1) {
            if (inputController.selectedLevelIndex <= progressManager.getUnlockedLevel() && inputController.selectedLevelIndex < levelManager.getLevels().size()) {
                soundManager.play("click");
                loadLevel(inputController.selectedLevelIndex);
            } else {
                soundManager.play("fail"); // Locked level
            }
        }
        if (state == GameState.BOAT_SELECT && inputController.selectedBoatIndex != -1) {
            if (inputController.selectedBoatIndex < boatCatalog.getBoats().size()) {
                BoatDefinition b = boatCatalog.getBoats().get(inputController.selectedBoatIndex);
                if (!b.displayName.contains("Soon")) {
                    soundManager.play("click");
                    progressManager.setSelectedBoatId(b.id);
                } else {
                    soundManager.play("fail");
                }
            }
        }
        if (state == GameState.GARAGE && inputController.repairPressed) performRepair();
    }

    private void performRepair() {
        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        float damage = progressManager.getBoatDamage(profile.id);
        int cost = (int)(damage * 20);
        if (damage <= 0) return;
        if (progressManager.getPlayerCash() >= cost) {
            progressManager.spendCash(cost);
            progressManager.setBoatDamage(profile.id, 0);
            progressManager.setBoatValue(profile.id, profile.value);
            showStatus("Boat Repaired!");
            soundManager.play("repair");
        } else {
            int available = progressManager.getPlayerCash();
            if (available > 0) {
                float repairedDamage = available / 20f;
                float newDamage = Math.max(0, damage - repairedDamage);
                progressManager.spendCash(available);
                progressManager.setBoatDamage(profile.id, newDamage);
                long newValue = profile.value - (long)(newDamage * (profile.value / 100f));
                progressManager.setBoatValue(profile.id, newValue);
                showStatus("Partial repair completed.");
                soundManager.play("repair");
            } else {
                showStatus("Not enough cash!");
                soundManager.play("fail");
            }
        }
    }

    private void renderWorld() {
        worldViewport.apply();

        float sx = 0, sy = 0;
        if (shakeTimer > 0) {
            sx = (float)(Math.random() - 0.5f) * 15f;
            sy = (float)(Math.random() - 0.5f) * 15f;
        }
        worldCamera.position.add(sx, sy, 0);
        worldCamera.update();

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawWaterDetails(shapeRenderer);
        dock.draw(shapeRenderer);
        drawEnvironmentalIndicators(shapeRenderer, levelManager.getCurrentLevel());
        wakeTrail.draw(shapeRenderer);
        boat.draw(shapeRenderer);

        if (inputController.debugToggled && dock.slipZone != null) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.rect(dock.slipZone.x, dock.slipZone.y, dock.slipZone.width, dock.slipZone.height);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        }

        shapeRenderer.end();

        batch.begin();
        floatingText.draw(batch, font);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        worldCamera.position.sub(sx, sy, 0);
        worldCamera.update();
    }

    private void drawWaterDetails(ShapeRenderer shape) {
        shape.setColor(1, 1, 1, 0.05f);
        shape.circle(100, 100, 8);
        shape.circle(700, 500, 8);
        shape.circle(400, 300, 8);
        shape.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        shape.rect(50, 50, 10, 10);
        shape.rect(740, 50, 10, 10);
    }

    private void renderHud(boolean boatTotaled) {
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        inputController.drawShapes(shapeRenderer, state, progressManager.getSelectedBoatId(), boatCatalog, boatTotaled);
        if (state == GameState.DOCKED || state == GameState.FAILED) drawResultsBackdrop();
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        inputController.drawLabels(batch, font, state, levelManager, progressManager, boatCatalog, boatTotaled);

        if (state == GameState.TITLE) drawTitleScreen();
        else if (state == GameState.LEVEL_SELECT) drawLevelSelectScreen();
        else if (state == GameState.BOAT_SELECT) drawBoatSelectScreen();
        else if (state == GameState.GARAGE) drawGarageScreen();
        else if (state == GameState.SETTINGS) drawSettingsScreen();
        else if (state == GameState.PLAYING) {
            drawHUDText();
            if (introTimer > 0) drawLevelIntro();
            drawDockingGuidance();
        } else if (state == GameState.DOCKED || state == GameState.FAILED) {
            drawHUDText();
            drawResultsText();
        }

        if (statusTimer > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, statusMessage, HUD_WIDTH / 2 - 100, 50);
            font.setColor(Color.WHITE);
        }

        if (inputController.debugToggled) drawDebugOverlay();
        batch.end();
    }

    private void drawSettingsScreen() {
        font.setColor(Color.WHITE);
        font.draw(batch, "SETTINGS", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 40);
        font.draw(batch, "Adjust your experience preferences.", HUD_WIDTH / 2 - 140, HUD_HEIGHT - 80);
    }

    private void drawLevelIntro() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        float alpha = Math.min(1.0f, introTimer);
        font.setColor(1, 1, 1, alpha);
        font.draw(batch, "DESTINATION: " + lvl.destinationName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 60);
        font.draw(batch, "LEVEL: " + lvl.levelName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 35);
        font.draw(batch, "PAR TIME: " + (int)lvl.parTimeSeconds + "s", HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 10);
        font.setColor(Color.WHITE);
    }

    private void drawDockingGuidance() {
        float speed = boat.velocity.len();
        if (dock.isInsideSlipZone(boat)) {
            font.setColor(Color.LIME);
            font.draw(batch, "STABILIZING: " + (int)(dock.getDockingProgress() * 100) + "%", HUD_WIDTH / 2 - 70, HUD_HEIGHT - 60);
        } else if (dock.slipZone.contains(boat.x, boat.y)) {
            if (speed >= 35f) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "TOO FAST! SLOW DOWN", HUD_WIDTH / 2 - 90, HUD_HEIGHT - 60);
            } else {
                font.setColor(Color.YELLOW);
                font.draw(batch, "LINE UP BOAT ANGLE", HUD_WIDTH / 2 - 90, HUD_HEIGHT - 60);
            }
        }
        font.setColor(Color.WHITE);
    }

    private void drawTitleScreen() {
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "DockMaster", HUD_WIDTH / 2 - 160, HUD_HEIGHT - 60);
        font.draw(batch, "Boat Docking Challenge", HUD_WIDTH / 2 - 120, HUD_HEIGHT - 110);
        font.draw(batch, "Total Cash: $" + progressManager.getPlayerCash(), 20, 100);
        font.draw(batch, "Total Stars: " + progressManager.getTotalStars(levelManager.getLevels().size()), 20, 130);
        font.draw(batch, "Selected Boat: " + boatCatalog.getBoatById(progressManager.getSelectedBoatId()).displayName, 20, 70);
    }

    private void drawLevelSelectScreen() {
        font.setColor(Color.WHITE);
        font.draw(batch, "SELECT LEVEL", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
        font.draw(batch, "Total Stars: " + progressManager.getTotalStars(levelManager.getLevels().size()), 20, 40);
    }

    private void drawBoatSelectScreen() {
        font.setColor(Color.WHITE);
        font.draw(batch, "SELECT BOAT", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
    }

    private void drawGarageScreen() {
        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        float damage = progressManager.getBoatDamage(profile.id);
        long value = progressManager.getBoatValue(profile.id, profile.value);
        int cost = (int)(damage * 20);
        font.setColor(Color.WHITE);
        font.draw(batch, "GARAGE", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 40);
        font.draw(batch, "Boat: " + profile.displayName, 100, HUD_HEIGHT - 120);
        font.draw(batch, profile.description, 100, HUD_HEIGHT - 150);
        font.setColor(damage > 50 ? Color.RED : (damage > 0 ? Color.YELLOW : Color.GREEN));
        font.draw(batch, "Current Damage: " + (int)damage + "%", 100, HUD_HEIGHT - 200);
        font.draw(batch, "Current Value: $" + value + " / $" + profile.value, 100, HUD_HEIGHT - 230);
        font.setColor(Color.WHITE);
        font.draw(batch, "Your Cash: $" + progressManager.getPlayerCash(), 100, HUD_HEIGHT - 280);
        if (damage > 0) {
            font.draw(batch, "Repair Cost: $" + cost, 100, HUD_HEIGHT - 310);
            if (progressManager.getPlayerCash() < cost) {
                font.setColor(Color.RED);
                font.draw(batch, "(Insufficient cash for full repair)", 300, HUD_HEIGHT - 310);
            }
        } else {
            font.setColor(Color.GREEN);
            font.draw(batch, "No repairs needed", 100, HUD_HEIGHT - 310);
        }
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
        float damageIncurred = boat.damage - levelStartDamage;
        damagePenalty = (int)(damageIncurred * 5);
        timeBonus = (levelTimer < lvl.parTimeSeconds) ? (int)((lvl.parTimeSeconds - levelTimer) * 10) : 0;
        if (state == GameState.DOCKED) {
            currentPayout = Math.max(0, lvl.basePayout + timeBonus - damagePenalty);
            if (boat.damage <= 10 && levelTimer <= lvl.parTimeSeconds) currentStars = 3;
            else if (boat.damage <= 40) currentStars = 2;
            else currentStars = 1;
        } else {
            currentPayout = 0;
            currentStars = 0;
        }
    }

    private void drawHUDText() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        font.setColor(Color.WHITE);
        float top = HUD_HEIGHT - 20;
        font.draw(batch, "Boat: " + boat.profile.displayName, 20, top);
        font.draw(batch, "Dest: " + lvl.destinationName, 20, top - 20);
        font.draw(batch, "Cash: $" + progressManager.getPlayerCash(), 20, top - 40);
        font.draw(batch, "Speed: " + (int)boat.velocity.len(), 20, top - 60);
        Color dmgColor = boat.damage > 50 ? Color.RED : (boat.damage > 20 ? Color.YELLOW : Color.WHITE);
        font.setColor(dmgColor);
        font.draw(batch, "Damage: " + (int)boat.damage + "%", 20, top - 80);
        font.setColor(Color.WHITE);
        font.draw(batch, "Boat Val: $" + boat.boatValue, 20, top - 100);
    }

    private void drawResultsBackdrop() {
        float centerX = HUD_WIDTH / 2;
        float centerY = HUD_HEIGHT / 2 + 100;
        shapeRenderer.setColor(0, 0, 0, 0.75f);
        shapeRenderer.rect(centerX - 150, centerY - 280, 300, 420);
    }

    private void drawResultsText() {
        LevelDefinition lvl = levelManager.getCurrentLevel();
        float centerX = HUD_WIDTH / 2 - 100;
        float centerY = HUD_HEIGHT / 2 + 150;
        font.setColor(Color.WHITE);
        if (state == GameState.DOCKED) {
            font.getData().setScale(1.5f);
            font.draw(batch, currentStars == 3 ? "CLEAN DOCK!" : "DOCKED!", centerX + 20, centerY + 80);
            font.getData().setScale(1.2f);
            font.draw(batch, "Rating: " + inputController.getStarString(currentStars), centerX + 40, centerY + 50);
            font.draw(batch, "Payout: $" + (currentPayout + starBonus), centerX + 40, centerY + 20);
            if (starBonus > 0) font.draw(batch, "+ Star Bonus!", centerX + 40, centerY - 5);
            font.draw(batch, "Total Cash: $" + progressManager.getPlayerCash(), centerX + 40, centerY - 40);
        } else {
            font.setColor(Color.RED);
            font.draw(batch, boat.damage >= 100 ? "BOAT TOTALED!" : "FAILED!", centerX + 20, centerY + 80);
            font.setColor(Color.WHITE);
            if (boat.damage >= 100) font.draw(batch, "Repair needed in Garage", centerX + 20, centerY + 50);
        }
    }

    private void drawDebugOverlay() {
        font.setColor(Color.LIME);
        float x = HUD_WIDTH - 220;
        float y = HUD_HEIGHT - 20;
        font.draw(batch, "DEBUG (F1)", x, y);
        font.draw(batch, "Pos: " + (int)boat.x + ", " + (int)boat.y, x, y - 20);
        font.draw(batch, "Vel: " + (int)boat.velocity.len(), x, y - 40);
        font.draw(batch, "Angle: " + (int)boat.angle, x, y - 60);

        if (dock.slipZone != null) {
            font.draw(batch, "Target: " + (int)dock.slipZone.x + "," + (int)dock.slipZone.y, x, y - 80);
            font.draw(batch, "In Zone: " + dock.slipZone.contains(boat.x, boat.y), x, y - 100);
            font.draw(batch, "Progress: " + (int)(dock.getDockingProgress() * 100) + "%", x, y - 120);
            font.draw(batch, "Fail Reason: " + dock.lastFailureReason, x, y - 140);
        }
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
        soundManager.dispose();
    }
}
