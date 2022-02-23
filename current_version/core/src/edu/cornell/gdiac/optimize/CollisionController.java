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
 *
 * This is a very inefficient physics engine.  Part of this lab is determining
 * how to make it more efficient.
 */
public class CollisionController {

	// 'Bounciness' constants
	/** Restitution for colliding with the (hard coded) box */
	protected static final float BOX_COEFF_REST   = 0.95f;
	/** Restitution for colliding with the (hard coded) bump */
	protected static final float BUMP_COEFF_REST  = 1.95f;
	/** Dampening factor when colliding with floor or shell */
	protected static final float DAMPENING_FACTOR = 0.95f;
	
	// Geometry of the background image
	/** (Scaled) distance of the floor ledge from bottom */
	protected static final float BOTTOM_OFFSET    = 0.075f;
	/** (Scaled) position of the box center */
	protected static final float BOX_X_POSITION   = 0.141f;
	/** (Scaled) position of half the box width */
	protected static final float BOX_HALF_WIDTH   = 0.133f;
	/** (Scaled) position of the box height from bottom of screen */
	protected static final float BOX_FULL_HEIGHT  = 0.2f;
	/** (Scaled) position of the bump center */
	protected static final float BUMP_X_POSITION  = 0.734f;
	/** (Scaled) position of the bump radius */
	protected static final float BUMP_RADIUS      = 0.11f;
	
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
	
	/**
	 * Returns the height of the floor ledge.
	 *
	 * The floor ledge supports the player ship, and is what all of the shells
	 * bounce off of.  It is raised slightly higher than the bottom of the screen.
	 *
	 * @return the height of the floor ledge.
	 */
	public float getFloorLedge() {
		return BOTTOM_OFFSET*height;
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

	private void handleCollision(Ship player, Wood o) {
		//TODO: code for collecting the wood: marking it as destroyed, increasing player health
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