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
	// TODO: add up/down movement and a health field
	/// CONSTANTS
	/** Horizontal speed **/
	private static final float BEETLE_SPEED = 4.0f;
	
	/// ATTRIBUTES
	/** The left/right movement of the player this turn */
	private float movement = 0.0f; // TODO: should probably be a Vector2
	
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
	public float getMovement() {
		return movement;
	}
	
	/**
	 * Sets the current player (left/right) movement input.
	 *
	 * @param value the current player movement input.
	 */
	public void setMovement(float value) {
		movement = value;
	}
	
	/**
	 * Initialize a ship with trivial starting position.
	 */
	public Ship() {
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
		// Call superclass's update
		super.update(delta);

		// Movement handling
		position.x += movement * BEETLE_SPEED;
	}
	
}
