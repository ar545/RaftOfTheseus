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
	 *  @param ship1 First ship in candidate collision
	 *  @param ship2 Second ship in candidate collision
	 */
	public void checkForCollision(Ship ship1, Ship ship2) {
		// Calculate the normal of the (possible) point of collision
		normal.set(ship1.getPosition()).sub(ship2.getPosition());
		float distance = normal.len();
		float impactDistance = (ship1.getDiameter() + ship2.getDiameter()) / 2f;
		normal.nor();

		// If this normal is too small, there was a collision
		if (distance < impactDistance) {
			// "Roll back" time so that the ships are barely touching (e.g. point of impact).
			// We need to use temp, as the method scl would change the contents of normal!
			temp.set(normal).scl((impactDistance - distance) / 2);  // normal * (d1 - dist)/2
			ship1.getPosition().add(temp);

			temp.set(normal).scl((impactDistance - distance) / 2);  // normal * (d2 - dist)/2
			ship2.getPosition().sub(temp);

			// Now it is time for Newton's Law of Impact.
			// Convert the two velocities into a single reference frame
			velocity.set(ship1.getVelocity()).sub(ship2.getVelocity()); // v1-v2

			// Compute the impulse (see Essential Math for Game Programmers)
			float impulse = (-(1 + COLLISION_COEFF) * normal.dot(velocity)) /
							(normal.dot(normal) * (1 / ship1.getMass() + 1 / ship2.getMass()));

			// Change velocity of the two ships using this impulse
			temp.set(normal).scl(impulse / ship1.getMass());
			ship1.getVelocity().add(temp);

			temp.set(normal).scl(impulse / ship2.getMass());
			ship2.getVelocity().sub(temp);
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