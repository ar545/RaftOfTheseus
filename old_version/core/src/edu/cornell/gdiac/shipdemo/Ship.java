/*
 * Ship.java
 *
 * This class tracks all of the state (position, velocity, rotation) of a 
 * single ship. In order to obey the separation of the model-view-controller 
 * pattern, controller specific code (such as reading the keyboard) is not 
 * present in this class.
 * 
 * Looking through this code you will notice certain optimizations. We want
 * to eliminate as many "new" statements as possible in the draw loop. In
 * game programming, it is considered bad form to have "new" statements in 
 * an update or a graphics loop if you can easily avoid it.  Each "new" is 
 * a potentially  expensive memory allocation. 
 *
 * To get around this, we have predeclared some Vector2 objects.  These are 
 * used by the draw method to position the objects on the screen. As we know
 * we will need that memory animation frame, it is better to have them
 * declared ahead of time (even though we are not taking state across frame
 * boundaries).
 *
 * Author: Walker M. White
 * Based on original GameX Ship Demo by Rama C. Hoetzlein, 2002
 * LibGDX version, 1/3/2015
 */
package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import edu.cornell.gdiac.util.*;

/**
 * Model class representing a player-controlled raft.
 * 
 * Note that the graphics resources in this class are static.  That
 * is because all ships share the same image file, and it would waste
 * memory to load the same image file for each ship.
 */
public class Ship extends GameObject {
    // Private constants to avoid use of "magic numbers"
    /** Amount to adjust forward movement from input */
    private static final float THRUST_FACTOR   = 0.4f;
    /** Amount to decay forward thrust over time */
    private static final float FORWARD_DAMPING = 0.9f;

	@Override
	public ObjectType getType() {
		return ObjectType.SHIP;
	}
	
	/**
	 * Creates a new ship at the given location with the given facing.
	 *
	 * @param x The initial x-coordinate of the center
	 * @param y The initial y-coordinate of the center
	 */
    public Ship(float x, float y) {
        // Set the position of this ship.
        this.position = new Vector2(x,y);

        // We start at rest.
        this.velocity = new Vector2();

		this.radius = 64;
    }

	/**
	 * Moves the ship by the specified amount.  
	 * 
	 * Forward is the amount to move forward, while turn is the angle to turn the ship 
	 * (used for the "banking" animation. This method performs no collision detection.  
	 * Collisions are resolved afterwards.
	 *
	 * @param forward	Amount to move forward
	 * @param turn		Amount to turn the ship
	 */
	public void move(float forward, float turn) {
		// Process the ship thrust.
		if (forward != 0.0f || turn != 0.0f) {
			// Thrust key pressed; increase the ship velocity.
			velocity.add(-turn * THRUST_FACTOR,forward * THRUST_FACTOR);
		} else {
			// Gradually slow the ship down
			velocity.scl(FORWARD_DAMPING);
		}

		// Move the ship position by the ship velocity
		position.add(velocity);
	}
}
