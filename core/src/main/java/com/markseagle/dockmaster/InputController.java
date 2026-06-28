package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
    public float throttleValue = 0f; // -1.0 (REV) to 1.0 (FWD)
    public float steeringValue = 0f; // -1.0 (LEFT) to 1.0 (RIGHT)
    private float rawThrottleValue = 0f;
    private float rawSteeringValue = 0f;
    private float stickyThrottleValue = 0f;

    // Control Tuning Constants
    private static final float THROTTLE_DEADZONE = 0.08f;
    private static final float STEERING_DEADZONE = 0.08f;
    private static final float THROTTLE_CURVE = 1.4f;
    private static final float STEERING_CURVE = 1.2f;
    private static final float THROTTLE_SMOOTHING = 8f;
    private static final float STEERING_SMOOTHING = 12f;

    private int throttlePointer = -1;
    private int steeringPointer = -1;

    public boolean nextPressed, retryPressed, levelSelectPressed, titlePressed, startPressed, boatSelectPressed, garagePressed, repairPressed, settingsPressed, trainingPressed, skipPressed, pausePressed;
    public boolean controlModeToggled, throttleModeToggled, controlFeelToggled;
    public boolean upgradeEnginePressed, upgradeSteeringPressed, upgradeHullPressed, upgradeReversePressed;
    public boolean soundToggled, vibrateToggled;
    public boolean debugToggled = false;

    private final Rectangle btnLeft, btnRight, btnFwd, btnRev;
    private final Rectangle btnNext, btnRetry, btnPause;
    private final Rectangle btnStart, btnBack, btnLevelSelect, btnBoatSelect, btnGarage, btnSettings, btnTraining;
    private final Rectangle btnRepair, btnSkip, btnControls, btnThrottleMode, btnControlFeel;
    private final Rectangle btnUpgradeEngine, btnUpgradeSteering, btnUpgradeHull, btnUpgradeReverse;
    private final Rectangle btnLevelSelectResults, btnTitleResults, btnGarageResults;
    private final Rectangle btnSound, btnVibrate;
    private final Rectangle btnResumePause, btnRetryPause, btnLevelSelectPause, btnTitlePause, btnGaragePause;

    // Boat Controls Areas
    private final float steeringCenterX = 145f;
    private final float steeringCenterY = 115f;
    private final float steeringRadius = 85f;
    private final Rectangle throttleRect = new Rectangle(650, 60, 70, 210);

    // Grid buttons
    public final List<Rectangle> levelButtons = new ArrayList<>();
    public final List<Rectangle> boatButtons = new ArrayList<>();
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
        btnStart = new Rectangle(hudWidth / 2 - 100, 360, 200, 45);
        btnTraining = new Rectangle(hudWidth / 2 - 100, 310, 200, 45);
        btnLevelSelect = new Rectangle(hudWidth / 2 - 100, 260, 200, 45);
        btnBoatSelect = new Rectangle(hudWidth / 2 - 100, 210, 200, 45);
        btnGarage = new Rectangle(hudWidth / 2 - 100, 160, 200, 45);
        btnSettings = new Rectangle(hudWidth / 2 - 100, 110, 200, 45);

        // Training UI
        btnSkip = new Rectangle(hudWidth - 110, hudHeight - 60, 100, 40);

        // Settings screen
        btnSound = new Rectangle(hudWidth / 2 - 100, 300, 200, 60);
        btnVibrate = new Rectangle(hudWidth / 2 - 100, 200, 200, 60);
        btnControls = new Rectangle(hudWidth / 2 - 100, 140, 200, 45);
        btnThrottleMode = new Rectangle(hudWidth / 2 - 100, 90, 200, 45);
        btnControlFeel = new Rectangle(hudWidth / 2 - 100, 40, 200, 45);

        // Garage screen
        btnRepair = new Rectangle(hudWidth / 2 - 100, 150, 200, 60);
        float upW = 160;
        float upH = 80;
        float upMargin = 20;
        btnUpgradeEngine = new Rectangle(hudWidth / 2 - upW - upMargin, 380, upW, upH);
        btnUpgradeSteering = new Rectangle(hudWidth / 2 + upMargin, 380, upW, upH);
        btnUpgradeHull = new Rectangle(hudWidth / 2 - upW - upMargin, 280, upW, upH);
        btnUpgradeReverse = new Rectangle(hudWidth / 2 + upMargin, 280, upW, upH);

        // Results screen
        btnRetry = new Rectangle(hudWidth / 2 - 110, 240, 100, 45);
        btnNext = new Rectangle(hudWidth / 2 + 10, 240, 100, 45);
        btnLevelSelectResults = new Rectangle(hudWidth / 2 - 110, 185, 220, 45);
        btnGarageResults = new Rectangle(hudWidth / 2 - 110, 130, 220, 45);
        btnTitleResults = new Rectangle(hudWidth / 2 - 110, 75, 220, 45);

        // Pause menu
        btnPause = new Rectangle(hudWidth - 70, hudHeight - 60, 60, 50);
        float pw = 200;
        float ph = 50;
        btnResumePause = new Rectangle(hudWidth / 2 - pw/2, 350, pw, ph);
        btnRetryPause = new Rectangle(hudWidth / 2 - pw/2, 290, pw, ph);
        btnLevelSelectPause = new Rectangle(hudWidth / 2 - pw/2, 230, pw, ph);
        btnGaragePause = new Rectangle(hudWidth / 2 - pw/2, 170, pw, ph);
        btnTitlePause = new Rectangle(hudWidth / 2 - pw/2, 110, pw, ph);

        // Back button
        btnBack = new Rectangle(20, hudHeight - 80, 100, 50);

        // Level select grid (3x4)
        for (int i = 0; i < 12; i++) {
            float x = 40 + (i % 3) * 250;
            float y = hudHeight - 140 - (i / 3) * 110;
            levelButtons.add(new Rectangle(x, y, 230, 90));
        }

        // Boat select grid
        for (int i = 0; i < 4; i++) {
            float x = 100 + (i % 2) * 350;
            float y = hudHeight - 200 - (i / 2) * 120;
            boatButtons.add(new Rectangle(x, y, 300, 100));
        }
    }

    public void update(float delta, Viewport hudViewport, DockMasterGame.GameState state, boolean boatTotaled, String controlMode, String throttleMode) {
        nextPressed = false;
        retryPressed = false;
        levelSelectPressed = false;
        boatSelectPressed = false;
        garagePressed = false;
        trainingPressed = false;
        skipPressed = false;
        pausePressed = false;
        controlModeToggled = false;
        throttleModeToggled = false;
        controlFeelToggled = false;
        repairPressed = false;
        upgradeEnginePressed = false;
        upgradeSteeringPressed = false;
        upgradeHullPressed = false;
        upgradeReversePressed = false;
        settingsPressed = false;
        soundToggled = false;
        vibrateToggled = false;
        titlePressed = false;
        startPressed = false;
        selectedLevelIndex = -1;
        selectedBoatIndex = -1;

        // --- Keyboard (Always Active, overrides target) ---
        boolean kFwd = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean kRev = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean kLeft = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean kRight = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        float targetThrottle = 0f;
        float targetSteering = 0f;

        if (kFwd && !kRev) targetThrottle = 1.0f;
        else if (kRev && !kFwd) targetThrottle = -1.0f;

        if (kRight && !kLeft) targetSteering = 1.0f;
        else if (kLeft && !kRight) targetSteering = -1.0f;

        // Keyboard One-shots
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (state == DockMasterGame.GameState.GARAGE) repairPressed = true;
            else if (!boatTotaled) retryPressed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) && !boatTotaled) nextPressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (state == DockMasterGame.GameState.TITLE) startPressed = true;
            if (state == DockMasterGame.GameState.GARAGE) repairPressed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) stickyThrottleValue = 0; // Emergency Neutral
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) levelSelectPressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) garagePressed = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (state == DockMasterGame.GameState.TITLE) boatSelectPressed = true;
            else titlePressed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if (state == DockMasterGame.GameState.PLAYING || state == DockMasterGame.GameState.TUTORIAL || state == DockMasterGame.GameState.PAUSED) {
                pausePressed = true;
            } else {
                titlePressed = true;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugToggled = !debugToggled;

        // --- Touch ---
        boolean boatMode = "boat".equals(controlMode);
        boolean isSticky = "sticky".equals(throttleMode);
        float touchThrottle = 0;
        float touchSteering = 0;
        boolean throttleTouched = false;
        boolean steeringTouched = false;

        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                Vector2 touch = new Vector2(Gdx.input.getX(i), Gdx.input.getY(i));
                hudViewport.unproject(touch);

                if (state == DockMasterGame.GameState.PLAYING || state == DockMasterGame.GameState.TUTORIAL) {
                    if (boatMode) {
                        // Steering area (padded)
                        if (Vector2.dst(touch.x, touch.y, steeringCenterX, steeringCenterY) < steeringRadius + 50) {
                            touchSteering = com.badlogic.gdx.math.MathUtils.clamp((touch.x - steeringCenterX) / steeringRadius, -1f, 1f);
                            steeringTouched = true;
                        }
                        // Throttle area (padded)
                        if (touch.x > throttleRect.x - 40 && touch.x < throttleRect.x + throttleRect.width + 40 &&
                            touch.y > throttleRect.y - 40 && touch.y < throttleRect.y + throttleRect.height + 40) {
                            float centerY = throttleRect.y + throttleRect.height / 2;
                            touchThrottle = com.badlogic.gdx.math.MathUtils.clamp((touch.y - centerY) / (throttleRect.height / 2), -1f, 1f);

                            // Neutral Snap for Sticky
                            if (isSticky && Math.abs(touchThrottle) < 0.12f) touchThrottle = 0;

                            stickyThrottleValue = touchThrottle;
                            throttleTouched = true;
                        }
                    } else {
                        // Button mode
                        if (btnLeft.contains(touch)) { touchSteering = -1.0f; steeringTouched = true; }
                        if (btnRight.contains(touch)) { touchSteering = 1.0f; steeringTouched = true; }
                        if (btnFwd.contains(touch)) { touchThrottle = 1.0f; throttleTouched = true; }
                        if (btnRev.contains(touch)) { touchThrottle = -1.0f; throttleTouched = true; }
                    }

                    if (Gdx.input.justTouched()) {
                        if (btnPause.contains(touch)) pausePressed = true;
                        if (state == DockMasterGame.GameState.TUTORIAL && btnSkip.contains(touch)) skipPressed = true;
                    }
                } else if (Gdx.input.justTouched()) {
                    // Menu clicks
                    if (state == DockMasterGame.GameState.TITLE) {
                        if (btnStart.contains(touch)) startPressed = true;
                        if (btnTraining.contains(touch)) trainingPressed = true;
                        if (btnLevelSelect.contains(touch)) levelSelectPressed = true;
                        if (btnBoatSelect.contains(touch)) boatSelectPressed = true;
                        if (btnGarage.contains(touch)) garagePressed = true;
                        if (btnSettings.contains(touch)) settingsPressed = true;
                    } else if (state == DockMasterGame.GameState.PAUSED) {
                        if (btnResumePause.contains(touch) || btnPause.contains(touch)) pausePressed = true;
                        if (btnRetryPause.contains(touch)) retryPressed = true;
                        if (btnLevelSelectPause.contains(touch)) levelSelectPressed = true;
                        if (btnGaragePause.contains(touch)) garagePressed = true;
                        if (btnTitlePause.contains(touch)) titlePressed = true;
                    } else if (state == DockMasterGame.GameState.SETTINGS) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        if (btnSound.contains(touch)) soundToggled = true;
                        if (btnVibrate.contains(touch)) vibrateToggled = true;
                        if (btnControls.contains(touch)) controlModeToggled = true;
                        if (btnThrottleMode.contains(touch)) throttleModeToggled = true;
                        if (btnControlFeel.contains(touch)) controlFeelToggled = true;
                    } else if (state == DockMasterGame.GameState.GARAGE) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        if (btnRepair.contains(touch)) repairPressed = true;
                        if (btnUpgradeEngine.contains(touch)) upgradeEnginePressed = true;
                        if (btnUpgradeSteering.contains(touch)) upgradeSteeringPressed = true;
                        if (btnUpgradeHull.contains(touch)) upgradeHullPressed = true;
                        if (btnUpgradeReverse.contains(touch)) upgradeReversePressed = true;
                    } else if (state == DockMasterGame.GameState.LEVEL_SELECT) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        for (int j = 0; j < levelButtons.size(); j++) if (levelButtons.get(j).contains(touch)) selectedLevelIndex = j;
                    } else if (state == DockMasterGame.GameState.BOAT_SELECT) {
                        if (btnBack.contains(touch)) titlePressed = true;
                        for (int j = 0; j < boatButtons.size(); j++) if (boatButtons.get(j).contains(touch)) selectedBoatIndex = j;
                    } else if (state == DockMasterGame.GameState.DOCKED || state == DockMasterGame.GameState.FAILED) {
                        if (btnRetry.contains(touch) && !boatTotaled) retryPressed = true;
                        if (btnNext.contains(touch) && !boatTotaled) nextPressed = true;
                        if (btnLevelSelectResults.contains(touch)) levelSelectPressed = true;
                        if (btnGarageResults.contains(touch)) garagePressed = true;
                        if (btnTitleResults.contains(touch)) titlePressed = true;
                    }
                }
            }
        }

        // Combine Keyboard + Touch
        // Keyboard overrides touch if active
        if (Math.abs(targetThrottle) < 0.01f) {
            if (boatMode && isSticky) targetThrottle = stickyThrottleValue;
            else targetThrottle = touchThrottle;
        }
        if (Math.abs(targetSteering) < 0.01f) targetSteering = touchSteering;

        // Deadzones
        if (Math.abs(targetThrottle) < THROTTLE_DEADZONE) targetThrottle = 0;
        if (Math.abs(targetSteering) < STEERING_DEADZONE) targetSteering = 0;

        // Curves
        rawThrottleValue = Math.signum(targetThrottle) * (float)Math.pow(Math.abs(targetThrottle), THROTTLE_CURVE);
        rawSteeringValue = Math.signum(targetSteering) * (float)Math.pow(Math.abs(targetSteering), STEERING_CURVE);

        // Smoothing
        float tSmooth = (kFwd || kRev || !boatMode) ? 100f : THROTTLE_SMOOTHING; // Snappy for keys/buttons
        float sSmooth = (kLeft || kRight || !boatMode) ? 100f : STEERING_SMOOTHING;

        throttleValue += (rawThrottleValue - throttleValue) * Math.min(1f, delta * tSmooth);
        steeringValue += (rawSteeringValue - steeringValue) * Math.min(1f, delta * sSmooth);

        // Sync booleans
        forward = throttleValue > 0.1f;
        reverse = throttleValue < -0.1f;
        left = steeringValue < -0.1f;
        right = steeringValue > 0.1f;
    }

    public void drawShapes(ShapeRenderer shape, DockMasterGame.GameState state, String currentBoatId, BoatCatalog bc, boolean boatTotaled, String controlMode) {
        if (state == DockMasterGame.GameState.PLAYING || state == DockMasterGame.GameState.TUTORIAL) {
            if (controlMode.equals("boat")) {
                // --- Steering Wheel / Pad ---
                shape.setColor(0.15f, 0.15f, 0.2f, 0.5f);
                shape.circle(steeringCenterX, steeringCenterY, steeringRadius);
                shape.setColor(1, 1, 1, 0.3f);
                shape.circle(steeringCenterX, steeringCenterY, steeringRadius, 32);

                // Indicator line
                shape.setColor(Color.WHITE);
                float steerAngle = -steeringValue * 45f;
                float lx = (float)Math.sin(Math.toRadians(steerAngle)) * (steeringRadius - 10);
                float ly = (float)Math.cos(Math.toRadians(steerAngle)) * (steeringRadius - 10);
                shape.line(steeringCenterX, steeringCenterY, steeringCenterX + lx, steeringCenterY + ly);

                // Steering knob
                float knobX = steeringCenterX + steeringValue * (steeringRadius - 20);
                shape.setColor(0.3f, 0.5f, 0.8f, 0.8f);
                shape.circle(knobX, steeringCenterY, 25);

                // --- Throttle Lever ---
                shape.setColor(0.15f, 0.15f, 0.2f, 0.5f);
                shape.rect(throttleRect.x, throttleRect.y, throttleRect.width, throttleRect.height);

                // Ranges
                shape.setColor(0, 1, 0, 0.05f);
                shape.rect(throttleRect.x, throttleRect.y + throttleRect.height/2, throttleRect.width, throttleRect.height/2);
                shape.setColor(1, 0, 0, 0.05f);
                shape.rect(throttleRect.x, throttleRect.y, throttleRect.width, throttleRect.height/2);

                shape.setColor(1, 1, 1, 0.3f);
                shape.rect(throttleRect.x, throttleRect.y, throttleRect.width, throttleRect.height);
                shape.rect(throttleRect.x, throttleRect.y + throttleRect.height / 2 - 1, throttleRect.width, 2);

                // Neutral Snap Highlight
                if (Math.abs(throttleValue) < 0.05f) {
                    shape.setColor(1, 1, 1, 0.2f);
                    shape.rect(throttleRect.x + 2, throttleRect.y + throttleRect.height / 2 - 10, throttleRect.width - 4, 20);
                }

                // Throttle knob
                float knobY = (throttleRect.y + throttleRect.height / 2) + (throttleValue * (throttleRect.height / 2 - 20));
                shape.setColor(throttleValue > 0.05f ? Color.LIME : (throttleValue < -0.05f ? Color.ORANGE : Color.WHITE));
                shape.rect(throttleRect.x + 5, knobY - 15, throttleRect.width - 10, 30);
            } else {
                drawButton(shape, btnLeft, left);
                drawButton(shape, btnRight, right);
                drawButton(shape, btnFwd, forward);
                drawButton(shape, btnRev, reverse);
            }
            drawButton(shape, btnPause, false);
            if (state == DockMasterGame.GameState.TUTORIAL) {
                drawButton(shape, btnSkip, false);
            }
        } else if (state == DockMasterGame.GameState.PAUSED) {
            drawButton(shape, btnResumePause, false);
            drawButton(shape, btnRetryPause, false);
            drawButton(shape, btnLevelSelectPause, false);
            drawButton(shape, btnGaragePause, false);
            drawButton(shape, btnTitlePause, false);
        } else if (state == DockMasterGame.GameState.TITLE) {
            drawButton(shape, btnStart, false);
            drawButton(shape, btnTraining, false);
            drawButton(shape, btnLevelSelect, false);
            drawButton(shape, btnBoatSelect, false);
            drawButton(shape, btnGarage, false);
            drawButton(shape, btnSettings, false);
        } else if (state == DockMasterGame.GameState.SETTINGS) {
            drawButton(shape, btnBack, false);
            drawButton(shape, btnSound, false);
            drawButton(shape, btnVibrate, false);
            drawButton(shape, btnControls, false);
            drawButton(shape, btnThrottleMode, false);
            drawButton(shape, btnControlFeel, false);
        } else if (state == DockMasterGame.GameState.GARAGE) {
            drawButton(shape, btnBack, false);
            drawButton(shape, btnRepair, false);
            drawButton(shape, btnUpgradeEngine, false);
            drawButton(shape, btnUpgradeSteering, false);
            drawButton(shape, btnUpgradeHull, false);
            drawButton(shape, btnUpgradeReverse, false);
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
            if (!boatTotaled) {
                drawButton(shape, btnRetry, false);
                drawButton(shape, btnNext, false);
            }
            drawButton(shape, btnLevelSelectResults, false);
            drawButton(shape, btnGarageResults, false);
            drawButton(shape, btnTitleResults, false);
        }
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font, DockMasterGame.GameState state, LevelManager lm, ProgressManager pm, BoatCatalog bc, boolean boatTotaled, TextureManager tm) {
        font.setColor(Color.WHITE);
        String controlMode = pm.getControlMode();
        String throttleMode = pm.getThrottleMode();
        if (state == DockMasterGame.GameState.PLAYING || state == DockMasterGame.GameState.TUTORIAL) {
            if (controlMode.equals("boat")) {
                font.draw(batch, "STEER " + (int)(Math.abs(steeringValue)*100) + "% " + (steeringValue < 0 ? "L" : "R"), steeringCenterX - 45, steeringCenterY + steeringRadius + 25);

                String tLabel = "NEUTRAL";
                if (throttleValue > 0.05f) tLabel = "FWD " + (int)(throttleValue*100) + "%";
                else if (throttleValue < -0.05f) tLabel = "REV " + (int)(Math.abs(throttleValue)*100) + "%";
                font.draw(batch, tLabel, throttleRect.x - 30, throttleRect.y + throttleRect.height + 25);

                font.draw(batch, "N", throttleRect.x - 20, throttleRect.y + throttleRect.height / 2 + 5);
                font.getData().setScale(0.7f);
                font.draw(batch, "MODE: " + throttleMode.toUpperCase(), throttleRect.x - 35, throttleRect.y - 30);
                font.getData().setScale(1.2f);
            } else {
                drawCenteredLabel(batch, font, "LEFT", btnLeft);
                drawCenteredLabel(batch, font, "RIGHT", btnRight);
                drawCenteredLabel(batch, font, "FWD", btnFwd);
                drawCenteredLabel(batch, font, "REV", btnRev);
            }
            drawCenteredLabel(batch, font, "MENU", btnPause);
            if (state == DockMasterGame.GameState.TUTORIAL) {
                drawCenteredLabel(batch, font, "SKIP", btnSkip);
            }
        } else if (state == DockMasterGame.GameState.PAUSED) {
            drawCenteredLabel(batch, font, "RESUME", btnResumePause);
            drawCenteredLabel(batch, font, "RETRY", btnRetryPause);
            drawCenteredLabel(batch, font, "LEVEL SELECT", btnLevelSelectPause);
            drawCenteredLabel(batch, font, "GARAGE", btnGaragePause);
            drawCenteredLabel(batch, font, "TITLE", btnTitlePause);
        } else if (state == DockMasterGame.GameState.TITLE) {
            drawCenteredLabel(batch, font, "START", btnStart);
            drawCenteredLabel(batch, font, "TRAINING", btnTraining);
            drawCenteredLabel(batch, font, "LEVEL SELECT", btnLevelSelect);
            drawCenteredLabel(batch, font, "BOAT SELECT", btnBoatSelect);
            drawCenteredLabel(batch, font, "GARAGE", btnGarage);
            drawCenteredLabel(batch, font, "SETTINGS", btnSettings);
        } else if (state == DockMasterGame.GameState.SETTINGS) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            drawCenteredLabel(batch, font, "SOUND: " + (pm.isSoundEnabled() ? "ON" : "OFF"), btnSound);
            drawCenteredLabel(batch, font, "VIBRATE: " + (pm.isVibrationEnabled() ? "ON" : "OFF"), btnVibrate);
            drawCenteredLabel(batch, font, "CONTROLS: " + pm.getControlMode().toUpperCase(), btnControls);
            drawCenteredLabel(batch, font, "THROTTLE: " + pm.getThrottleMode().toUpperCase(), btnThrottleMode);
            drawCenteredLabel(batch, font, "FEEL: " + pm.getControlFeelPreset().toUpperCase(), btnControlFeel);
        } else if (state == DockMasterGame.GameState.GARAGE) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            drawCenteredLabel(batch, font, "REPAIR", btnRepair);

            BoatDefinition profile = bc.getBoatById(pm.getSelectedBoatId());
            drawUpgradeLabel(batch, font, "ENGINE", btnUpgradeEngine, pm.getUpgradeLevel(profile.id, "engine"));
            drawUpgradeLabel(batch, font, "STEERING", btnUpgradeSteering, pm.getUpgradeLevel(profile.id, "steering"));
            drawUpgradeLabel(batch, font, "HULL", btnUpgradeHull, pm.getUpgradeLevel(profile.id, "hull"));
            drawUpgradeLabel(batch, font, "REVERSE", btnUpgradeReverse, pm.getUpgradeLevel(profile.id, "reverse"));
        } else if (state == DockMasterGame.GameState.LEVEL_SELECT) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            List<LevelDefinition> levels = lm.getLevels();
            for (int i = 0; i < levels.size(); i++) {
                if (i >= levelButtons.size()) break;
                Rectangle r = levelButtons.get(i);
                LevelDefinition lvl = levels.get(i);
                boolean unlocked = i <= pm.getUnlockedLevel();

                // Card Details
                font.setColor(unlocked ? Color.WHITE : Color.GRAY);
                font.getData().setScale(0.7f);
                font.draw(batch, (i+1) + ". " + lvl.levelName, r.x + 10, r.y + 75);
                font.getData().setScale(0.6f);
                font.draw(batch, lvl.destinationName, r.x + 10, r.y + 55);
                font.draw(batch, "Payout: $" + lvl.basePayout, r.x + 10, r.y + 40);

                int stars = pm.getBestStars(i);
                font.setColor(Color.YELLOW);

                Texture starFilled = tm.getTexture("ui_star_filled");
                if (starFilled != null) {
                    for (int s = 0; s < 3; s++) {
                        if (s < stars) batch.draw(starFilled, r.x + 140 + s * 22, r.y + 25, 20, 20);
                    }
                } else {
                    font.draw(batch, getStarString(stars), r.x + 140, r.y + 40);
                }

                if (!unlocked) {
                    font.setColor(Color.RED);
                    font.draw(batch, "LOCKED", r.x + 140, r.y + 75);
                }
                font.getData().setScale(1.2f);
            }
        } else if (state == DockMasterGame.GameState.BOAT_SELECT) {
            drawCenteredLabel(batch, font, "BACK", btnBack);
            List<BoatDefinition> boats = bc.getBoats();
            for (int i = 0; i < boats.size(); i++) {
                Rectangle r = boatButtons.get(i);
                BoatDefinition b = boats.get(i);
                boolean selected = b.id.equals(pm.getSelectedBoatId());

                font.setColor(selected ? Color.YELLOW : Color.WHITE);
                font.draw(batch, b.displayName, r.x + 15, r.y + 85);
                font.getData().setScale(0.8f);
                font.setColor(Color.WHITE);
                font.draw(batch, b.description, r.x + 15, r.y + 60);
                font.draw(batch, "Value: $" + b.value, r.x + 15, r.y + 40);

                if (b.displayName.contains("Soon")) {
                    font.setColor(Color.GRAY);
                    font.draw(batch, "COMING SOON", r.x + 180, r.y + 85);
                } else if (selected) {
                    font.setColor(Color.LIME);
                    font.draw(batch, "SELECTED", r.x + 200, r.y + 85);
                }
                font.getData().setScale(1.2f);
            }
        } else if (state == DockMasterGame.GameState.DOCKED || state == DockMasterGame.GameState.FAILED) {
            if (!boatTotaled) {
                drawCenteredLabel(batch, font, "RETRY", btnRetry);
                drawCenteredLabel(batch, font, "NEXT", btnNext);
            }
            drawCenteredLabel(batch, font, "LEVEL SELECT", btnLevelSelectResults);
            drawCenteredLabel(batch, font, "GARAGE", btnGarageResults);
            drawCenteredLabel(batch, font, "TITLE SCREEN", btnTitleResults);
        }
    }

    public String getStarString(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i < stars) sb.append("*");
            else sb.append("-");
        }
        return "[" + sb.toString() + "]";
    }

    public float getThrottleValue() { return throttleValue; }
    public float getSteeringValue() { return steeringValue; }

    public void resetAnalog() {
        throttleValue = 0;
        steeringValue = 0;
        rawThrottleValue = 0;
        rawSteeringValue = 0;
        stickyThrottleValue = 0;
    }

    private void drawUpgradeLabel(SpriteBatch batch, BitmapFont font, String title, Rectangle rect, int currentLevel) {
        font.setColor(Color.YELLOW);
        font.draw(batch, title, rect.x + 10, rect.y + rect.height - 10);
        font.setColor(Color.WHITE);
        font.getData().setScale(0.7f);
        font.draw(batch, "LVL: " + currentLevel, rect.x + 10, rect.y + rect.height - 35);
        if (currentLevel < 3) {
            font.draw(batch, "NEXT: $" + getUpgradeCost(currentLevel + 1), rect.x + 10, rect.y + rect.height - 55);
        } else {
            font.setColor(Color.LIME);
            font.draw(batch, "MAXED", rect.x + 10, rect.y + rect.height - 55);
        }
        font.getData().setScale(1.2f);
    }

    public static int getUpgradeCost(int nextLevel) {
        if (nextLevel == 1) return 750;
        if (nextLevel == 2) return 2000;
        if (nextLevel == 3) return 4500;
        return 0;
    }

    private void drawCenteredLabel(SpriteBatch batch, BitmapFont font, String text, Rectangle rect) {
        font.draw(batch, text, rect.x + rect.width / 2 - text.length() * 4, rect.y + rect.height / 2 + 5);
    }

    private void drawButton(ShapeRenderer shape, Rectangle rect, boolean active) {
        // Shadow
        shape.setColor(0, 0, 0, 0.4f);
        shape.rect(rect.x + 3, rect.y - 3, rect.width, rect.height);

        // Button Body
        if (active) {
            shape.setColor(0.3f, 0.5f, 0.8f, 0.9f); // Blueish when active
        } else {
            shape.setColor(0.15f, 0.15f, 0.2f, 0.7f); // Dark translucent
        }
        shape.rect(rect.x, rect.y, rect.width, rect.height);

        // Bevel/Border
        shape.setColor(1, 1, 1, 0.3f);
        shape.rect(rect.x, rect.y + rect.height - 2, rect.width, 2); // Top highlight
        shape.setColor(0, 0, 0, 0.3f);
        shape.rect(rect.x, rect.y, rect.width, 2); // Bottom shadow
    }
}
