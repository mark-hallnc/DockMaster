package com.markseagle.dockmaster;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Boat {
    public float x, y;
    public float angle; // in degrees
    public Vector2 velocity = new Vector2();

    private final float THRUST = 240f;
    private final float REVERSE_THRUST = 120f;
    private final float DRAG = 0.985f;
    private final float BRAKE_DRAG = 0.94f;
    private final float TURN_SPEED = 140f;
    private final float DRIFT_FACTOR = 0.85f;

    public Boat(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 90; // Pointing up
    }

    public void update(float delta) {
        boolean forward = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean reverse = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean braking = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        float currentSpeed = velocity.len();

        // Turning - easier when moving
        float turnModifier = MathUtils.clamp(currentSpeed / 80f, 0.3f, 1.0f);
        if (left) angle += TURN_SPEED * turnModifier * delta;
        if (right) angle -= TURN_SPEED * turnModifier * delta;

        // Thrust
        if (forward) {
            velocity.add(
                MathUtils.cosDeg(angle) * THRUST * delta,
                MathUtils.sinDeg(angle) * THRUST * delta
            );
        }
        if (reverse) {
            velocity.add(
                -MathUtils.cosDeg(angle) * REVERSE_THRUST * delta,
                -MathUtils.sinDeg(angle) * REVERSE_THRUST * delta
            );
        }

        // Apply drag
        float dragToApply = braking ? BRAKE_DRAG : DRAG;
        velocity.scl((float) Math.pow(dragToApply, delta * 60f));

        // Sideways drift (dampen sideways velocity)
        if (velocity.len() > 0.1f) {
            Vector2 forwardDir = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
            float forwardVelocityMag = velocity.dot(forwardDir);
            Vector2 forwardVelocity = new Vector2(forwardDir).scl(forwardVelocityMag);
            Vector2 sidewaysVelocity = new Vector2(velocity).sub(forwardVelocity);

            // Dampen sideways more than forward
            sidewaysVelocity.scl((float) Math.pow(DRIFT_FACTOR, delta * 60f));
            velocity.set(forwardVelocity.add(sidewaysVelocity));
        }

        // Update position
        x += velocity.x * delta;
        y += velocity.y * delta;
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(Color.WHITE);

        shape.flush();
        shape.getTransformMatrix().idt().translate(x, y, 0).rotate(0, 0, 1, angle);
        shape.updateMatrices();

        float length = 40;
        float width = 20;

        // Hull
        shape.rect(-length / 2, -width / 2, length * 0.7f, width);
        // Bow
        shape.triangle(
            length * 0.2f, -width / 2,
            length * 0.2f, width / 2,
            length / 2 + 10, 0
        );

        shape.flush();
        shape.getTransformMatrix().idt();
        shape.updateMatrices();
    }

    public void reset(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 90;
        this.velocity.set(0, 0);
    }
}
