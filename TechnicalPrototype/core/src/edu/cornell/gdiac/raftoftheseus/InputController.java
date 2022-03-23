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

	/*=*=*=*=*=*=*=*=*=* GAME NAVIGATION CONTROLS *=*=*=*=*=*=*=*=*=*/

	/** Whether the button to advanced worlds was pressed. */
	private boolean nextPressed;
	private boolean nextPrevious;
	/** Whether the button to step back worlds was pressed. */
	private boolean prevPressed;
	private boolean prevPrevious;
	/** Whether the reset button was pressed. */
	private boolean resetPressed;
	private boolean resetPrevious;
	/** Whether the debug toggle was pressed. */
	private boolean debugPressed;
	private boolean debugPrevious;
	/** Whether the exit button was pressed. */
	private boolean exitPressed;
	private boolean exitPrevious;

	/*=*=*=*=*=*=*=*=*=* PLAYER ACTIONS *=*=*=*=*=*=*=*=*=*/

	/** Whether the map button was pressed. */
	private boolean mapPressed;
	private boolean mapPrevious;
	/** Whether the fire button was pressed. */
	private boolean firePressed;
	private boolean firePrevious;
	/** Whether the tab button was pressed for selection. */
	private boolean tabPressed;
	private boolean tabPrevious;
	/** Whether the right arrow button/D was pressed for adjusting settings. */
	private boolean rightPressed;
	/** Whether the left arrow button/A was pressed for adjusting settings. */
	private boolean leftPressed;
	/** Whether the left mouse button was pressed. */
	private boolean mousePressed;
	private boolean mousePrevious;
	/** How much did we move (left/right)? */
	private float x_offset;
	/** How much did we move (up/down)? */
	private float y_offset;
	/** How much did we move in 2D? */
	private Vector2 mov_offset;
	/** Where did we fire? */
	private Vector2 fire_location;

	/** The singleton instance of the input controller */
	private static InputController theController = null;

	/*=*=*=*=*=*=*=*=*=* GETTERS *=*=*=*=*=*=*=*=*=*/

	/**
	 * Creates a new input controller for mouse and keyboard.
	 */
	public InputController() {
		mov_offset = new Vector2();
		fire_location = new Vector2();
	}

	/**
	 * @return the singleton instance of the input controller
	 */
	public static InputController getInstance() {
		if (theController == null) {
			theController = new InputController();
		}
		return theController;
	}

	/**
	 * @return true if the map button was pressed.
	 */
	public boolean didNext() { return nextPressed && !nextPrevious; }

	/**
	 * @return true if the map button was pressed.
	 */
	public boolean didPrevious() {
		return prevPressed && !prevPrevious;
	}

	/**
	 * @return true if the debug button was pressed.
	 */
	public boolean didDebug() {
		return debugPressed && !debugPrevious;
	}

	/**
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {return resetPressed && !resetPrevious;}

	/**
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() { return exitPressed && !exitPrevious; }

	/**
	 * @return true if the map button was pressed.
	 */
	public boolean didMap() {
		return mapPressed && !mapPrevious;
	}

	/**
	 * @return true if the fire button was pressed.
	 */
	public boolean didFire() { return firePressed && !firePrevious; }

	/**
	 * @return true if the tab button was pressed for changing what is selected on a screen.
	 */
	public boolean didTab() { return tabPressed && !tabPrevious; }

	/**
	 * @return true if the "right" direction button was pressed for keyboard control of settings.
	 */
	public boolean didRight() { return rightPressed; }

	/**
	 * @return true if the "left" direction button was pressed for keyboard control of settings.
	 */
	public boolean didLeft() { return leftPressed; }

	/**
	 * -1 = down/left, 1 = up/right, 0 = still
	 * @return the amount of vertical and horizontal movement
	 */
	public Vector2 getMovement() {
		return mov_offset.set(x_offset, y_offset).nor(); // normalize vector so diagonal movement isn't 41.4% faster than normal movement
	}

	/** Find whether the player moved and should reduce health . */
	public boolean Moved(){ return (x_offset!= 0 || y_offset != 0); }

	/**
	 * @return where the mouse was clicked in screen coordinates
	 */
	public Vector2 getFireLocation() {
		fire_location.set(x_offset, y_offset);
		return fire_location;
	}

//	/**
//	 * @return true if the fire button was pressed.
//	 */
//	public boolean didLeftClick() {
//		return mousePressed && !mousePrevious;
//	}

	/*=*=*=*=*=*=*=*=*=* READ INPUT *=*=*=*=*=*=*=*=*=*/

	/**
	 * Reads the input for the player and converts the result into game logic.
	 */
	public void readInput() {
		// Store previous values
		resetPrevious  = resetPressed;
		nextPrevious = nextPressed;
		prevPrevious = prevPressed;
		mapPrevious = mapPressed;
		firePrevious = firePressed;
		debugPrevious  = debugPressed;
		exitPrevious = exitPressed;
		mousePrevious = mousePressed;
		tabPrevious = tabPressed;

		// Read new input
		readKeyboard();
		readMouse();
	}

	/**
	 * Reads input from the keyboard for movement.
	 */
	private void readKeyboard() {
		// Navigation keys
		nextPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_2);
		prevPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_1);
		debugPressed  = Gdx.input.isKeyPressed(Input.Keys.G);
		resetPressed = Gdx.input.isKeyPressed(Input.Keys.R);
		exitPressed  = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);

		// Player action keys
		mapPressed = Gdx.input.isKeyPressed(Input.Keys.SPACE);
		firePressed  = Gdx.input.isKeyPressed(Input.Keys.F);
		tabPressed = Gdx.input.isKeyPressed(Input.Keys.TAB);

		// Reset offsets
		x_offset = 0;
		y_offset = 0;

		// Read direction key inputs
		rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
		leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
		if (rightPressed) {
			x_offset += 1.0f;
		}
		if (leftPressed) {
			x_offset -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
			y_offset += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
			y_offset -= 1.0f;
		}
	}

	/**
	 * Reads input from the mouse for firing and direction.
	 */
	private void readMouse() {
		firePressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
		if (firePressed) {
			fire_location.set(Gdx.input.getX(), Gdx.input.getY());
		}
	}
}
