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
package edu.cornell.gdiac.raftoftheseus.singleton;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Singleton class for reading player input.
 * This only supports keyboard.
 */
public class InputController {
	// Fields to manage game state
	/*=*=*=*=*=*=*=*=*=* SETTINGS *=*=*=*=*=*=*=*=*=*/

	public static void setConstants(JsonValue keyParams){
		controlSettings = keyParams;
	}

	private static JsonValue controlSettings;

	private ArrayMap<String, Integer> mappings;

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
	/** Whether the pause button was pressed */
	private boolean pausePressed;
	private boolean pausePrevious;

	/*=*=*=*=*=*=*=*=*=* PLAYER ACTIONS *=*=*=*=*=*=*=*=*=*/

	/** Whether the map button was pressed. */
	private boolean mapPressed;
	private boolean mapPrevious;
	/** Whether the fire button was pressed. */
	private boolean firePressed;
	private boolean firePrevious;
	/** Whether the tab button was pressed for selection. */
	private boolean specialPressed;
	private boolean specialPrevious;
	/** Whether the right arrow button/D was pressed for adjusting settings. CAN BE HELD.*/
	private boolean rightPressed;
	/** Whether the left arrow button/A was pressed for adjusting settings. CAN BE HELD.*/
	private boolean leftPressed;
	/** Whether the player is sprinting. CAN BE HELD.*/
	private boolean sprintPressed;
	/** Whether the change control scheme button was pressed. */
	private boolean changePrevious;
	private boolean changePressed;
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

	/** Creates a new input controller for mouse and keyboard. */
	public InputController() {
		mov_offset = new Vector2();
		fire_location = new Vector2();
		mappings = new ArrayMap<>();
		populateMap("mouse keyboard");
	}

	/* SETTERS */

	/**
	 * Puts in the control mapping the default key value pairs
	 * @param mapping String key for getting correct JsonValue with mappings
	 */
	private void populateMap(String mapping){
		JsonValue m = controlSettings.get(mapping);
		for( JsonValue i : m ){
			if(i.isString()){
				mappings.put(i.name(), Input.Keys.valueOf(i.asString()));
			} else if (i.isNumber()){
				mappings.put(i.name(), i.asInt());
			}
		}
	}

	/* CUSTOMIZATION INTERFACE */

	/**
	 * Operation to allow customization of key binds. Should be limited
	 * @param action The abstract activity that the user wishes the change, should a key name from the control_settings.json
	 *               Best limited to "map" and "fire" keys.
	 * @param key The new key binding to map action to.
	 * @return Key binding has been succesful
	 */
	public boolean setKey(String action, String key){
		if(mappings.get(action) == null ){
			throw new RuntimeException("Given action does not exist");
		} else if (!Input.Keys.toString(mappings.get(action)).equals(key)) {
			mappings.put(action, Input.Keys.valueOf(key));
			return true;
		}
		return false;
	}

	/**
	 * Function to return which key is being used for a given action.
	 * @param action The abstract activity that the user wishes the change, should a key name from the control_settings.json
	 *               Best limited to "map" and "fire" keys.
	 * @return String of key associated with action.
	 */
	public String getKey(String action){
		if (action.equals("fire")) {
			return "Left Mouse Button";
		}
		String k = Input.Keys.toString(mappings.get(action));
		if (k == null) {
			throw new RuntimeException("Given action does not exist");
		}
		return k;
	}

	/* GETTERS */

	/** @return the singleton instance of the input controller */
	public static InputController getInstance() {
		if (theController == null) {
			theController = new InputController();
		}
		return theController;
	}

	/** @return true if the map button was pressed. */
	public boolean didNext() { return nextPressed && !nextPrevious; }
	/** @return true if the map button was pressed. */
	public boolean didPrevious() { return prevPressed && !prevPrevious; }
	/** @return true if the debug button was pressed. */
	public boolean didDebug() { return debugPressed && !debugPrevious; }
	/** @return true if the reset button was pressed. */
	public boolean didReset() {return resetPressed && !resetPrevious;}
	/** @return true if the exit button was pressed. */
	public boolean didExit() { return exitPressed && !exitPrevious; }
	/** @return true if the map button was pressed. */
	public boolean didMap() { return mapPressed && !mapPrevious; }
	/** @return true if the fire button was pressed. */
	public boolean didCharge() { return firePressed && !firePrevious; }
	/** @return true if the fire button was pressed. */
	public boolean didRelease() { return !firePressed && firePrevious; }
	/** @return true if the change control scheme button was pressed. */
	public boolean didChange() { return changePressed && !changePrevious; }
	/** @return whether special key was pressed. */
	public boolean didSpecial() { return specialPressed && !specialPrevious; }
	/** @return true if the "right" direction button was pressed for keyboard control of settings. */
	public boolean didRight() { return rightPressed; }
	/** @return true if the "left" direction button was pressed for keyboard control of settings. */
	public boolean didLeft() { return leftPressed; }
	/** @return true if the sprint button was pressed. */
	public boolean didSprint() { return sprintPressed; }
	/** @return true if the settings button was pressed. */
	public boolean didPause() { return pausePressed & !pausePrevious; }

	/**
	 * -1 = down/left, 1 = up/right, 0 = still
	 * @return the amount of vertical and horizontal movement
	 * normalize vector so diagonal movement isn't 41.4% faster than normal movement
	 */
	public Vector2 getMovement() { return mov_offset.set(x_offset, y_offset).nor(); }
	/** @return where the mouse is in screen coordinates. */
	public Vector2 getMouseLocation() { return fire_location; }

	/* READ INPUT */

	/** Reads the input for the player and converts the result into game logic. */
	public void readInput() {
		// Store previous values
		resetPrevious  = resetPressed;
		nextPrevious = nextPressed;
		prevPrevious = prevPressed;
		mapPrevious = mapPressed;
		debugPrevious  = debugPressed;
		exitPrevious = exitPressed;
		pausePrevious = pausePressed;
		specialPrevious = specialPressed;
		changePrevious = changePressed;
		firePrevious = firePressed;
		// Read new input
		readKeys();
	}

	/** Reads input from the keyboard for movement. */
	private void readKeys() {
		// Update the mappings
		populateMap("mouse keyboard");

		// Navigation keys
		nextPressed = Gdx.input.isKeyPressed(mappings.get("next"));
		prevPressed = Gdx.input.isKeyPressed(mappings.get("previous"));
		debugPressed  = Gdx.input.isKeyPressed(mappings.get("debug"));
		resetPressed = Gdx.input.isKeyPressed(mappings.get("reset"));
		exitPressed  = Gdx.input.isKeyPressed(mappings.get("exit"));
		pausePressed = Gdx.input.isKeyPressed(mappings.get("pause"));

		// Player action keys
		changePressed = Gdx.input.isKeyPressed(mappings.get("test")); //		changePressed = false;
		specialPressed = Gdx.input.isKeyPressed(mappings.get("special"));
		sprintPressed = Gdx.input.isKeyPressed(mappings.get("sprint"));

		// Reset offsets
		x_offset = 0;
		y_offset = 0;

		// Read direction key inputs
		rightPressed = Gdx.input.isKeyPressed(mappings.get("right")) || Gdx.input.isKeyPressed(mappings.get("d"));
		leftPressed = Gdx.input.isKeyPressed(mappings.get("left")) || Gdx.input.isKeyPressed(mappings.get("a"));
		if (rightPressed) {
			x_offset += 1.0f;
		}
		if (leftPressed) {
			x_offset -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(mappings.get("up")) || Gdx.input.isKeyPressed(mappings.get("w"))) {
			y_offset += 1.0f;
		}
		if (Gdx.input.isKeyPressed(mappings.get("down")) || Gdx.input.isKeyPressed(mappings.get("s"))) {
			y_offset -= 1.0f;
		}

		// TODO add rotational aiming

		mapPressed = Gdx.input.isKeyPressed(mappings.get("map")) || Gdx.input.isKeyPressed(mappings.get("map_alt"));
		firePressed = Gdx.input.isButtonPressed(mappings.get("fire")) || Gdx.input.isKeyPressed(mappings.get("fireKey"));
		fire_location.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
	}

}
