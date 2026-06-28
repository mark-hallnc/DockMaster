package com.markseagle.dockmaster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.List;

public class DockMasterGame extends ApplicationAdapter {
    public enum GameState { TITLE, LEVEL_SELECT, BOAT_SELECT, GARAGE, SETTINGS, TUTORIAL, PLAYING, DOCKED, FAILED, PAUSED }
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
    private TextureManager textureManager;

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
    private boolean showHelp = false;

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;
    private static final float HUD_WIDTH = 800;
    private static final float HUD_HEIGHT = 600;

    private static final boolean USE_TEXTURES = true;

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
        textureManager = new TextureManager();

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

        // Apply Upgrades
        boat.engineLevel = progressManager.getUpgradeLevel(profile.id, "engine");
        boat.steeringLevel = progressManager.getUpgradeLevel(profile.id, "steering");
        boat.hullLevel = progressManager.getUpgradeLevel(profile.id, "hull");
        boat.reverseLevel = progressManager.getUpgradeLevel(profile.id, "reverse");

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

    private float dockingFeedbackTimer = 0;

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

        inputController.update(delta, hudViewport, state, boatTotaled, progressManager.getControlMode());
        handleTransitions();

        if (statusTimer > 0) statusTimer -= delta;

        if (state == GameState.PLAYING || state == GameState.TUTORIAL) {
            levelTimer += delta;
            if (introTimer > 0) introTimer -= delta;
            if (shakeTimer > 0) shakeTimer -= delta;
            if (collisionFeedbackTimer > 0) collisionFeedbackTimer -= delta;

            boat.update(delta, inputController, currentLevel);
            dock.update(boat, delta);

            // Wake from stern
            Vector2 stern = boat.getSternPos();
            wakeTrail.update(delta, stern.x, stern.y, boat.velocity.len());

            // Throttle turbulence
            if (inputController.forward) {
                wakeTrail.spawn(stern.x, stern.y, 200f);
            }
            if (inputController.reverse) {
                Vector2 bow = boat.getBowPos();
                wakeTrail.spawn(bow.x, bow.y, 100f);
            }

            floatingText.update(delta);

            if (state == GameState.TUTORIAL) {
                tutorialManager.update(delta, boat, inputController, dock);
            }

            // Proximity/Approach feedback
            if (dock.slipZone.contains(boat.x, boat.y)) {
                dockingFeedbackTimer += delta;
                if (dockingFeedbackTimer > 1.5f) {
                    dockingFeedbackTimer = 0;
                    float speed = boat.velocity.len();
                    if (speed > 40f) floatingText.spawn("TOO FAST!", boat.x, boat.y + 20, Color.ORANGE);
                    else if (speed > 5f) floatingText.spawn("GOOD SPEED", boat.x, boat.y + 20, Color.LIME);
                }
            } else {
                dockingFeedbackTimer = 0;
            }

            if (dock.checkCollision(boat)) {
                if (collisionFeedbackTimer <= 0) {
                    float impact = boat.velocity.len();
                    Vector2 impactVelocity = new Vector2(boat.velocity);
                    float dmg = boat.handleCollision(impact);

                    // Robust Collision Separation
                    boat.restorePreviousPosition();

                    // Iterative pushback
                    int iterations = 10;
                    Vector2 pushDir = new Vector2(impactVelocity).nor().scl(-1f);
                    if (pushDir.len() < 0.1f) pushDir.set(0, 1); // Fallback

                    for (int i = 0; i < iterations; i++) {
                        if (!dock.checkCollision(boat)) break;
                        boat.x += pushDir.x * 4f;
                        boat.y += pushDir.y * 4f;
                        boat.updateBoundsPublic();
                    }

                    // Cap bounce velocity
                    boat.velocity.limit(80f);
                    boat.updateBoundsPublic();

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

                // Failure Feedback
                floatingText.spawn("BOAT TOTALED!", boat.x, boat.y + 40, Color.RED);
                shakeTimer = 0.5f;

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

                    // Success Feedback
                    String successMsg = "SUCCESS!";
                    if (currentStars == 3) successMsg = "PERFECT DOCK!";
                    else if (currentStars == 2) successMsg = "GREAT JOB!";
                    floatingText.spawn(successMsg, boat.x, boat.y + 40, Color.LIME);

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
            inputController.throttleValue);

        ScreenUtils.clear(0.1f, 0.3f, 0.5f, 1f);

        if (state == GameState.PLAYING || state == GameState.DOCKED || state == GameState.FAILED || state == GameState.TUTORIAL || state == GameState.PAUSED) {
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

        if (inputController.controlModeToggled) {
            String nextMode = progressManager.getControlMode().equals("buttons") ? "boat" : "buttons";
            progressManager.setControlMode(nextMode);
            soundManager.play("click");
        }

        if (inputController.pausePressed) {
            soundManager.play("click");
            if (state == GameState.PLAYING || state == GameState.TUTORIAL) state = GameState.PAUSED;
            else if (state == GameState.PAUSED) state = GameState.PLAYING;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.H)) {
            showHelp = !showHelp;
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

        if (state == GameState.GARAGE) {
            if (inputController.upgradeEnginePressed) performUpgrade("engine");
            if (inputController.upgradeSteeringPressed) performUpgrade("steering");
            if (inputController.upgradeHullPressed) performUpgrade("hull");
            if (inputController.upgradeReversePressed) performUpgrade("reverse");
        }
    }

    private void performUpgrade(String category) {
        String boatId = progressManager.getSelectedBoatId();
        int currentLevel = progressManager.getUpgradeLevel(boatId, category);
        if (currentLevel >= 3) {
            showStatus("Already at max level!");
            soundManager.play("fail");
            return;
        }

        int cost = InputController.getUpgradeCost(currentLevel + 1);
        if (progressManager.getPlayerCash() >= cost) {
            progressManager.spendCash(cost);
            progressManager.setUpgradeLevel(boatId, category, currentLevel + 1);
            showStatus(category.toUpperCase() + " Upgraded!");
            soundManager.play("cash");
            vibrate(100);
        } else {
            showStatus("Not enough cash!");
            soundManager.play("fail");
        }
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

        // --- PASS 1: BACKGROUND SPRITES ---
        batch.begin();
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (USE_TEXTURES) {
            drawTexturedWater(batch);
        }
        batch.end();

        // --- PASS 2: SHAPES (Fallbacks and effects) ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw water shapes over background if needed, or if no background
        if (!USE_TEXTURES || !textureManager.hasTexture("water_tile")) {
            drawStyledWater(shapeRenderer);
        } else {
            drawWaterDetails(shapeRenderer); // Shimmer over texture
        }

        // Draw docks if NO texture exists
        if (!USE_TEXTURES || !textureManager.hasTexture("dock_plank")) {
            dock.drawDocks(shapeRenderer);
        }

        dock.drawSlipZoneFilled(shapeRenderer, boat);
        drawEnvironmentalIndicators(shapeRenderer, currentLevel);

        if (!USE_TEXTURES || !textureManager.hasTexture("buoy")) {
            drawWaterDetails(shapeRenderer);
        }

        wakeTrail.draw(shapeRenderer);

        // Draw boat if NO texture exists
        String boatTexKey = "boat_" + boat.profile.id;
        if (!USE_TEXTURES || !textureManager.hasTexture(boatTexKey)) {
            boat.draw(shapeRenderer);
        }

        shapeRenderer.end();

        // --- PASS 2.5: LINES ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        dock.drawSlipZoneLines(shapeRenderer, boat);
        if (inputController.debugToggled && dock.slipZone != null) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(dock.slipZone.x, dock.slipZone.y, dock.slipZone.width, dock.slipZone.height);
        }
        shapeRenderer.end();

        // --- PASS 3: WORLD SPRITES (Textured objects) ---
        batch.begin();
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (USE_TEXTURES) {
            if (textureManager.hasTexture("dock_plank")) {
                drawTexturedDocks(batch);
            }

            drawTexturedDecor(batch);

            if (textureManager.hasTexture(boatTexKey)) {
                drawTexturedBoat(batch, boatTexKey);
            }
        }

        floatingText.draw(batch, font);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        worldCamera.position.sub(sx, sy, 0);
        worldCamera.update();
    }

    private void drawTexturedWater(SpriteBatch batch) {
        Texture water = textureManager.getTexture("water_tile");
        if (water == null) return;

        // Draw large enough to cover the view and support some camera movement
        // We use UV coordinates to repeat the texture based on world position
        float startX = worldCamera.position.x - WORLD_WIDTH * worldCamera.zoom;
        float startY = worldCamera.position.y - WORLD_HEIGHT * worldCamera.zoom;
        float width = WORLD_WIDTH * 2 * worldCamera.zoom;
        float height = WORLD_HEIGHT * 2 * worldCamera.zoom;

        // Tiling calculation based on world units
        float u = startX / water.getWidth();
        float v = -startY / water.getHeight(); // Flip Y for typical UV space
        float u2 = u + width / water.getWidth();
        float v2 = v - height / water.getHeight();

        batch.draw(water, startX, startY, width, height, u, v, u2, v2);
    }

    private void drawTexturedDocks(SpriteBatch batch) {
        Texture plank = textureManager.getTexture("dock_plank");
        if (plank == null) return;

        Texture piling = textureManager.getTexture("dock_piling");
        Texture bumper = textureManager.getTexture("dock_tire_bumper");
        Texture cleat = textureManager.getTexture("dock_cleat");

        for (com.badlogic.gdx.math.Polygon p : dock.collisionPolys) {
            float x = p.getX();
            float y = p.getY();
            float w = p.getVertices()[2];
            float h = p.getVertices()[5];

            // 1. Tiled Plank Texture
            float tileSize = 64f;
            for (float tx = 0; tx < w; tx += tileSize) {
                for (float ty = 0; ty < h; ty += tileSize) {
                    float drawW = Math.min(tileSize, w - tx);
                    float drawH = Math.min(tileSize, h - ty);

                    // Use u2, v2 to clip if needed
                    float u2 = drawW / plank.getWidth();
                    float v2 = drawH / plank.getHeight();

                    batch.draw(plank, x + tx, y + ty, drawW, drawH, 0, 0, u2, v2);
                }
            }

            // 2. Optional Detail: Pilings at corners
            if (piling != null) {
                float s = 16f;
                batch.draw(piling, x - s/2, y - s/2, s, s);
                batch.draw(piling, x + w - s/2, y - s/2, s, s);
                batch.draw(piling, x - s/2, y + h - s/2, s, s);
                batch.draw(piling, x + w - s/2, y + h - s/2, s, s);
            }

            // 3. Optional Detail: Tire Bumpers along longest sides
            if (bumper != null) {
                float bs = 24f;
                if (w > h) {
                    for (float bx = x + 30; bx < x + w - 30; bx += 60) {
                        batch.draw(bumper, bx - bs/2, y - bs/4, bs, bs);
                        batch.draw(bumper, bx - bs/2, y + h - bs*0.75f, bs, bs);
                    }
                } else {
                    for (float by = y + 30; by < y + h - 30; by += 60) {
                        batch.draw(bumper, x - bs/4, by - bs/2, bs, bs);
                        batch.draw(bumper, x + w - bs*0.75f, by - bs/2, bs, bs);
                    }
                }
            }

            // 4. Optional Detail: Cleats
            if (cleat != null && w > 40 && h > 40) {
                batch.draw(cleat, x + w/2 - 8, y + h/2 - 8, 16, 16);
            }
        }
    }

    private void drawTexturedDecor(SpriteBatch batch) {
        Texture buoy = textureManager.getTexture("buoy");
        Texture markerGreen = textureManager.getTexture("channel_marker_green");
        Texture markerRed = textureManager.getTexture("channel_marker_red");
        Texture pump = textureManager.getTexture("fuel_pump");
        Texture umbrella = textureManager.getTexture("umbrella");

        // Basic Buoys
        if (buoy != null) {
            batch.draw(buoy, 100-16, 100-16, 32, 32);
            batch.draw(buoy, 700-16, 500-16, 32, 32);
        }

        // Destination / Level Specific Decor
        String dest = currentLevel.destinationName;
        if (dest.equals("Lake Resort")) {
            if (umbrella != null) {
                batch.draw(umbrella, 50, 50, 48, 48);
                batch.draw(umbrella, 700, 40, 48, 48);
            }
        } else if (dest.equals("Coastal Harbor")) {
            if (markerGreen != null) batch.draw(markerGreen, 300, 100, 32, 32);
            if (markerRed != null) batch.draw(markerRed, 350, 100, 32, 32);
        }

        // Fuel Pump for Fuel Levels
        if (pump != null && currentLevel.levelName.toLowerCase().contains("fuel")) {
            // Find a dock to place it near, or just a fixed spot for now
            batch.draw(pump, 310, 460, 32, 32);
        }
    }

    private void drawTexturedBoat(SpriteBatch batch, String texKey) {
        Texture tex = textureManager.getTexture(texKey);
        if (tex == null) return;

        float l = boat.profile.length * boat.profile.visualScale;
        float w = boat.profile.width * boat.profile.visualScale;

        // In our ShapeRenderer, boat points right at 0 degrees, and 90 is up.
        // LibGDX draw() with rotation assumes the texture points right at 0 degrees.
        // So this should align perfectly if the PNG points right.
        batch.draw(tex,
            boat.x - l/2, boat.y - w/2, // position
            l/2, w/2,                   // origin for rotation
            l, w,                       // size
            1f, 1f,                     // scale
            boat.angle,                 // rotation
            0, 0,                       // srcX, srcY
            tex.getWidth(), tex.getHeight(), // srcW, srcH
            false, false                // flip
        );
    }

    private void drawStyledWater(ShapeRenderer shape) {
        // Deep water base
        shape.setColor(0.1f, 0.3f, 0.5f, 1f);
        shape.rect(worldCamera.position.x - WORLD_WIDTH, worldCamera.position.y - WORLD_HEIGHT, WORLD_WIDTH * 2, WORLD_HEIGHT * 2);

        drawWaterDetails(shape);
    }

    private void drawWaterDetails(ShapeRenderer shape) {
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

    /**
     * Renders HUD and Menus in two passes to avoid mixing ShapeRenderer and SpriteBatch.
     * Pass 1: Shapes only.
     * Pass 2: Text only.
     */
    private void renderHud(boolean boatTotaled) {
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        // --- PASS 0: BACKGROUND SPRITES (for menus) ---
        if (USE_TEXTURES && (state == GameState.TITLE || state == GameState.LEVEL_SELECT || state == GameState.BOAT_SELECT)) {
            batch.begin();
            Texture water = textureManager.getTexture("water_tile");
            if (water != null) {
                batch.draw(water, 0, 0, HUD_WIDTH, HUD_HEIGHT, 0, 0, (int)(HUD_WIDTH/water.getWidth()), (int)(HUD_HEIGHT/water.getHeight()));
            }
            batch.end();
        }

        // --- PASS 1: SHAPES ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        inputController.drawShapes(shapeRenderer, state, progressManager.getSelectedBoatId(), boatCatalog, boatTotaled, progressManager.getControlMode());

        if (state == GameState.TITLE) {
            if (!USE_TEXTURES || !textureManager.hasTexture("water_tile")) {
                drawStyledWater(shapeRenderer);
            }
            shapeRenderer.setColor(0, 0, 0.2f, 0.4f);
            shapeRenderer.rect(0, 0, HUD_WIDTH, HUD_HEIGHT);
        } else if (state == GameState.LEVEL_SELECT || state == GameState.BOAT_SELECT) {
            if (!USE_TEXTURES || !textureManager.hasTexture("water_tile")) {
                drawStyledWater(shapeRenderer);
            }
            shapeRenderer.setColor(0, 0, 0, 0.4f);
            shapeRenderer.rect(0, 0, HUD_WIDTH, HUD_HEIGHT);
        } else if (state == GameState.GARAGE) {
            drawPanel(shapeRenderer, 50, 100, 700, 400);
        } else if (state == GameState.SETTINGS) {
            drawPanel(shapeRenderer, HUD_WIDTH / 2 - 200, 100, 400, 350);
        } else if (state == GameState.PAUSED) {
            drawPanel(shapeRenderer, HUD_WIDTH / 2 - 150, 80, 300, 380);
        } else if (state == GameState.TUTORIAL) {
            drawHUDShapes();
            if (tutorialManager.getCurrentStep() == TutorialManager.Step.COMPLETE) {
                drawTutorialCompleteShapes();
            }
        } else if (state == GameState.PLAYING || state == GameState.PAUSED) {
            drawHUDShapes();
            if (introTimer > 0 && state != GameState.PAUSED) drawLevelIntroShapes();
        } else if (state == GameState.DOCKED || state == GameState.FAILED) {
            drawHUDShapes();
            drawResultsBackdrop();
        }

        if (showHelp) {
            drawPanel(shapeRenderer, HUD_WIDTH / 2 - 150, HUD_HEIGHT / 2 - 100, 300, 200);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- PASS 2: TEXT ---
        batch.begin();
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        inputController.drawLabels(batch, font, state, levelManager, progressManager, boatCatalog, boatTotaled, textureManager);

        if (state == GameState.TITLE) {
            drawTitleText();
        } else if (state == GameState.LEVEL_SELECT) {
            drawLevelSelectText();
        } else if (state == GameState.BOAT_SELECT) {
            drawBoatSelectText();
            if (USE_TEXTURES) drawBoatPreviews();
        } else if (state == GameState.GARAGE) {
            drawGarageText();
        } else if (state == GameState.SETTINGS) {
            drawSettingsText();
        } else if (state == GameState.TUTORIAL) {
            drawHUDText();
            tutorialManager.draw(batch, font, HUD_WIDTH, HUD_HEIGHT, progressManager.getControlMode());
            if (tutorialManager.getCurrentStep() == TutorialManager.Step.COMPLETE) {
                drawTutorialCompleteText();
            }
        } else if (state == GameState.PLAYING || state == GameState.PAUSED) {
            drawHUDText();
            if (state != GameState.PAUSED && introTimer > 0) drawLevelIntroText();
            drawDockingGuidanceText();
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
        if (showHelp) drawHelpText();
        batch.end();
    }

    private void drawHelpText() {
        font.setColor(Color.YELLOW);
        font.draw(batch, "CONTROLS & HELP", HUD_WIDTH / 2 - 80, HUD_HEIGHT / 2 + 80);
        font.setColor(Color.WHITE);
        font.draw(batch, "W/FWD: Forward Throttle", HUD_WIDTH / 2 - 130, HUD_HEIGHT / 2 + 50);
        font.draw(batch, "S/REV: Reverse Throttle", HUD_WIDTH / 2 - 130, HUD_HEIGHT / 2 + 30);
        font.draw(batch, "A/D: Steer Left/Right", HUD_WIDTH / 2 - 130, HUD_HEIGHT / 2 + 10);
        font.draw(batch, "Dock slowly in green zone", HUD_WIDTH / 2 - 130, HUD_HEIGHT / 2 - 20);
        font.draw(batch, "Repair damage in Garage", HUD_WIDTH / 2 - 130, HUD_HEIGHT / 2 - 40);
        font.draw(batch, "Press H to close", HUD_WIDTH / 2 - 60, HUD_HEIGHT / 2 - 70);
    }

    private void drawHUDShapes() {
        drawPanel(shapeRenderer, 10, HUD_HEIGHT - 170, 200, 160);
    }

    private void drawHUDText() {
        LevelDefinition lvl = currentLevel;

        Texture cashIcon = textureManager.getTexture("ui_cash");
        Texture damageIcon = textureManager.getTexture("ui_damage");

        font.setColor(Color.WHITE);
        float top = HUD_HEIGHT - 20;
        font.draw(batch, "Boat: " + boat.profile.displayName, 20, top);
        font.draw(batch, "Dest: " + lvl.destinationName, 20, top - 20);

        if (cashIcon != null) batch.draw(cashIcon, 20, top - 55, 16, 16);
        font.draw(batch, "Cash: $" + progressManager.getPlayerCash(), cashIcon != null ? 40 : 20, top - 40);

        font.draw(batch, "Speed: " + (int)boat.velocity.len(), 20, top - 60);

        Color dmgColor = boat.damage > 50 ? Color.RED : (boat.damage > 20 ? Color.YELLOW : Color.WHITE);
        font.setColor(dmgColor);
        if (damageIcon != null) {
            batch.setColor(dmgColor);
            batch.draw(damageIcon, 20, top - 95, 16, 16);
            batch.setColor(Color.WHITE);
        }
        font.draw(batch, "Damage: " + (int)boat.damage + "%", damageIcon != null ? 40 : 20, top - 80);

        font.setColor(Color.WHITE);
        font.draw(batch, "Boat Val: $" + boat.boatValue, 20, top - 100);
        font.draw(batch, "Time: " + (int)levelTimer + "s", 20, top - 120);
    }

    private void drawTutorialCompleteShapes() {
        drawPanel(shapeRenderer, HUD_WIDTH / 2 - 180, HUD_HEIGHT / 2 - 100, 360, 200);
    }

    private void drawTutorialCompleteText() {
        float centerX = HUD_WIDTH / 2;
        float centerY = HUD_HEIGHT / 2;
        font.setColor(Color.LIME);
        font.draw(batch, "TRAINING COMPLETE!", centerX - 100, centerY + 60);
        font.setColor(Color.WHITE);
        font.draw(batch, "Press START to begin Level 1", centerX - 120, centerY + 10);
        font.draw(batch, "or TITLE to exit.", centerX - 80, centerY - 20);
    }

    private void drawSettingsText() {
        font.setColor(Color.YELLOW);
        font.draw(batch, "SETTINGS", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 40);
        font.setColor(Color.WHITE);
        font.draw(batch, "Adjust your experience preferences.", HUD_WIDTH / 2 - 140, HUD_HEIGHT - 80);
    }

    private void drawLevelIntroShapes() {
        float alpha = Math.min(1.0f, introTimer);
        shapeRenderer.setColor(0, 0, 0, alpha * 0.5f);
        shapeRenderer.rect(0, HUD_HEIGHT / 2 - 20, HUD_WIDTH, 120);
    }

    private void drawLevelIntroText() {
        LevelDefinition lvl = currentLevel;
        float alpha = Math.min(1.0f, introTimer);
        font.setColor(1, 1, 0, alpha); // Yellow
        font.draw(batch, "DESTINATION: " + lvl.destinationName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 105);
        font.setColor(1, 1, 1, alpha);
        font.draw(batch, "LEVEL: " + lvl.levelName, HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 80);
        font.draw(batch, "PAR TIME: " + (int)lvl.parTimeSeconds + "s", HUD_WIDTH / 2 - 120, HUD_HEIGHT / 2 + 55);

        font.setColor(0, 1, 1, alpha); // Cyan
        font.draw(batch, "OBJECTIVE: Dock safely without totaling.", HUD_WIDTH / 2 - 140, HUD_HEIGHT / 2 + 30);

        if (lvl.windForce.len() > 0 || !lvl.currentZones.isEmpty()) {
            font.setColor(1, 0.5f, 0, alpha); // Orange
            font.draw(batch, "WARNING: WIND/CURRENT DETECTED", HUD_WIDTH / 2 - 140, HUD_HEIGHT / 2 + 5);
        }

        font.setColor(Color.WHITE);
    }

    private void drawDockingGuidanceText() {
        float speed = boat.velocity.len();
        if (dock.isInsideSlipZone(boat)) {
            font.setColor(Color.LIME);
            font.draw(batch, "HOLD STEADY: " + (int)(dock.getDockingProgress() * 100) + "%", HUD_WIDTH / 2 - 70, HUD_HEIGHT - 60);
        } else if (dock.slipZone.contains(boat.x, boat.y)) {
            if (speed >= dock.dockingMaxSpeed) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "!!! TOO FAST !!!", HUD_WIDTH / 2 - 60, HUD_HEIGHT - 60);
            } else if (dock.checkCollision(boat)) {
                font.setColor(Color.RED);
                font.draw(batch, "TOUCHING DOCK", HUD_WIDTH / 2 - 60, HUD_HEIGHT - 60);
            } else {
                font.setColor(Color.YELLOW);
                font.draw(batch, "ALIGN WITH DOCK ARROW", HUD_WIDTH / 2 - 100, HUD_HEIGHT - 60);
            }
        } else {
            // Distance check to show "Dock Here" when near but not inside
            float dst = new Vector2(boat.x, boat.y).dst(dock.slipZone.x + dock.slipZone.width/2, dock.slipZone.y + dock.slipZone.height/2);
            if (dst < 250) {
                font.setColor(Color.CYAN);
                font.draw(batch, "DOCK HERE", HUD_WIDTH / 2 - 40, HUD_HEIGHT - 60);
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

    private void drawTitleText() {
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
    }

    private void drawLevelSelectText() {
        font.setColor(Color.YELLOW);
        font.draw(batch, "SELECT LEVEL", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);
        font.setColor(Color.WHITE);
        font.draw(batch, "Total Stars: " + progressManager.getTotalStars(levelManager.getLevels().size()), 20, 40);
    }

    private void drawBoatSelectText() {
        font.setColor(Color.YELLOW);
        font.draw(batch, "SELECT BOAT", HUD_WIDTH / 2 - 80, HUD_HEIGHT - 40);

        BoatDefinition b = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        int e = progressManager.getUpgradeLevel(b.id, "engine");
        int s = progressManager.getUpgradeLevel(b.id, "steering");
        int h = progressManager.getUpgradeLevel(b.id, "hull");
        int r = progressManager.getUpgradeLevel(b.id, "reverse");
        font.setColor(Color.WHITE);
        font.draw(batch, "Selected Upgrades: E" + e + " S" + s + " H" + h + " R" + r, 20, 40);
    }

    private void drawBoatPreviews() {
        List<BoatDefinition> boats = boatCatalog.getBoats();
        List<Rectangle> buttons = inputController.boatButtons;
        for (int i = 0; i < boats.size(); i++) {
            BoatDefinition b = boats.get(i);
            Texture tex = textureManager.getTexture("boat_" + b.id);
            if (tex != null) {
                Rectangle r = buttons.get(i);
                // Draw a mini preview centered in the right side of the card
                float size = 60;
                batch.draw(tex, r.x + r.width - size - 20, r.y + (r.height - size/2) / 2, size, size/2);
            }
        }
    }

    private void drawGarageText() {
        BoatDefinition profile = boatCatalog.getBoatById(progressManager.getSelectedBoatId());
        float damage = progressManager.getBoatDamage(profile.id);
        long value = progressManager.getBoatValue(profile.id, profile.value);
        int cost = (int)(damage * 20);

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

        // Target position based on boat + look ahead
        float lookAheadFactor = 0.6f;
        float targetX = boat.x + boat.velocity.x * lookAheadFactor;
        float targetY = boat.y + boat.velocity.y * lookAheadFactor;

        worldCamera.position.x += (targetX - worldCamera.position.x) * lerp * delta;
        worldCamera.position.y += (targetY - worldCamera.position.y) * lerp * delta;

        // Speed based zoom (Slightly closer overall by 10%)
        float minZoom = 0.75f; // reduced from 0.85
        float maxZoom = 1.25f; // reduced from 1.35
        float speedNormalized = MathUtils.clamp(boat.velocity.len() / 350f, 0, 1);
        float targetZoom = minZoom + (maxZoom - minZoom) * speedNormalized;

        worldCamera.zoom += (targetZoom - worldCamera.zoom) * (lerp * 0.5f) * delta;

        // Bounds clamping (example 2000x2000 world)
        // For now let's just keep it centered.

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

        Texture starFilled = textureManager.getTexture("ui_star_filled");
        Texture starEmpty = textureManager.getTexture("ui_star_empty");

        if (state == GameState.DOCKED) {
            font.getData().setScale(1.8f);
            font.setColor(Color.LIME);
            font.draw(batch, currentStars == 3 ? "CLEAN DOCK!" : "SUCCESS!", centerX, centerY + 80);
            font.getData().setScale(1.2f);

            font.setColor(Color.YELLOW);
            font.draw(batch, "RATING: ", centerX, centerY + 40);
            if (starFilled != null && starEmpty != null) {
                for (int i = 0; i < 3; i++) {
                    batch.draw(i < currentStars ? starFilled : starEmpty, centerX + 90 + i * 30, centerY + 18, 24, 24);
                }
            } else {
                font.draw(batch, inputController.getStarString(currentStars), centerX + 90, centerY + 40);
            }

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
    }

    private void drawDebugOverlay() {
        font.setColor(Color.LIME);
        float x = HUD_WIDTH - 220;
        float y = HUD_HEIGHT - 20;
        font.draw(batch, "DEBUG (F1)", x, y);
        font.draw(batch, "Pos: " + (int)boat.x + ", " + (int)boat.y, x, y - 20);
        font.draw(batch, "Vel: " + (int)boat.velocity.len(), x, y - 40);
        font.draw(batch, "Angle: " + (int)boat.angle, x, y - 60);
        font.draw(batch, "Raw Throttle: " + String.format("%.2f", inputController.getThrottleValue()), x, y - 80);
        font.draw(batch, "Raw Steer: " + String.format("%.2f", inputController.getSteeringValue()), x, y - 100);

        if (dock.slipZone != null) {
            font.draw(batch, "Target: " + (int)dock.slipZone.x + "," + (int)dock.slipZone.y + " (Angle: " + (int)dock.targetAngle + ")", x, y - 120);
            font.draw(batch, "In Zone: " + dock.slipZone.contains(boat.x, boat.y), x, y - 140);
            font.draw(batch, "Speed: " + (int)boat.velocity.len() + " / " + (int)dock.dockingMaxSpeed, x, y - 160);

            float angleDiff = Math.abs(boat.angle % 360 - dock.targetAngle);
            if (angleDiff > 180) angleDiff = 360 - angleDiff;
            font.draw(batch, "Angle Diff: " + (int)angleDiff + " / " + (int)dock.dockingAngleTolerance, x, y - 180);

            font.draw(batch, "Progress: " + (int)(dock.getDockingProgress() * 100) + "%", x, y - 200);
            font.draw(batch, "Fail Reason: " + dock.lastFailureReason, x, y - 220);
        }

        font.draw(batch, "Motor Vol: " + String.format("%.2f", soundManager.getMotorVolume()), x, y - 240);
        font.draw(batch, "Motor Pitch: " + String.format("%.2f", soundManager.getMotorPitch()), x, y - 260);

        font.draw(batch, "Upgrades: E" + boat.engineLevel + " S" + boat.steeringLevel + " H" + boat.hullLevel + " R" + boat.reverseLevel, x, y - 280);

        String boatTex = textureManager.getTextureStatus("boat_" + boat.profile.id);
        String boatAlpha = textureManager.getAlphaStatus("boat_" + boat.profile.id);
        String waterTex = textureManager.getTextureStatus("water_tile");
        String dockTex = textureManager.getTextureStatus("dock_plank");

        font.draw(batch, "Tex Mode: " + (USE_TEXTURES ? "ON" : "OFF"), x, y - 260);
        font.draw(batch, "Boat Tex: " + boatTex + " (" + boatAlpha + ")", x, y - 280);
        font.draw(batch, "Water Tex: " + waterTex, x, y - 300);
        font.draw(batch, "Dock Tex: " + dockTex, x, y - 320);

        font.draw(batch, "Control Mode: " + progressManager.getControlMode().toUpperCase(), x, y - 340);
        font.draw(batch, "Render Scale: " + String.format("%.2f", boat.profile.visualScale), x, y - 360);
        font.draw(batch, "Visual Size: " + (int)(boat.profile.length * boat.profile.visualScale) + "x" + (int)(boat.profile.width * boat.profile.visualScale), x, y - 380);
        font.draw(batch, "Collision Size: " + (int)boat.profile.length + "x" + (int)boat.profile.width, x, y - 400);
        font.draw(batch, "Cam Zoom: " + String.format("%.2f", worldCamera.zoom), x, y - 420);
        font.draw(batch, "Paused: " + (state == GameState.PAUSED), x, y - 440);
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
        textureManager.dispose();
    }
}
