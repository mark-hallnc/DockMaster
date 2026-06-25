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
    public enum GameState { TITLE, LEVEL_SELECT, BOAT_SELECT, GARAGE, SETTINGS, TUTORIAL, PLAYING, DOCKED, FAILED }
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
    private LevelDefinition currentLevel;
    private InputController inputController;
    private ProgressManager progressManager;
    private BoatCatalog boatCatalog;
    private SoundManager soundManager;
    private TutorialManager tutorialManager;

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
    private float totalTime = 0;

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
        tutorialManager = new TutorialManager();
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTimer = 3.0f;
    }

    private void loadTrainingLevel() {
        currentLevel = new LevelDefinition(
            "Training Center", "Basics 101",
            400, 100, 90,
            350, 400, 100, 100,
            100, 999f
        );
        currentLevel.addDock(250, 400, 100, 50);
        currentLevel.addDock(450, 400, 100, 50);

        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        if (boat == null) {
            boat = new Boat(400, 100, 90, profile);
        } else {
            boat.profile = profile;
        }
        boat.reset(400, 100, 90, 0);
        boat.boatValue = profile.value;

        dock.setLevel(currentLevel);
        levelTimer = 0;
        tutorialManager.reset();
        wakeTrail.clear();
        floatingText.clear();
        state = GameState.TUTORIAL;

        worldCamera.position.set(boat.x, boat.y, 0);
        worldCamera.update();
    }

    private void loadLevel(int index) {
        levelManager.setCurrentLevel(index);
        currentLevel = levelManager.getCurrentLevel();
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
        totalTime += delta;

        boolean boatTotaled = false;
        if (state == GameState.PLAYING || state == GameState.TUTORIAL || state == GameState.DOCKED || state == GameState.FAILED) {
            if (boat != null && boat.damage >= 100) boatTotaled = true;
        } else {
            BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
            if (progressManager.getBoatDamage(profile.id) >= 100) boatTotaled = true;
        }

        inputController.update(hudViewport, state, boatTotaled);
        handleTransitions();

        if (statusTimer > 0) statusTimer -= delta;

        if (state == GameState.PLAYING || state == GameState.TUTORIAL) {
            levelTimer += delta;
            if (introTimer > 0) introTimer -= delta;
            if (shakeTimer > 0) shakeTimer -= delta;
            if (collisionFeedbackTimer > 0) collisionFeedbackTimer -= delta;

            boat.update(delta, inputController, currentLevel);
            dock.update(boat, delta);
            wakeTrail.update(delta, boat.x, boat.y, boat.velocity.len());
            floatingText.update(delta);

            if (state == GameState.TUTORIAL) {
                tutorialManager.update(delta, boat, inputController, dock);
            }

            if (dock.checkCollision(boat)) {
                if (collisionFeedbackTimer <= 0) {
                    float impact = boat.velocity.len();
                    float dmg = boat.handleCollision(impact);

                    if (state == GameState.TUTORIAL) {
                        boat.damage = 0; // No damage in training
                        dmg = 0;
                    }

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
                if (state == GameState.TUTORIAL) {
                    progressManager.setTutorialCompleted(true);
                    // Stay in tutorial until they confirm next step via transition buttons
                } else {
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
            }
            updateCamera(delta);
        }

        // Motor Loop Update
        boolean motorShouldRun = (state == GameState.PLAYING || state == GameState.TUTORIAL) && boat != null && boat.active;
        soundManager.updateMotorLoop(motorShouldRun, delta,
            boat != null ? boat.velocity.len() : 0,
            inputController.forward, inputController.reverse);

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
            if (state == GameState.TUTORIAL && tutorialManager.getCurrentStep() == TutorialManager.Step.COMPLETE) loadLevel(0);
        }
        if (inputController.trainingPressed) {
            soundManager.play("click");
            loadTrainingLevel();
        }
        if (inputController.skipPressed && state == GameState.TUTORIAL) {
            soundManager.play("click");
            tutorialManager.skip();
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
        drawStyledWater(shapeRenderer);
        dock.draw(shapeRenderer);
        drawEnvironmentalIndicators(shapeRenderer, currentLevel);
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

    private void drawStyledWater(ShapeRenderer shape) {
        // Deep water base
        shape.setColor(0.1f, 0.3f, 0.5f, 1f);
        shape.rect(worldCamera.position.x - WORLD_WIDTH, worldCamera.position.y - WORLD_HEIGHT, WORLD_WIDTH * 2, WORLD_HEIGHT * 2);

        // Animated ripples
        shape.setColor(1, 1, 1, 0.05f);
        for (int i = 0; i < 10; i++) {
            float ox = (float)Math.sin(totalTime * 0.5f + i) * 20f;
            float oy = (float)Math.cos(totalTime * 0.3f + i * 2) * 15f;
            shape.circle(100 + i * 150 + ox, 100 + i * 80 + oy, 4 + (float)Math.sin(totalTime + i) * 2);
        }

        // Small shimmer dots
        shape.setColor(1, 1, 1, 0.1f);
        for (int x = 0; x < 1200; x += 200) {
            for (int y = 0; y < 1000; y += 150) {
                float shift = (float)Math.sin(totalTime * 2f + (x+y)) * 5f;
                shape.rect(x + shift, y + shift, 2, 2);
            }
        }
    }

    private void renderHud(boolean boatTotaled) {
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        inputController.drawShapes(shapeRenderer, state, progressManager.getSelectedBoatId(), boatCatalog, boatTotaled);
        if (state == GameState.DOCKED || state == GameState.FAILED) drawResultsBackdrop();

        // These methods might open/close batches and shapes.
        // We'll keep shapeRenderer.begin() open and let them manage it if needed.
        if (state == GameState.TITLE) drawTitleScreen();
        else if (state == GameState.LEVEL_SELECT) drawLevelSelectScreen();
        else if (state == GameState.BOAT_SELECT) drawBoatSelectScreen();
        else if (state == GameState.GARAGE) drawGarageScreen();
        else if (state == GameState.SETTINGS) {
            shapeRenderer.end();
            batch.begin();
            drawSettingsScreen();
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        } else if (state == GameState.TUTORIAL) {
            shapeRenderer.end();
            drawHUDText();
            batch.begin();
            tutorialManager.draw(batch, font, HUD_WIDTH, HUD_HEIGHT);
            if (tutorialManager.getCurrentStep() == TutorialManager.Step.COMPLETE) {
                drawTutorialCompletePopup();
            }
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        } else if (state == GameState.PLAYING) {
            shapeRenderer.end();
            drawHUDText();
            batch.begin();
            if (introTimer > 0) drawLevelIntro();
            drawDockingGuidance();
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        } else if (state == GameState.DOCKED || state == GameState.FAILED) {
            shapeRenderer.end();
            drawHUDText();
            drawResultsText();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        }

        shapeRenderer.end();

        batch.begin();
        inputController.drawLabels(batch, font, state, levelManager, progressManager, boatCatalog, boatTotaled);

        if (statusTimer > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, statusMessage, HUD_WIDTH / 2 - 100, 50);
            font.setColor(Color.WHITE);
        }

        if (inputController.debugToggled) drawDebugOverlay();
        batch.end();
    }

    private void drawTutorialCompletePopup() {
        float centerX = HUD_WIDTH / 2;
        float centerY = HUD_HEIGHT / 2;

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawPanel(shapeRenderer, centerX - 180, centerY - 100, 360, 200);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.LIME);
        font.draw(batch, "TRAINING COMPLETE!", centerX - 100, centerY + 60);
        font.setColor(Color.WHITE);
        font.draw(batch, "Press START to begin Level 1", centerX - 120, centerY + 10);
        font.draw(batch, "or TITLE to exit.", centerX - 80, centerY - 20);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawSettingsScreen() {
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawPanel(shapeRenderer, HUD_WIDTH / 2 - 200, 100, 400, 350);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "SETTINGS", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 40);
        font.setColor(Color.WHITE);
        font.draw(batch, "Adjust your experience preferences.", HUD_WIDTH / 2 - 140, HUD_HEIGHT - 80);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawLevelIntro() {
        LevelDefinition lvl = currentLevel;
        float alpha = Math.min(1.0f, introTimer);

        // Draw background bar
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setColor(0, 0, 0, alpha * 0.5f);
        shapeRenderer.rect(0, HUD_HEIGHT / 2 - 20, HUD_WIDTH, 120);
        shapeRenderer.end();

        batch.begin();
        font.setColor(1, 1, 0, alpha); // Yellow
        font.draw(batch, "DESTINATION: " + lvl.destinationName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 80);
        font.setColor(1, 1, 1, alpha);
        font.draw(batch, "LEVEL: " + lvl.levelName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 55);
        font.draw(batch, "PAR TIME: " + (int)lvl.parTimeSeconds + "s", HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 30);
        font.setColor(Color.WHITE);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawDockingGuidance() {
        float speed = boat.velocity.len();
        if (dock.isInsideSlipZone(boat)) {
            font.setColor(Color.LIME);
            font.draw(batch, "STABILIZING: " + (int)(dock.getDockingProgress() * 100) + "%", HUD_WIDTH / 2 - 70, HUD_HEIGHT - 60);
        } else if (dock.slipZone.contains(boat.x, boat.y)) {
            if (speed >= 35f) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "!!! TOO FAST! SLOW DOWN !!!", HUD_WIDTH / 2 - 120, HUD_HEIGHT - 60);
            } else {
                font.setColor(Color.YELLOW);
                font.draw(batch, "ALIGN BOAT TO DOCK", HUD_WIDTH / 2 - 90, HUD_HEIGHT - 60);
            }
        }

        // Speed warning if near docks
        if (speed > 180f) {
            boolean near = false;
            for (com.badlogic.gdx.math.Polygon p : dock.collisionPolys) {
                if (new Vector2(boat.x, boat.y).dst(p.getX() + 50, p.getY() + 50) < 120) near = true;
            }
            if (near) {
                font.setColor(Color.RED);
                font.draw(batch, "!!! DANGER: SLOW DOWN !!!", HUD_WIDTH / 2 - 110, 150);
            }
        }
        font.setColor(Color.WHITE);
    }

    private void drawTitleScreen() {
        // Draw stylized background directly in hud pass
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawStyledWater(shapeRenderer);
        // Overlay a dark tint
        shapeRenderer.setColor(0, 0, 0.2f, 0.4f);
        shapeRenderer.rect(0, 0, HUD_WIDTH, HUD_HEIGHT);
        shapeRenderer.end();

        batch.begin();
        titleFont.setColor(Color.YELLOW);
        titleFont.draw(batch, "DockMaster", HUD_WIDTH / 2 - 180, HUD_HEIGHT - 60);
        font.setColor(Color.WHITE);
        font.draw(batch, "The Premier Boat Docking Challenge", HUD_WIDTH / 2 - 160, HUD_HEIGHT - 120);

        // Info Strip
        font.draw(batch, "Cash: $" + progressManager.getPlayerCash() + " | Stars: " + progressManager.getTotalStars(levelManager.getLevels().size()), 20, 40);
        font.draw(batch, "Active Boat: " + boatCatalog.getBoatById(progressManager.getSelectedBoatId()).displayName, 20, 70);

        if (!progressManager.isTutorialCompleted()) {
            font.setColor(Color.ORANGE);
            font.draw(batch, ">>> NEW PLAYER? TRY TRAINING <<<", HUD_WIDTH / 2 - 130, 295);
            font.setColor(Color.WHITE);
        }
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawLevelSelectScreen() {
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawStyledWater(shapeRenderer);
        shapeRenderer.setColor(0, 0, 0, 0.4f);
        shapeRenderer.rect(0, 0, HUD_WIDTH, HUD_HEIGHT);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "SELECT LEVEL", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
        font.setColor(Color.WHITE);
        font.draw(batch, "Total Stars: " + progressManager.getTotalStars(levelManager.getLevels().size()), 20, 40);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawBoatSelectScreen() {
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawStyledWater(shapeRenderer);
        shapeRenderer.setColor(0, 0, 0, 0.4f);
        shapeRenderer.rect(0, 0, HUD_WIDTH, HUD_HEIGHT);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "SELECT BOAT", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawGarageScreen() {
        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        float damage = progressManager.getBoatDamage(profile.id);
        long value = progressManager.getBoatValue(profile.id, profile.value);
        int cost = (int)(damage * 20);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawPanel(shapeRenderer, 50, 100, 700, 400);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "GARAGE", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 40);

        font.setColor(Color.WHITE);
        font.draw(batch, "Boat: " + profile.displayName, 100, 450);
        font.getData().setScale(0.9f);
        font.draw(batch, profile.description, 100, 425);
        font.getData().setScale(1.2f);

        font.setColor(damage > 50 ? Color.RED : (damage > 0 ? Color.YELLOW : Color.LIME));
        font.draw(batch, "Condition: " + (int)(100 - damage) + "%", 100, 360);
        font.setColor(Color.WHITE);
        font.draw(batch, "Current Resale Value: $" + value, 100, 330);
        font.draw(batch, "Original Value: $" + profile.value, 100, 305);

        font.draw(batch, "Your Cash: $" + progressManager.getPlayerCash(), 450, 360);

        if (damage > 0) {
            font.setColor(Color.ORANGE);
            font.draw(batch, "Estimated Repair: $" + cost, 450, 330);
            if (progressManager.getPlayerCash() < cost) {
                font.setColor(Color.RED);
                font.draw(batch, "INSUFFICIENT FUNDS", 450, 305);
            }
        } else {
            font.setColor(Color.LIME);
            font.draw(batch, "NO REPAIRS NEEDED", 450, 330);
        }
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
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

        float pulse = (float)(Math.sin(totalTime * 4f + x + y) + 1.0f) * 0.2f;
        shape.setColor(1, 1, 1, 0.3f + pulse);

        float angle = force.angleDeg();
        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        shape.rect(0, -1, size, 2);
        shape.triangle(size, -4, size, 4, size + 6, 0);

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
        LevelDefinition lvl = currentLevel;
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
        LevelDefinition lvl = currentLevel;

        // Stats Panel
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        drawPanel(shapeRenderer, 10, HUD_HEIGHT - 170, 200, 160);
        shapeRenderer.end();

        batch.begin();
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
        font.draw(batch, "Time: " + (int)levelTimer + "s", 20, top - 120);
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawPanel(ShapeRenderer shape, float x, float y, float w, float h) {
        // Shadow
        shape.setColor(0, 0, 0, 0.4f);
        shape.rect(x + 4, y - 4, w, h);
        // Background
        shape.setColor(0.1f, 0.15f, 0.25f, 0.8f);
        shape.rect(x, y, w, h);
        // Border
        shape.setColor(1, 1, 1, 0.2f);
        shape.rect(x, y, w, 2);
        shape.rect(x, y + h - 2, w, 2);
        shape.rect(x, y, 2, h);
        shape.rect(x + w - 2, y, 2, h);
    }

    private void drawResultsBackdrop() {
        float centerX = HUD_WIDTH / 2;
        float centerY = HUD_HEIGHT / 2 + 100;
        drawPanel(shapeRenderer, centerX - 180, centerY - 280, 360, 420);
    }

    private void drawResultsText() {
        LevelDefinition lvl = currentLevel;
        float centerX = HUD_WIDTH / 2 - 120;
        float centerY = HUD_HEIGHT / 2 + 130;

        batch.begin();
        if (state == GameState.DOCKED) {
            font.getData().setScale(1.8f);
            font.setColor(Color.LIME);
            font.draw(batch, currentStars == 3 ? "CLEAN DOCK!" : "SUCCESS!", centerX, centerY + 80);
            font.getData().setScale(1.2f);

            font.setColor(Color.YELLOW);
            font.draw(batch, "RATING: " + inputController.getStarString(currentStars), centerX, centerY + 40);

            font.setColor(Color.WHITE);
            font.draw(batch, "Base Payout: $" + lvl.basePayout, centerX, centerY + 10);
            font.draw(batch, "Time Bonus: $" + timeBonus, centerX, centerY - 15);
            font.setColor(Color.ORANGE);
            font.draw(batch, "Damage Penalty: -$" + damagePenalty, centerX, centerY - 40);

            if (starBonus > 0) {
                font.setColor(Color.GOLD);
                font.draw(batch, "Star Bonus: +$" + starBonus, centerX, centerY - 65);
            }

            font.setColor(Color.LIME);
            font.draw(batch, "FINAL EARNINGS: $" + (currentPayout + starBonus), centerX, centerY - 100);
        } else {
            font.getData().setScale(1.8f);
            font.setColor(Color.RED);
            font.draw(batch, boat.damage >= 100 ? "BOAT TOTALED!" : "FAILED!", centerX, centerY + 80);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            if (boat.damage >= 100) {
                font.draw(batch, "Visit the Garage for repairs.", centerX, centerY + 30);
            } else {
                font.draw(batch, "Try again to dock safely.", centerX, centerY + 30);
            }
        }
        batch.end();
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

        font.draw(batch, "Motor Vol: " + String.format("%.2f", soundManager.getMotorVolume()), x, y - 160);
        font.draw(batch, "Motor Pitch: " + String.format("%.2f", soundManager.getMotorPitch()), x, y - 180);
    }

    @Override
    public void pause() {
        soundManager.stopMotorLoop();
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
