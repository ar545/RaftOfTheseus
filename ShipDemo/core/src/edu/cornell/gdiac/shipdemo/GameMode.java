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
import edu.cornell.gdiac.util.FilmStrip;

import com.badlogic.gdx.audio.*;
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
	/** Number of rows in the ship image filmstrip */
	private static final int SHIP_ROWS = 4;
	/** Number of columns in this ship image filmstrip */
	private static final int SHIP_COLS = 5;
	/** Number of elements in this ship image filmstrip */
	private static final int SHIP_SIZE = 18;
	
	/** The background image for the battle */
	private Texture background;
	/** Texture for the ship (colored for each player) */
	private Texture shipTexture;
	
    // Instance variables
	/** Read input for player from keyboard (CONTROLLER CLASS) */
	protected InputController playerController;
    /** Handle collision and physics (CONTROLLER CLASS) */
    protected CollisionController physicsController;

	/** Location and animation information for player ship (MODEL CLASS) */
	protected Ship playerShip;

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
		// Extract the assets from the asset directory.  All images are textures.
		background = assets.getEntry("background", Texture.class );
		shipTexture = assets.getEntry( "ship", Texture.class );

		bounds = new Rectangle(0,0,width,height);

		// Create the ship and place it
		
        // PLAYER
		playerShip = new Ship(width*(2.0f / 3.0f), height*(1.0f / 2.0f), 180);
		playerShip.setFilmStrip(new FilmStrip(shipTexture,SHIP_ROWS,SHIP_COLS,SHIP_SIZE));
		playerShip.setColor(new Color(0.5f, 0.5f, 1.0f, 1.0f));

		// Create the input controllers.
		playerController = new InputController();
        physicsController = new CollisionController();
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
		// Read the keyboard for each controller.
		playerController.readInput ();

		// Move the ships forward (ignoring collisions)
		playerShip.move(playerController.getForward(), playerController.getTurn());

		physicsController.checkInBounds(playerShip, bounds);
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
		
		// First drawing pass (ships + shadows)
		playerShip.drawShip(canvas);
	}

	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		// Garbage collection here is sufficient.  Nothing to do
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