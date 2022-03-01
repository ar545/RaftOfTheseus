/*
 *  GameObject.java
 *
 * In this application, we have a lot more model classes than we did in
 * previous labs.  Because these classes have a lot in common, we have
 * put their features into a base class, just as you learned in OO programming.
 *
 * With that said, you have to be very careful when subclassing your models.
 * Your hierarchy can get deep and complicated very fast if you are not 
 * careful.  In fact, we have a later lecture about how subclassing is
 * not always a good idea. But it is okay in this instance because we are
 * only subclass one-level deep.
 *
 * This class continues our policy of using "passive" models. It does not 
 * access the methods or fields of any other Model class.  It also 
 * does not store any other model object as a field. This allows us
 * to prevent the models from being tightly coupled.  All of the coupled
 * behavior has been moved to GameplayController.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import edu.cornell.gdiac.util.*;


/**
 * Base class for all Model objects in the game.
 */
public abstract class GameObject extends Environment {

	// Attributes for all game objects in addition to environment
	/** Object velocity vector */
	protected Vector2 velocity;
	/** Whether or not the object should be removed at next timestep. */
	protected boolean destroyed;

	/// Velocity
	/**
	 * Returns the velocity of this object in pixels per animation frame.
	 *
	 * The value returned is a reference to the velocity vector, which may be
	 * modified freely.
	 *
	 * @return the velocity of this object 
	 */
	public Vector2 getVelocity() {
		return velocity;
	}

	/**
	 * Returns the x-coordinate of the object velocity.
	 *
	 * @return the x-coordinate of the object velocity.
	 */
	public float getVX() {
		return velocity.x;
	}

	/**
	 * Sets the x-coordinate of the object velocity.
	 *
	 * @param value the x-coordinate of the object velocity.
	 */
	public void setVX(float value) {
		velocity.x = value;
	}

	/**
	 * Sets the y-coordinate of the object velocity.
	 *
	 * @return the y-coordinate of the object velocity.
	 */
	public float getVY() {
		return velocity.y;
	}

	/**
	 * Sets the y-coordinate of the object velocity.
	 *
	 * @param value the y-coordinate of the object velocity.
	 */
	public void setVY(float value) {
		velocity.y = value;
	}
	

	/**
	 * Returns true if this object is destroyed.
	 *
	 * Objects are not removed immediately when destroyed.  They are garbage collected 
	 * at the end of the frame.  This tells us whether the object should be garbage 
	 * collected at the frame end.
	 *
	 * @return true if this object is destroyed
	 */
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Sets whether this object is destroyed.
	 *
	 * Objects are not removed immediately when destroyed.  They are garbage collected 
	 * at the end of the frame.  This tells us whether the object should be garbage 
	 * collected at the frame end.
	 *
	 * @param value whether this object is destroyed
	 */
	public void setDestroyed(boolean value) {
		destroyed = value;
	}


	/**
	 * Constructs a trivial game object
	 *
	 * The created object has no position or size.  These should be set by the subclasses.
	 */
	public GameObject() {
		position = new Vector2(0.0f, 0.0f);
		velocity = new Vector2(0.0f, 0.0f);
		radius = 0.0f;
		destroyed = false;
	}

	/**
	 * Updates the state of this object.
	 *
	 * This method is only intended to update values that change local state in
	 * well-defined ways, like position or a cool-down value.  It does not handle
	 * collisions (which are determined by the CollisionController).  It is
	 * not intended to interact with other objects in any way at all.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void update(float delta) {
		position.add(velocity);
	}
	
}