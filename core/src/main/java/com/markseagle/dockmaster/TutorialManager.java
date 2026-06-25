package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class TutorialManager {
    public enum Step {
        THRUST,
        STEER,
        COAST,
        REVERSE,
        ENTER_ZONE,
        DOCK,
        COMPLETE
    }

    private Step currentStep = Step.THRUST;
    private float stepTimer = 0;
    private boolean actionPerformed = false;

    public void update(float delta, Boat boat, InputController input, Dock dock) {
        stepTimer += delta;

        switch (currentStep) {
            case THRUST:
                if (input.forward) actionPerformed = true;
                if (actionPerformed && stepTimer > 2.0f) nextStep();
                break;
            case STEER:
                if (input.left || input.right) actionPerformed = true;
                if (actionPerformed && stepTimer > 2.0f) nextStep();
                break;
            case COAST:
                if (!input.forward && !input.reverse && boat.velocity.len() > 10 && boat.velocity.len() < 100) actionPerformed = true;
                if (actionPerformed && stepTimer > 2.0f) nextStep();
                break;
            case REVERSE:
                if (input.reverse) actionPerformed = true;
                if (actionPerformed && stepTimer > 2.0f) nextStep();
                break;
            case ENTER_ZONE:
                if (dock.slipZone.contains(boat.x, boat.y)) nextStep();
                break;
            case DOCK:
                if (dock.successfullyDocked) nextStep();
                break;
        }
    }

    public void nextStep() {
        Step[] steps = Step.values();
        int nextIndex = currentStep.ordinal() + 1;
        if (nextIndex < steps.length) {
            currentStep = steps[nextIndex];
            stepTimer = 0;
            actionPerformed = false;
        }
    }

    public void skip() {
        currentStep = Step.COMPLETE;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public String getPrompt() {
        boolean isDesktop = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop;
        switch (currentStep) {
            case THRUST:
                return isDesktop ? "Hold W or UP to move forward." : "Hold FWD to move forward.";
            case STEER:
                return isDesktop ? "Use A/D or LEFT/RIGHT to steer." : "Use LEFT and RIGHT to steer.";
            case COAST:
                return "Release throttle and let the boat coast.";
            case REVERSE:
                return isDesktop ? "Hold S or DOWN to slow down or back up." : "Hold REV to slow down or back up.";
            case ENTER_ZONE:
                return "Enter the yellow docking zone slowly.";
            case DOCK:
                return "Line up boat angle and hold steady to dock.";
            case COMPLETE:
                return "Training Complete! You're ready for Level 1.";
            default:
                return "";
        }
    }

    public void draw(SpriteBatch batch, BitmapFont font, float width, float height) {
        String prompt = getPrompt();
        if (prompt.isEmpty()) return;

        // Draw simple box
        font.setColor(Color.YELLOW);
        font.draw(batch, prompt, width / 2 - 180, height - 100);
        font.setColor(Color.WHITE);
    }

    public void reset() {
        currentStep = Step.THRUST;
        stepTimer = 0;
        actionPerformed = false;
    }
}
