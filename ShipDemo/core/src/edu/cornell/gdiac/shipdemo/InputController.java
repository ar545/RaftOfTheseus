/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameMode does not have to keep track of the current
 * key mapping.
 *
 * This class is NOT a singleton. Each input device is its own instance,
 * and you may have multiple input devices attached to the game.
 *
 * Author: Walker M. White
 * Based on original GameX Ship Demo by Rama C. Hoetzlein, 2002
 * LibGDX version, 1/16/2015
 */
package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.util.*;
import com.badlogic.gdx.controllers.Controller;

/**
 * Device-independent input manager.
 *
 * This class supports both a keyboard and an X-Box controller.  Each player is
 * assigned an ID.  When the class is created, we check to see if there is a 
 * controller for that ID.  If so, we use the controller.  Otherwise, we default
 * the the keyboard.
 */
public class InputController {
	// TODO: Rewrite this class so that up/down/left/right move the player up/down/left/right, instead of rotating the player and moving them forward/backward.
	/** How much forward are we going? */
	private float forward;				
	
	/** How much are we turning? */
	private float turning;
	
	/** 
	 * Returns the amount of forward movement.
	 * 
	 * -1 = backward, 1 = forward, 0 = still
	 *  
	 * @return amount of forward movement.
	 */
	public float getForward() {
		return forward;
	}

	/**
	 * Returns the amount to turn the ship.
	 * 
	 * -1 = clockwise, 1 = counter-clockwise, 0 = still
	 * 
	 * @return amount to turn the ship.
	 */
	public float getTurn() {
		return turning;
	}

	/**
	 * Creates a new input controller.
	 * 
	 * The game supports one player controllable using WASD or Arrow keys.
	 */
	public InputController() {
		assert(true);
	}

	/**
	 * Reads the input for this player and converts the result into game logic.
	 *
	 * This is an example of polling input.  Instead of registering a listener,
	 * we ask the controller about its current state.  When the game is running,
	 * it is typically best to poll input instead of using listeners.  Listeners
	 * are more appropriate for menus and buttons (like the loading screen). 
	 */
	public void readInput() {
		// Figure out, based on which player we are, which keys
		// control our actions (depends on player).
		// TODO: make it so that both arrow keys and WASD can be used to control the player.
		int up, left, right, down;
		up    = Input.Keys.UP;
		down  = Input.Keys.DOWN;
		left  = Input.Keys.LEFT;
		right = Input.Keys.RIGHT;
//		up    = Input.Keys.W;
//		down  = Input.Keys.S;
//		left  = Input.Keys.A;
//		right = Input.Keys.D;

		// Convert keyboard state into game commands
		forward = turning = 0;

		// Movement forward/backward
		if (Gdx.input.isKeyPressed(up) && !Gdx.input.isKeyPressed(down)) {
			forward = 1;
		} else if (Gdx.input.isKeyPressed(down) && !Gdx.input.isKeyPressed(up)) {
			forward = -1;
		}

		// Movement left/right
		if (Gdx.input.isKeyPressed(left) && !Gdx.input.isKeyPressed(right)) {
			turning = 1;
		} else if (Gdx.input.isKeyPressed(right) && !Gdx.input.isKeyPressed(left)) {
			turning = -1;
		}
    }
}