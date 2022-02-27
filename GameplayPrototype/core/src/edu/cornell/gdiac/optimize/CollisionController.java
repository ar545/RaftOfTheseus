/*
 * CollisionController.java
 *
 * This controller implements basic collision detection as described in
 * the instructions.  All objects in this game are treated as circles,
 * and a collision happens when circles intersect.
 *
 * This controller is EXTREMELY inefficient.  To improve its performance,
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
	// These cannot be modified after the controller is constructed.
	// If these change, make a new constructor.
	/** Width of the collision geometry */
	private final float width;
	/** Height of the collision geometry */
	private final float height;
	
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

	// TODO: It is efficient right now to O(n) through all object to find the player and compute collision between
	// TODO: the player and others. However, since we are implementing enemies, we should follow the Lab3 design where
	// TODO: all possible collision between object are checked but only those meaningful are handled. -- Leo
	/**
	 * This is the main (incredibly unoptimized) collision detetection method.
	 *
	 * @param objects List of live objects to check 
	 * @param total_time  time used to calculate big wave in Technical prototype
	 */
	public void processCollisions(Array<GameObject> objects, int total_time) {
		// Find which object is the player (O(n))
		Ship player = null;
		for (GameObject o : objects) {
			if (o.getType() == GameObject.ObjectType.SHIP) {
				player = (Ship) o;
				break;
			}
		}

		// assert player != null, avoid null pointer exception
		if(player == null){
			return;
		}

		// Process player bounds
		handleBounds(player);

		// For each wood object, check for collisions with the player
		// For target, check for collision with palyer
		for (GameObject o : objects) {
			if (o.getType() == GameObject.ObjectType.WOOD) {
				handleCollision(player, (Wood)o);
			}else if(o.getType() == GameObject.ObjectType.TARGET){
				handleCollision(player, (Target)o);
			}else if(o.getType() == GameObject.ObjectType.OBSTACLE){
				handleCollision(player, (Obstacle)o);
			}
		}


	}

	/** handel the collision between ship and wood, wood get destroyed and ship get extended life
	 * @param player - the ship to extend life
	 * @param wood   - the wood to destroy */
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

	/** destroy the target and declare the player win when the player clash the target */
	private void handleCollision(Ship player, Target t){
		if (player.isDestroyed() || t.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(t.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() /*+ t.getRadius()*/ ) {
			return;
		}

		// Destroy Target
		t.setDestroyed(true);

	}

	private void handleCollision(Ship player, Obstacle o){
		if (player.isDestroyed() || o.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(o.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() /*+ t.getRadius()*/ ) {
			return;
		}

		// push the player away from rock
		player.getPosition().add(player.last_movement.scl(-4));

	}

	/**
	 * Check a bullet for being out-of-bounds.
	 * both x and y-axis are implemented
	 *
	 * @param sh Ship to check 
	 */
	private void handleBounds(Ship sh) {
		// Do not let the ship go off-world on both-axis: x
		if (sh.getX() <= sh.getRadius()) {
			sh.setX(sh.getRadius());
		} else if (sh.getX() >= getWidth() - sh.getRadius()) {
			sh.setX(getWidth() - sh.getRadius());
		}

		// Do not let the ship go off-world on both-axis: y
		if (sh.getY() <= sh.getRadius()) {
			sh.setY(sh.getRadius());
		} else if (sh.getY() >= getHeight() - sh.getRadius()) {
			sh.setY(getHeight() - sh.getRadius());
		}
	}
}