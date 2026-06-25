package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ProgressManager {
    private static final String PREFS_NAME = "DockMasterPrefs";
    private static final String KEY_UNLOCKED = "unlockedLevel";
    private static final String KEY_CASH = "playerCash";
    private static final String KEY_SELECTED_BOAT = "selectedBoatId";

    // Per-boat keys will be like "damage_skiff", "value_skiff"
    private static final String PREFIX_DAMAGE = "damage_";
    private static final String PREFIX_VALUE = "value_";

    // Per-level keys will be like "stars_0"
    private static final String PREFIX_STARS = "stars_";

    private Preferences prefs;
    private int unlockedLevel; // 0-based index
    private int playerCash;
    private String selectedBoatId;

    public ProgressManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        unlockedLevel = prefs.getInteger(KEY_UNLOCKED, 0);
        playerCash = prefs.getInteger(KEY_CASH, 0);
        selectedBoatId = prefs.getString(KEY_SELECTED_BOAT, "skiff");
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
}
