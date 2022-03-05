/*
 * GameMode.java
 *
 * This is the primary class file for running the game.  You should study this file for
 * ideas on how to structure your own root class. This class follows a 
 * model-view-controller pattern fairly strictly.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;

//import com.badlogic.gdx.utils.*;
//import com.badlogic.gdx.assets.*;
//import com.badlogic.gdx.graphics.g2d.freetype.*;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;

/**
 * The primary controller class for the game.
 *
 * While GDXRoot is the root class, it delegates all the work to the player mode
 * classes. This is the player mode class for running the game. In initializes all the other classes in the game
 * and hooks them together.  It also provides the
 * basic game loop (update-draw).
 *
 * Screen size defined by canvas.getWidth(), canvas.getHeight()
 *
 * Map size defined by
 */
public class GameMode implements Screen {
	/** 
 	 * Track the current state of the game for the update loop.
 	 */
	public enum GameState {
		/** Before the game has started */
		INTRO,
		/** While we are playing the game */
		PLAY,
		/** When the player loses the game */
		OVER,
		/** When the player wins the game */
		WIN
	}
	
	// Loaded assets
	/** The background image for the game */
	private Texture background;
	/** The font for giving messages to the player */
	private BitmapFont displayFont;
	/** The alternate font for giving messages to the player */
	private BitmapFont alternateFont;

	// Load progress bar assets. progress bar is a "texture atlas." Break it up into parts.
	/** Left cap to the status background (grey region) */
	private TextureRegion statusBkgLeft;
	/** Middle portion of the status background (grey region) */
	private TextureRegion statusBkgMiddle;
	/** Right cap to the status background (grey region) */
	private TextureRegion statusBkgRight;
	/** Left cap to the status foreground (colored region) */
	private TextureRegion statusFrgLeft;
	/** Middle portion of the status foreground (colored region) */
	private TextureRegion statusFrgMiddle;
	/** Right cap to the status foreground (colored region) */
	private TextureRegion statusFrgRight;

	// TIME CONSTANTS
	/** the time constant of period of blinking of text and progress bar*/
	private static final int TIME_CONSTANT = 800;

	// DISPLAY CONSTANTS
	/** Offset for the shell counter message on the screen */
	private static final float COUNTER_OFFSET   = 5.0f;
	/** Offset for the game over message on the screen */
	private static final float GAME_OVER_OFFSET = 80.0f;
	/** Height of the health progress bar on screen */
	private static final float GAME_BAR_HEIGHT_RATIO = 0.89f;
	/** Width of the health progress bar */
	private static final float GAME_BAR_WIDTH_RATIO = 0.8f;
	/** Height of the progress bar */
	private static final int PROGRESS_HEIGHT = 30;
	/** Width of the rounded cap on left or right */
	private static final int PROGRESS_CAP    = 15;

	// MAP CONSTANTS
	// TODO: substitute this with level JSON in Technical Prototype
	/** temporary world size: width (in tiles) */
	private static final int WORLD_WIDTH = 13;
	/** temporary world size: height (in tiles) */
	private static final int WORLD_HEIGHT = 11;
	/** Size of a tile (in pixels) */
	private static final float WORLD_TILE_SIZE = 100.0f;
	/** How many wood pieces to place on the level */
	private static final int WORLD_WOOD_AMOUNT = 24;


	// reference to gameplay elements
	/** Reference to drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;
	/** Reads input from keyboard or game pad (CONTROLLER CLASS) */
	private InputController inputController;
	/** Handle collision and physics (CONTROLLER CLASS) */
	private CollisionController physicsController;
	/** Constructs the game models and handle basic gameplay (CONTROLLER CLASS) */
	private GameplayController gameplayController;
	/** Variable to track the game state (SIMPLE FIELDS) */
	private GameState gameState;
	/** Variable to track total time played in milliseconds (SIMPLE FIELDS) */
	private float totalTime = 0;
	/** Whether this player mode is still active */
	private boolean active;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;

	// reference to canvas size
	/** Standard window size */
	private final float canvas_width;
	/** Standard window height */
	private final float canvas_height;

	/**
	 * Creates a new game with the given drawing context.
	 *
	 * This constructor initializes the models and controllers for the game.  The
	 * view has already been initialized by the root class.
	 */
	public GameMode(GameCanvas canvas) {
		this.canvas = canvas;
		active = false;
		// Null out all pointers, 0 out all ints, etc.
		gameState = GameState.INTRO;

		// Create the controllers.
		inputController = new InputController();
		gameplayController = new GameplayController();
		// TODO for technical prototype
		// TODO: change to gameplayController.start(level JSON class);
		physicsController = new CollisionController(WORLD_WIDTH, WORLD_HEIGHT, WORLD_TILE_SIZE);
		canvas_height = canvas.getHeight();
		canvas_width = canvas.getWidth();
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
	 * Populates this mode from the given the directory.
	 *
	 * The asset directory is a dictionary that maps string keys to assets.
	 * Assets can include images, sounds, and fonts (and more). This
	 * method delegates to the gameplay controller
	 *
	 * @param directory 	Reference to the asset directory.
	 */
	public void populate(AssetDirectory directory) {
		background  = directory.getEntry("new_background",Texture.class);
		displayFont = directory.getEntry("times",BitmapFont.class);
		alternateFont = directory.getEntry("grande",BitmapFont.class);

		// populate progressbar texture Break up the status bar texture into regions
		statusBkgLeft = directory.getEntry( "progress.backleft", TextureRegion.class );
		statusBkgRight = directory.getEntry( "progress.backright", TextureRegion.class );
		statusBkgMiddle = directory.getEntry( "progress.background", TextureRegion.class );
		statusFrgLeft = directory.getEntry( "progress.foreleft", TextureRegion.class );
		statusFrgRight = directory.getEntry( "progress.foreright", TextureRegion.class );
		statusFrgMiddle = directory.getEntry( "progress.foreground", TextureRegion.class );

		gameplayController.populate(directory);
	}

	/**
	 * Update the game state.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	private void update(float delta) {
		// Process the game input
		inputController.readInput();

		// Test whether to reset the game.
		switch (gameState) {
		case INTRO:
			gameState = GameState.PLAY;
			// TODO for technical prototype
			// TODO: change to gameplayController.start(level JSON class);
			gameplayController.start(WORLD_WIDTH, WORLD_HEIGHT, WORLD_TILE_SIZE, WORLD_WOOD_AMOUNT);
			break;

		case WIN:
		case OVER:
			if (inputController.didReset()) {
				gameState = GameState.PLAY;
				gameplayController.reset();
				// TODO for technical prototype
				// TODO: change to gameplayController.start(level JSON class);
				gameplayController.start(WORLD_WIDTH, WORLD_HEIGHT, WORLD_TILE_SIZE, WORLD_WOOD_AMOUNT);

			} else {
				play(delta);
			}
			break;
		case PLAY:
			play(delta);
			break;
		default:
			break;
		}
	}

	
	/**
	 * This method processes a single step in the game loop.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	protected void play(float delta) {
		// if no player is win, declare game win
		if (gameplayController.isWin()) {
			gameState = GameState.WIN;
		}

		// if no player is alive, declare game over
		else if (!gameplayController.isAlive()) {
			gameState = GameState.OVER;
		}

		// Update objects.
		gameplayController.resolveActions(inputController,delta);

		// Check for collisions
		totalTime += (delta*1000); // Seconds to milliseconds

		// Process player - objects collision
		// TODO: process inter-objects collision instead of merely player objects interaction
		physicsController.processCollisions(gameplayController.getObjects(), gameplayController.getPlayer(),(int)totalTime);

		// Process player - environment collision
		physicsController.processCollisions(gameplayController.getEnvs(), gameplayController.getPlayer());

		// Clean up destroyed objects
		gameplayController.garbageCollect();
	}
	
	/**
	 * Draw the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 */
	private void draw(float delta) {
		// calculate offset = (ship pos) - (canvas size / 2)
		Vector2 offset2 = new Vector2(canvas_width/2, canvas_height/2);
		offset2.sub(gameplayController.getPlayerPosition());

		canvas.begin();
		//draw background
		canvas.drawBackgroundAffine(background, offset2);
		// draw grid
		gameplayController.getGrid().drawAffine(canvas, offset2);

		// Draw the game environments
		for (Environment e : gameplayController.getEnvs()) {
			e.drawAffine(canvas, offset2);
		}

		// Draw the game objects
		for (GameObject o : gameplayController.getObjects()) {
			o.drawAffine(canvas, offset2);
		}

		// Draw progress bar
		drawProgress(canvas, gameplayController.getProgress(), ((int)totalTime / TIME_CONSTANT) % 2 == 0 );

		// Draw text
		String top_message = String.format("Current player health: %.1f", gameplayController.getPlayerHealth());
		if(gameState != GameState.PLAY) {
			String end_message = "Game Over!";
			String blink_message = "Press R to Restart";
			if(gameState == GameState.WIN){
				switch(gameplayController.getStar()){
					case 3:
						top_message = "3 Stars! You are the best!";
						break;
					case 2:
						top_message = "2 Stars! You found the goal!";
						break;
					default:
						top_message = "1 Star! You made it!";
						break;
				}
				end_message = "You Win!";
			}
			canvas.drawTextCentered(end_message,displayFont, 1.5f * GAME_OVER_OFFSET);
			// make restart line blinking
//			if(((int)totalTime / TIME_CONSTANT) % 2 == 0 ){
				canvas.drawTextCentered(blink_message, alternateFont, GAME_OVER_OFFSET);
//			}
		}
		canvas.drawText(top_message, displayFont, COUNTER_OFFSET, canvas.getHeight()-COUNTER_OFFSET);

		// Flush information to the graphic buffer.
		canvas.end();
	}
	
	/**
	 * Called when the Screen is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		// IGNORE FOR NOW
	}

	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			update(delta);
			draw(delta);
			if (inputController.didExit() && listener != null) {
				listener.exitScreen(this, 0);
			}
		}
	}

	/**
	 * Called when the Screen is paused.
	 * 
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub
	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
		active = true;
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to request to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}


	/**
	 * Updates the progress bar according to crush_shells_progress
	 *
	 * The progress bar is composed of parts: two rounded caps on the end,
	 * and a rectangle in a middle.  We adjust the size of the rectangle in
	 * the middle to represent the amount of progress.
	 *
	 * @param canvas The drawing context
	 */
	private void drawProgress(GameCanvas canvas, float progress, boolean blink) {

		// Compute the drawing scale
		float sx = ((float)canvas.getWidth())/canvas_width;
		float sy = ((float)canvas.getHeight())/canvas_height;
		float scale = (Math.min(sx, sy));
		int width = (int)(GAME_BAR_WIDTH_RATIO*canvas.getWidth());
		int centerY = (int)(GAME_BAR_HEIGHT_RATIO*canvas.getHeight());
		int centerX = width/2;
		int heightY = canvas.getWidth();
		Color color = Color.WHITE;
//		if(blink){
//			color = Color.SALMON;
//		}

		// draw the background
		canvas.draw(statusBkgLeft,   centerX-width/2, centerY,
				scale*PROGRESS_CAP, scale*PROGRESS_HEIGHT, Color.LIGHT_GRAY);
		canvas.draw(statusBkgRight,  centerX+width/2-scale*PROGRESS_CAP, centerY,
				scale*PROGRESS_CAP, scale*PROGRESS_HEIGHT, Color.LIGHT_GRAY);
		canvas.draw(statusBkgMiddle, centerX-width/2+scale*PROGRESS_CAP, centerY,
				width-2*scale*PROGRESS_CAP, scale*PROGRESS_HEIGHT, Color.LIGHT_GRAY);

		// draw the foreground
		canvas.draw(statusFrgLeft,   centerX-width/2, centerY,
				scale*PROGRESS_CAP, scale*PROGRESS_HEIGHT, color);

		if (progress > 0) {
			float span = progress * (width - 2 * scale * PROGRESS_CAP) * 0.99f;
			canvas.draw(statusFrgRight,  centerX-width/2+scale*PROGRESS_CAP+span, centerY,
					scale*PROGRESS_CAP, scale*PROGRESS_HEIGHT, color);
			canvas.draw(statusFrgMiddle, centerX-width/2+scale*PROGRESS_CAP, centerY,
					span, scale* PROGRESS_HEIGHT, color);
		} else {
			canvas.draw(statusFrgRight,  centerX-width/2+scale* PROGRESS_CAP, centerY,
					scale* PROGRESS_CAP, scale* PROGRESS_HEIGHT, color);
		}
	}
}