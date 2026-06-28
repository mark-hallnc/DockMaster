package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ProgressManager {
    private static final String PREFS_NAME = "DockMasterPrefs";
    private static final String KEY_UNLOCKED = "unlockedLevel";
    private static final String KEY_CASH = "playerCash";
    private static final String KEY_SELECTED_BOAT = "selectedBoatId";
    private static final String KEY_SOUND = "soundEnabled";
    private static final String KEY_VIBRATE = "vibrationEnabled";
    private static final String KEY_TUTORIAL = "tutorialCompleted";
    private static final String KEY_CONTROL_MODE = "controlMode"; // "buttons" or "boat"

    // Per-boat keys will be like "damage_skiff", "value_skiff"
    private static final String PREFIX_DAMAGE = "damage_";
    private static final String PREFIX_VALUE = "value_";

    // Per-level keys will be like "stars_0"
    private static final String PREFIX_STARS = "stars_";

    // Upgrade keys
    private static final String PREFIX_UPGRADE = "upgrade_";

    private Preferences prefs;
    private int unlockedLevel; // 0-based index
    private int playerCash;
    private String selectedBoatId;
    private boolean soundEnabled;
    private boolean vibrationEnabled;
    private boolean tutorialCompleted;
    private String controlMode;

    public ProgressManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        unlockedLevel = prefs.getInteger(KEY_UNLOCKED, 0);
        playerCash = prefs.getInteger(KEY_CASH, 0);
        selectedBoatId = prefs.getString(KEY_SELECTED_BOAT, "skiff");
        soundEnabled = prefs.getBoolean(KEY_SOUND, true);
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATE, true);
        tutorialCompleted = prefs.getBoolean(KEY_TUTORIAL, false);
        controlMode = prefs.getString(KEY_CONTROL_MODE, "buttons");
    }

    public String getSelectedBoatId() {
        return selectedBoatId;
    }

    public void setSelectedBoatId(String id) {
        selectedBoatId = id;
        prefs.putString(KEY_SELECTED_BOAT, id);
        prefs.flush();
    }

    public int getUnlockedLevel() {
        return unlockedLevel;
    }

    public void unlockNextLevel(int completedLevelIndex) {
        if (completedLevelIndex >= unlockedLevel) {
            unlockedLevel = completedLevelIndex + 1;
            prefs.putInteger(KEY_UNLOCKED, unlockedLevel);
            prefs.flush();
        }
    }

    public int getPlayerCash() {
        return playerCash;
    }

    public void addCash(int amount) {
        playerCash += amount;
        prefs.putInteger(KEY_CASH, playerCash);
        prefs.flush();
    }

    public void spendCash(int amount) {
        playerCash -= amount;
        if (playerCash < 0) playerCash = 0;
        prefs.putInteger(KEY_CASH, playerCash);
        prefs.flush();
    }

    public float getBoatDamage(String boatId) {
        return prefs.getFloat(PREFIX_DAMAGE + boatId, 0f);
    }

    public void setBoatDamage(String boatId, float damage) {
        prefs.putFloat(PREFIX_DAMAGE + boatId, damage);
        prefs.flush();
    }

    public long getBoatValue(String boatId, long defaultValue) {
        return prefs.getLong(PREFIX_VALUE + boatId, defaultValue);
    }

    public void setBoatValue(String boatId, long value) {
        prefs.putLong(PREFIX_VALUE + boatId, value);
        prefs.flush();
    }

    public int getBestStars(int levelIndex) {
        return prefs.getInteger(PREFIX_STARS + levelIndex, 0);
    }

    public void setBestStars(int levelIndex, int stars) {
        int currentBest = getBestStars(levelIndex);
        if (stars > currentBest) {
            prefs.putInteger(PREFIX_STARS + levelIndex, stars);
            prefs.flush();
        }
    }

    public int getTotalStars(int numLevels) {
        int total = 0;
        for (int i = 0; i < numLevels; i++) {
            total += getBestStars(i);
        }
        return total;
    }

    public int getUpgradeLevel(String boatId, String category) {
        return prefs.getInteger(PREFIX_UPGRADE + category + "_" + boatId, 0);
    }

    public void setUpgradeLevel(String boatId, String category, int level) {
        prefs.putInteger(PREFIX_UPGRADE + category + "_" + boatId, level);
        prefs.flush();
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        prefs.putBoolean(KEY_SOUND, enabled);
        prefs.flush();
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
        prefs.putBoolean(KEY_VIBRATE, enabled);
        prefs.flush();
    }

    public boolean isTutorialCompleted() {
        return tutorialCompleted;
    }

    public void setTutorialCompleted(boolean completed) {
        this.tutorialCompleted = completed;
        prefs.putBoolean(KEY_TUTORIAL, completed);
        prefs.flush();
    }

    public String getControlMode() {
        return controlMode;
    }

    public void setControlMode(String mode) {
        this.controlMode = mode;
        prefs.putString(KEY_CONTROL_MODE, mode);
        prefs.flush();
    }
}
