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
	// TODO: design choice: ship_life is implemented in Gameplay Control
	// CONSTANTS
	/** Horizontal speed **/
	private static final float RAFT_SPEED = 2.0f;
	
	// ATTRIBUTES
	/** The movement of the player this turn */
	private Vector2 movement = new Vector2(0f,0f);
	/** The most recent non-zero movement of the player this turn */
	public Vector2 last_movement = new Vector2(0f,0f);

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
		movement = value;
	}

	public float getHealth() { return health; }

	public void setHealth(float newHealth) {
		health = Math.max(0, newHealth);
	}

	public void addHealth(float wood) {
		health = Math.min(health + wood, MAXIMUM_PLAYER_HEALTH);
	}
	
	/**
	 * Initialize a ship with trivial starting position.
	 */
	public Ship() {
		 health = INITIAL_PLAYER_HEALTH;
	}
	
	public void setTexture(Texture texture) {
		animator = new FilmStrip(texture,1,1, 1);
		radius = animator.getRegionHeight() / 2.0f;
		origin = new Vector2(animator.getRegionWidth()/2.0f, animator.getRegionHeight()/2.0f);
	}
	
	/**
	 * Updates the animation frame and position of this ship.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void update(float delta) {
		// Call superclasses' update
		super.update(delta);

		// Movement handling
		health -= movement.len() * RAFT_SPEED * 0.04f; // scale health by distance traveled
		if (health < 0) health = 0;
		position.add(movement.scl(RAFT_SPEED));
		if(!movement.isZero()){
			last_movement.set(getMovement());
		}
	}
	
}
