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

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;

/**
 * The primary controller class for the game.
 *
 * While GDXRoot is the root class, it delegates all the work to the player mode
 * classes. This is the player mode class for running the game. In initializes all the other classes in the game
 * and hooks them together.  It also provides the
 * basic game loop (update-draw).
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
		/** When the ships are dead (but shells still work) */
		OVER
	}
	
	// Loaded assets
	/** The background image for the game */
	private Texture background;
	/** The font for giving messages to the player */
	private BitmapFont displayFont;
	/** The alternate font for giving messages to the player */
	private BitmapFont alternateFont;

	// following assets are used in the progress bar. progress bar is a "texture atlas." Break it up into parts.
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

	/// CONSTANTS
	/** Factor used to compute where we are in scrolling process */
	private static final float TIME_MODIFIER    = 0.06f;
	/** Offset for the shell counter message on the screen */
	private static final float COUNTER_OFFSET   = 5.0f;
	/** Offset for the game over message on the screen */
	private static final float GAME_OVER_OFFSET = 40.0f;
	/** Height of the crush shell progress bar */
	private static final float GAME_BAR_HEIGHT_RATIO = 0.9f;
	/** Width of the crush shell progress bar */
	private static final float GAME_BAR_WIDTH_RATIO = 0.8f;
	/** Height of the progress bar */
	private static final int PROGRESS_HEIGHT = 30;
	/** Width of the rounded cap on left or right */
	private static final int PROGRESS_CAP    = 15;
	/** the time constant of period of blinking of text and progress bar*/
	private static final int TIME_CONSTANT = 800;
	/** Standard window size (for scaling) */
	private static final int STANDARD_WIDTH  = 800;
	/** Standard window height (for scaling) */
	private static final int STANDARD_HEIGHT = 700;
	
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
		physicsController = new CollisionController(canvas.getWidth(), canvas.getHeight());
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
		background  = directory.getEntry("background",Texture.class);
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
			gameplayController.start(canvas.getWidth(), canvas.getHeight());
			break;
		case OVER:
			if (inputController.didReset()) {
				gameState = GameState.PLAY;
				gameplayController.reset();
				gameplayController.start(canvas.getWidth(), canvas.getHeight());
				gameplayController.player_health = GameplayController.INITIAL_PLAYER_HEALTH;
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
		// if no player is alive, declare game over
		if (!gameplayController.isAlive()) {
			gameState = GameState.OVER;
		}

		// Update objects.
		gameplayController.resolveActions(inputController,delta);

		// Check for collisions
		totalTime += (delta*1000); // Seconds to milliseconds
		float offset =  canvas.getWidth() - (totalTime * TIME_MODIFIER) % canvas.getWidth();
		physicsController.processCollisions(gameplayController.getObjects(),(int)offset);

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
		float offset = -((totalTime * TIME_MODIFIER) % canvas.getWidth());
		canvas.begin();
		canvas.drawBackground(background,offset,-100);
		// Draw the game objects
		for (GameObject o : gameplayController.getObjects()) {
			o.draw(canvas);
		}

		// Draw progress bar
		drawProgress(canvas, gameplayController.getProgress(), ((int)totalTime / TIME_CONSTANT) % 2 == 0 );

		// Draw text
		if (gameState == GameState.OVER) {
			canvas.drawTextCentered("Game Over!",displayFont, GAME_OVER_OFFSET);
			// make restart line blinking
			if(((int)totalTime / TIME_CONSTANT) % 2 == 0 ){
				canvas.drawTextCentered("Press R to Restart", alternateFont, 0);
			}
			canvas.drawText("Current player health: 0", displayFont, COUNTER_OFFSET, canvas.getHeight()-COUNTER_OFFSET);
		}else{
			// Output a simple debugging message stating the number of shells on the screen
			String message = "Current player health: " + gameplayController.player_health;
			canvas.drawText(message, displayFont, COUNTER_OFFSET, canvas.getHeight()-COUNTER_OFFSET);
		}

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
		float sx = ((float)canvas.getWidth())/STANDARD_WIDTH;
		float sy = ((float)canvas.getHeight())/STANDARD_HEIGHT;
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