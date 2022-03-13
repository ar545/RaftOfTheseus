/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * This class is a singleton for this application, but we have not designed
 * it as one.  That is to give you some extra functionality should you want
 * to add multiple ships.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.util.Controllers;
import edu.cornell.gdiac.util.XBoxController;

/**
 * Class for reading player input. 
 *
 * This supports both a keyboard and X-Box controller. In previous solutions, we only 
 * detected the X-Box controller on start-up.  This class allows us to hot-swap in
 * a controller via the new XBox360Controller class.
 */
public class InputController {
	// Fields to manage game state
	/** Whether the reset button was pressed. */
	protected boolean resetPressed;
	/** Whether the exit button was pressed. */
	protected boolean exitPressed;
	/** Whether the mouse input should be considered */
	protected boolean mouseAllowed;
	/** How much did we move (left/right)? */
	private float x_offset;
	/** How much did we move (up/down)? */
	private float y_offset;
	/** XBox Controller support */
	private final XBoxController xbox;
	
	/**
	 * Returns the amount of sideways movement. 
	 *
	 * -1 = left, 1 = right, 0 = still
	 *
	 * @return the amount of sideways movement. 
	 */
	public Vector2 getMovement() {
		Vector2 m = new Vector2(x_offset, y_offset);
		m.nor(); // normalize vector so diagonal movement isn't 41.4% faster than normal movement
		return m;
	}

	/**
	 * Returns true if the reset button was pressed.
	 *
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {
		return resetPressed;
	}

	/**
	 * Returns true if the exit button was pressed.
	 *
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() {
		return exitPressed;
	}
	
	/**
	 * Creates a new input controller
	 * 
	 * The input controller attempts to connect to the X-Box controller at device 0,
	 * if it exists.  Otherwise, it falls back to the keyboard control.
	 */
	public InputController() { 
		// If we have a game-pad for id, then use it.
		Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
		mouseAllowed = false;
		if (controllers.size > 0) {
			xbox = controllers.get(0);
		} else {
			xbox = null;
		}
	}

	/**
	 * Reads the input for the player and converts the result into game logic.
	 */
	public void readInput() {
		// Check to see if a GamePad is connected
		if (xbox != null && xbox.isConnected()) {
			readGamepad();
			readKeyboard(true); // Read as a back-up
		} else {
			readKeyboard(false);
		}
	}

	/**
	 * Reads input from an X-Box controller connected to this computer.
	 */
	private void readGamepad() {
		resetPressed = xbox.getA();
		exitPressed  = xbox.getBack();

		// Increase animation frame, but only if trying to move
		x_offset = xbox.getLeftX();
		y_offset = xbox.getLeftY();
	}

	/**
	 * Reads input from the keyboard and mouse for movement
	 *
	 * This controller reads from the keyboard regardless of whether an X-Box
	 * controller is connected.  However, if a controller is connected, this method
	 * gives priority to the X-Box controller.
	 *
	 * @param secondary true if the keyboard should give priority to a gamepad
	 */
	private void readKeyboard(boolean secondary) {
		// Give priority to gamepad results
		resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
		exitPressed  = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
		// Press M to enable mouse
//		if(Gdx.input.isKeyJustPressed(Input.Keys.M)){
//			mouseAllowed = !mouseAllowed;
//		}
		// set up the offset
		// TODO: Currently the way this is set up, if the player connects both a keyboard and gamepad controller, they
		//  can move twice as fast by using both input methods at the same time. This doesn't matter for now since the
		//  gameplay prototype won't be using an XBox controller (why did we add support for this to the prototype if
		//  we can't even test it?), but we should be wary of this if we reuse this code.
		x_offset = (secondary ? x_offset : 0.0f);
		y_offset = (secondary ? y_offset : 0.0f);

		// Read keyboard inputs
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
			x_offset += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
			x_offset -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
			y_offset += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)  || Gdx.input.isKeyPressed(Input.Keys.S)) {
			y_offset -= 1.0f;
		}

		//read mouse inputs
		// NOTE if you uncomment this, this code is bugged and allows the player to move twice as fast when using both mouse and another input method.
//		if(mouseAllowed){
//			if (Gdx.input.getDeltaX() > 0) {
//				x_offset += 1.0f;
//			}
//			if (Gdx.input.getDeltaX() < 0) {
//				x_offset -= 1.0f;
//			}
//			if (Gdx.input.getDeltaY() < 0) {
//				y_offset += 1.0f;
//			}
//			if (Gdx.input.getDeltaY() > 0) {
//				y_offset -= 1.0f;
//			}
//		}
	}
}
