/*
 * CollisionController.java
 *
 * This controller implements basic collision detection as described in
 * the instructions.  All objects in this game are treated as circles,
 * and a collision happens when circles intersect.
 *
 * This controller is EXTREMELY ineffecient.  To improve its performance,
 * you will need to use collision cells, as described in the instructions.
 * You should not need to modify any method other than the constructor
 * and processCollisions.  However, you will need to add your own methods.
 *
 * This is the only file that you need to modify as the first part of
 * the lab. 
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.optimize.entity.*;

/**
 * Controller implementing simple game physics.
 */
public class CollisionController {
	// TODO: don't allow the ship to go offscreen vertically.
	// These cannot be modified after the controller is constructed.
	// If these change, make a new constructor.
	/** Width of the collision geometry */
	private float width;
	/** Height of the collision geometry */
	private float height;
	
	// Cache objects for collision calculations
	private Vector2 temp1;
	private Vector2 temp2;
	
	/// ACCESSORS
	
	/**
	 * Returns width of the game window (necessary to detect out of bounds)
	 *
	 * @return width of the game window
	 */
	public float getWidth() {
		return width;
	}
	
	/**
	 * Returns height of the game window (necessary to detect out of bounds)
	 *
	 * @return height of the game window
	 */
	public float getHeight() {
		return height;
	}

	//#region Initialization (MODIFY THIS CODE)
	
	/**
	 * Creates a CollisionController for the given screen dimensions.
	 *
	 * @param width   Width of the screen 
	 * @param height  Height of the screen 
	 */
	public CollisionController(float width, float height) {
		this.width = width;
		this.height = height;
		
		// Initialize cache objects
		temp1 = new Vector2();
		temp2 = new Vector2();
	}
	
	/**
	 * This is the main (incredibly unoptimized) collision detetection method.
	 *
	 * @param objects List of live objects to check 
	 * @param offset  Offset of the box and bump 
	 */
	public void processCollisions(Array<GameObject> objects, int offset) {
		// Find which object is the player
		Ship player = null;
		for (GameObject o : objects) {
			if (o.getType() == GameObject.ObjectType.SHIP) {
				player = (Ship) o;
				break;
			}
		}

		// Process player bounds
		handleBounds(player);

		// For each wood object, check for collisions with the player
		for (GameObject o : objects) {
			if (o.getType() == GameObject.ObjectType.WOOD) {
				handleCollision(player, (Wood)o);
			}
		}
	}

	private void handleCollision(Ship player, Wood wood) {
		if (player.isDestroyed() || wood.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(wood.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() + wood.getRadius()) {
			return;
		}

		// Destroy wood
		wood.setDestroyed(true);
	}

	/**
	 * Check a bullet for being out-of-bounds.
	 *
	 * @param sh Ship to check 
	 */
	private void handleBounds(Ship sh) {
		// Do not let the ship go off screen.
		if (sh.getX() <= sh.getRadius()) {
			sh.setX(sh.getRadius());
		} else if (sh.getX() >= getWidth() - sh.getRadius()) {
			sh.setX(getWidth() - sh.getRadius());
		}
	}

	//#endregion
}