package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ProgressManager {
    private static final String PREFS_NAME = "DockMasterPrefs";
    private static final String KEY_UNLOCKED = "unlockedLevel";
    private static final String KEY_CASH = "playerCash";
    private static final String KEY_BOAT_VAL = "boatValue";
    private static final String KEY_SELECTED_BOAT = "selectedBoatId";

    private Preferences prefs;
    private int unlockedLevel; // 0-based index
    private int playerCash;
    private long boatValue;
    private String selectedBoatId;

    public ProgressManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        unlockedLevel = prefs.getInteger(KEY_UNLOCKED, 0);
        playerCash = prefs.getInteger(KEY_CASH, 0);
        boatValue = prefs.getLong(KEY_BOAT_VAL, 10000);
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

    public long getBoatValue() {
        return boatValue;
    }

    public void updateBoatValue(long newValue) {
        boatValue = newValue;
        prefs.putLong(KEY_BOAT_VAL, boatValue);
        prefs.flush();
    }
}
