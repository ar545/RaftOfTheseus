/*
 * GameMode.java
 *
 * This is the primary class file for running the game.  You should study this file for
 * ideas on how to structure your own root class. This class follows a 
 * model-view-controller pattern fairly strictly.
 *
 * Author: Walker M. White
 * Based on original GameX Ship Demo by Rama C. Hoetzlein, 2002
 * LibGDX version, 1/16/2015
 */
package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.math.*;

import edu.cornell.gdiac.assets.AssetDirectory;

import com.badlogic.gdx.graphics.*;

/**
 * The primary controller class for the game.
 *
 * While GDXRoot is the root class, it delegates all of the work to the player mode
 * classes. This is the player mode class for running the game. In initializes all 
 * of the other classes in the game and hooks them together.  It also provides the
 * basic game loop (update-draw).
 */
public class GameMode implements ModeController {
	/**
	 * Track the current state of the game for the update loop.
	 */
	public enum GameState {
		/** Before the game has started */
		INTRO,
		/** While we are playing the game */
		PLAY,
		/** When the ships is dead (but shells still work) */
		OVER
	}

	/** The background image for the battle */
	private Texture background;
//	/** Texture for the ship */
//	private Texture shipTexture;
//	/** Texture for driftwood */
//	private Texture woodTexture;

	/** Reference to drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;

    // Instance variables
	/** Read input for player from keyboard (CONTROLLER CLASS) */
	protected InputController inputController;
	/** Handle collision and physics (CONTROLLER CLASS) */
	protected CollisionController physicsController;
	/** Constructs the game models and handle basic gameplay (CONTROLLER CLASS) */
	private GameplayController gameplayController;

	/** Variable to track the game state (SIMPLE FIELDS) */
	private GameState gameState;
	/** Whether or not this player mode is still active */
	private boolean active;

	/** Location and animation information for player ship (MODEL CLASS) */
	protected Ship playerShip;
	/** Information for 1 piece of wood (MODEL CLASS) */
	protected Wood someWood;

	/** Store the bounds to enforce the playing region */	
	private Rectangle bounds;

	/**
	 * Creates a new game with a playing field of the given size.
	 *
	 * This constructor initializes the models and controllers for the game.  The
	 * view has already been initialized by the root class.
	 *
	 * @param width 	The width of the game window
	 * @param height 	The height of the game window
	 * @param assets	The asset directory containing all the loaded assets
	 */
	public GameMode(float width, float height, AssetDirectory assets) {
		this.canvas = new GameCanvas((int)width, (int)height);
		active = false;
		// Null out all pointers, 0 out all ints, etc.
		gameState = GameState.INTRO;

		// Extract the assets from the asset directory.  All images are textures.
		background = assets.getEntry("background", Texture.class );

		bounds = new Rectangle(0,0,width,height);

		// Create the controllers.
		inputController = new InputController();
        physicsController = new CollisionController();
		gameplayController = new GameplayController();

		// populate gameplay controller with textures
		gameplayController.populate(assets);
	}

	/** 
	 * Read user input, calculate physics, and update the models.
	 *
	 * This method is HALF of the basic game loop.  Every graphics frame 
	 * calls the method update() and the method draw().  The method update()
	 * contains all of the calculations for updating the world, such as
	 * checking for collisions, gathering input, and playing audio.  It
	 * should not contain any calls for drawing to the screen.
	 */
	@Override
	public void update() {
		// Process the game input
		inputController.readInput();

		// Test whether to reset the game.
		switch (gameState) {
			case INTRO:
				gameState = GameState.PLAY;
				gameplayController.start(canvas.getWidth() * 0.67f, canvas.getHeight()*0.5f);
				break;
			case OVER:
				if (inputController.didReset()) {
					gameState = GameState.PLAY;
					gameplayController.reset();
					gameplayController.start(canvas.getWidth() * 0.67f, canvas.getHeight()*0.5f);
				} else {
					play();
				}
				break;
			case PLAY:
				play();
				break;
			default:
				break;
		}
	}

	/**
	 * This method processes a single step in the game loop.
	 *
	 */
	protected void play() {
		// if no player is alive, declare game over
		if (!gameplayController.isAlive()) {
			gameState = GameState.OVER;
		}

		// Update objects.
		gameplayController.resolveActions(inputController);

		// Check for collisions
		physicsController.processCollisions(gameplayController.getPlayer(), gameplayController.getObjects());

		// Clean up destroyed objects
		gameplayController.garbageCollect();
	}

	/**
	 * Draw the game on the provided GameCanvas
	 *
	 * There should be no code in this method that alters the game state.  All 
	 * assignments should be to local variables or cache fields only.
	 *
	 * @param canvas The drawing context
	 */
	@Override
	public void draw(GameCanvas canvas) {
		canvas.drawOverlay(background, true);
		// Draw the game objects
		for (GameObject o : gameplayController.getObjects()) {
			o.draw(canvas);
		}
	}

	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		inputController = null;
		gameplayController = null;
		physicsController  = null;
		canvas = null;
	}
	
	/**
	 * Resize the window for this player mode to the given dimensions.
	 *
	 * This method is not guaranteed to be called when the player mode
	 * starts.  If the window size is important to the player mode, then
	 * these values should be passed to the constructor at start.
	 *
	 * @param width The width of the game window
	 * @param height The height of the game window
	 */
	public void resize(int width, int height) {
		bounds.set(0,0,width,height);
	}
}