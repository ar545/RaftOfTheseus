/* 
 * CollisionController.java
 * 
 * Unless you are making a point-and-click adventure game, every single 
 * game is going to need some sort of collision detection.  In a later 
 * lab, we will see how to do this with a physics engine. For now, we use
 * custom physics. 
 * 
 * This class is an example of subcontroller.  A lot of this functionality
 * could go into GameMode (which is the primary controller).  However, we
 * have factored it out into a separate class because it makes sense as a
 * self-contained subsystem.  Note that this class needs to be aware of
 * of all the models, but it does not store anything as fields.  Everything
 * it needs is passed to it by the parent controller.
 * 
 * This class is also an excellent example of the perils of heap allocation.
 * Because there is a lot of vector mathematics, we want to make heavy use
 * of the Vector2 class.  However, every time you create a new Vector2 
 * object, you must allocate to the heap.  Therefore, we determine the
 * minimum number of objects that we need and pre-allocate them in the
 * constructor.
 *
 * Author: Walker M. White
 * Based on original GameX Ship Demo by Rama C. Hoetzlein, 2002
 * LibGDX version, 1/16/2015
 */
package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.math.*;

/**
 * Controller implementing simple game physics.
 *  
 * This is the simplest of physics engines.  In later labs, we 
 * will see how to work with more interesting engines.
 */
public class CollisionController {

	/** Impulse for giving collisions a slight bounce. */
	public static final float COLLISION_COEFF = 0.1f;
	
	/** Caching object for computing normal */
	private Vector2 normal;

	/** Caching object for computing net velocity */
	private Vector2 velocity;
	
	/** Caching object for intermediate calculations */
	private Vector2 temp;

	/**
     * Contruct a new controller. 
     * 
     * This constructor initializes all the caching objects so that
     * there is no heap allocation during collision detection.
     */
	public CollisionController() { 
		velocity = new Vector2();
		normal = new Vector2();
		temp = new Vector2();
	}

	/** 
	 *  Handles collisions between ships, causing them to bounce off one another.
	 * 
	 *  This method updates the velocities of both ships: the collider and the 
	 *  collidee. Therefore, you should only call this method for one of the 
	 *  ships, not both. Otherwise, you are processing the same collisions twice.
	 * 
	 *  @param ship Ship in candidate collision
	 *  @param wood Driftwood in candidate collision
	 */
	public void checkForCollision(Ship ship, Wood wood) {
		// Calculate the normal of the (possible) point of collision
		normal.set(ship.getPosition()).sub(wood.getPosition());
		float distance = normal.len();
		float impactDistance = (ship.getDiameter() + wood.getDiameter()) / 2f;
		normal.nor();

		// If this normal is too small, there was a collision
		if (distance < impactDistance) {
			// TODO: consume the wood and add it to the player's health
		}
	}

	/**
	 * Nudge the ship to ensure it does not do out of view.
	 *
	 * This code bounces the ship off walls.  You will replace it as part of
	 * the lab.
	 *
	 * @param ship		They player's ship which may have collided
	 * @param bounds	The rectangular bounds of the playing field
	 */
	public void checkInBounds(Ship ship, Rectangle bounds) {
		// TODO: Change this so the raft doesn't bounce off of walls like a bouncy ball
		//Ensure the ship doesn't go out of view. Bounce off walls.
		if (ship.getPosition().x <= bounds.x) {
			ship.getVelocity().x = -ship.getVelocity().x;
			ship.getPosition().x = bounds.x;
		} else if (ship.getPosition().x >= bounds.width) {
			ship.getVelocity().x = -ship.getVelocity().x;
			ship.getPosition().x = bounds.width - 1.0f;
		}

		if (ship.getPosition().y <= bounds.y) {
			ship.getVelocity().y = -ship.getVelocity().y;
			ship.getPosition().y = bounds.y;
		} else if (ship.getPosition().y >= bounds.height) {
			ship.getVelocity().y = -ship.getVelocity().y;
			ship.getPosition().y = bounds.height - 1.0f;
		}
	}
}