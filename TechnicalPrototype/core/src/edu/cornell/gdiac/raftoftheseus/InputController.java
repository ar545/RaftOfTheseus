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
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.util.Controllers;
import edu.cornell.gdiac.util.XBoxController;

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

	private enum ControlScheme{
		KeyboardOnly,
		KeyboardMouse,
		Custom
	}
	private ControlScheme controlScheme;

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

	/*=*=*=*=*=*=*=*=*=* SETTERS *=*=*=*=*=*=*=*=*=*/

	private void populateMap(String mapping){
		JsonValue m = controlSettings.get(mapping);
		System.out.println(m.name());
		for( JsonValue i : m ){
			if(i.isString()){
				mappings.put(i.name(), Input.Keys.valueOf(i.asString()));
			} else if (i.isNumber()){
				mappings.put(i.name(), i.asInt());
			}
		}
	}

	private void setControlScheme(){
		if (controlScheme == ControlScheme.KeyboardMouse){
			populateMap("mouse keyboard");
		}
		else if (controlScheme == ControlScheme.KeyboardOnly){
			populateMap("keyboard only");
		}
	}

	public void setKeyboardMouse(){
		controlScheme = ControlScheme.KeyboardMouse;
		setControlScheme();
	}

	public void setKeyboardOnly(){
		controlScheme = ControlScheme.KeyboardOnly;
		setControlScheme();
	}

	private void changeControlScheme(){
		System.out.println(1);
		switch (controlScheme){
			case KeyboardMouse: setKeyboardOnly(); return;
			case KeyboardOnly: setKeyboardMouse(); return;
			case Custom: return;
		}
	}

	/*=*=*=*=*=*=*=*=*=* GETTERS *=*=*=*=*=*=*=*=*=*/

	/** Creates a new input controller for mouse and keyboard. */
	public InputController() {
		mov_offset = new Vector2();
		fire_location = new Vector2();
		mappings = new ArrayMap<>();
		setKeyboardMouse();
	}

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
	public boolean didFire() { return firePressed && !firePrevious; }
	/** @return true if the tab button was pressed for changing what is selected on a screen. */
	public boolean didTab() { return tabPressed && !tabPrevious; }
	/** @return true if the change control scheme button was pressed. */
	private boolean didChange() { return changePressed && !changePrevious; }
	/** @return true if the "right" direction button was pressed for keyboard control of settings. */
	public boolean didRight() { return rightPressed; }
	/** @return true if the "left" direction button was pressed for keyboard control of settings. */
	public boolean didLeft() { return leftPressed; }
	/** @return true if the sprint button was pressed. */
	public boolean didSprint() { return sprintPressed; }
	/** @return true if the mouse is being used. */
	public boolean mouseActive() { return controlScheme == ControlScheme.KeyboardMouse; }
	/** Find whether the player moved and should reduce health . */
	public boolean Moved(){ return (x_offset!= 0 || y_offset != 0); }

	/**
	 * -1 = down/left, 1 = up/right, 0 = still
	 * @return the amount of vertical and horizontal movement
	 * normalize vector so diagonal movement isn't 41.4% faster than normal movement
	 */
	public Vector2 getMovement() { return mov_offset.set(x_offset, y_offset).nor(); }
	/** @return where the mouse was clicked in screen coordinates */
	public Vector2 getFireDirection() { return fire_location; }

	/*=*=*=*=*=*=*=*=*=* READ INPUT *=*=*=*=*=*=*=*=*=*/

	/** Reads the input for the player and converts the result into game logic. */
	public void readInput() {
		// Store previous values
		resetPrevious  = resetPressed;
		nextPrevious = nextPressed;
		prevPrevious = prevPressed;
		mapPrevious = mapPressed;
		firePrevious = firePressed;
		debugPrevious  = debugPressed;
		exitPrevious = exitPressed;
		tabPrevious = tabPressed;
		changePrevious = changePressed;

		// Read new input
		readKeys();
	}

	/** Reads input from the keyboard for movement. */
	private void readKeys() {
		// Navigation keys
		nextPressed = Gdx.input.isKeyPressed(mappings.get("next"));
		prevPressed = Gdx.input.isKeyPressed(mappings.get("previous"));
		debugPressed  = Gdx.input.isKeyPressed(mappings.get("debug"));
		resetPressed = Gdx.input.isKeyPressed(mappings.get("reset"));
		exitPressed  = Gdx.input.isKeyPressed(mappings.get("exit"));

		// Player action keys
		changePressed = Gdx.input.isKeyPressed(mappings.get("change controls"));
		tabPressed = Gdx.input.isKeyPressed(mappings.get("tab"));
		sprintPressed = Gdx.input.isKeyPressed(mappings.get("sprint"));

		// Reset offsets
		x_offset = 0;
		y_offset = 0;

		// Read direction key inputs
		rightPressed = Gdx.input.isKeyPressed(mappings.get("right"));
		leftPressed = Gdx.input.isKeyPressed(mappings.get("left"));
		if (rightPressed) {
			x_offset += 1.0f;
		}
		if (leftPressed) {
			x_offset -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(mappings.get("up"))) {
			y_offset += 1.0f;
		}
		if (Gdx.input.isKeyPressed(mappings.get("down"))) {
			y_offset -= 1.0f;
		}
		// Map dependent
		if(controlScheme == ControlScheme.KeyboardMouse) {
			mapPressed = Gdx.input.isButtonJustPressed(mappings.get("map"));
			firePressed = Gdx.input.isButtonJustPressed(mappings.get("fire"));
			if (firePressed) {
				fire_location.set(Gdx.input.getX(), Gdx.input.getY());
			}
		} else if (controlScheme == ControlScheme.KeyboardOnly) {
			mapPressed = Gdx.input.isKeyPressed(mappings.get("map"));
			firePressed = Gdx.input.isKeyPressed(mappings.get("fire"));
		}

		if(didChange()){changeControlScheme();}
	}
}
