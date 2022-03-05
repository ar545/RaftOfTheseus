/*
 * Ship.java
 *
 * This is a passive model, and this model does very little by itself.  
 * All of its work is done by the CollisionController or the 
 * GameplayController. 
 * 
 * This separation is very important for this class because it has a lot 
 * of interactions with other classes.  When a ship fires, it creates  
 * bullets. If did not move that behavior to the GameplayController,
 * then we would have to have a reference to the GameEngine in this
 * class. Tight coupling with the GameEngine is a very bad idea, so
 * we have separated this out.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.*;
import edu.cornell.gdiac.util.*;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;

/**
 * Model class for the player ship.
 */
public class Ship extends GameObject {
	// CONSTANTS
	/** Movement speed **/
	private static final float RAFT_SPEED = 7.0f;
	/** Movement cost for a pixel distance **/
	private static final float MOVE_COST = 0.04f;

	
	// ATTRIBUTES
	/** The movement of the player this turn */
	private Vector2 movement = new Vector2(0f,0f);
	/** The most recent non-zero movement of the player this turn */
	private Vector2 last_movement = new Vector2(0f,0f);


	/** The health of the ship. This must be >=0. */
	private float health;
	/** Maximum player health */
	public static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
	/** Initial player health */
	public static final float INITIAL_PLAYER_HEALTH = 20.0f;
	
	/**
	 * Returns the type of this object.
	 *
	 * We use this instead of runtime-typing for performance reasons.
	 *
	 * @return the type of this object.
	 */
	public ObjectType getType() {
		return ObjectType.SHIP;
	}

	/**
	 * Returns the current player (left/right) movement input.
	 *
	 * @return the current player movement input.
	 */
	public Vector2 getMovement() {
		return movement;
	}
	
	/**
	 * Sets the current player (left/right) movement input.
	 *
	 * @param value the current player movement input.
	 */
	public void setMovement(Vector2 value) {
		movement.set(value);
	}

	/** Getter and setters for health */
	public float getHealth() { return health; }

	public void setHealth(float newHealth) {
		health = Math.max(0, newHealth);
	}

	public void addHealth(float wood) {
		health = Math.min(health + wood, MAXIMUM_PLAYER_HEALTH);
	}

	/** If the player collides with a border/rock, this is called to prevent that movement from costing health */
	public void cancelLastMovementCost() {
		health += last_movement.len()*RAFT_SPEED*MOVE_COST;
	}

	/** If the player collides with a rock, this is called to undo that movement's change to the raft position */
	public void cancelLastMovement() {
		position.add(last_movement.cpy().scl(-RAFT_SPEED));
	}
	
	/**
	 * Initialize a ship with trivial starting position.
	 */
	public Ship() {
		super();
		radius = 50;
		health = INITIAL_PLAYER_HEALTH;
	}
	
	/**
	 * Updates the animation frame and position of this ship.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void update(float delta) {
		// Call superclasses' update
		super.update(delta);

		// Apply movement
		Vector2 temp = movement.cpy();
		position.add(temp.scl(RAFT_SPEED));
		health -= movement.len() * RAFT_SPEED * MOVE_COST; // scale health by distance traveled
		if (health < 0) health = 0;
		if(!movement.isZero()){
			last_movement.set(movement);
		}
	}
	
}
