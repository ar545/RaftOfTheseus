/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter.
 * There must be some undocumented OpenGL code in setScreen.
 *
 * This time we shown how to use Game with player modes.  The player modes are
 * implemented by screens.  Player modes handle their own rendering (instead of the
 * root class calling render for them).  When a player mode is ready to quit, it
 * notifies the root class through the ScreenListener interface.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.*;

/**
 * Root class for a LibGDX.  
 *
 * This class is technically not the ROOT CLASS. Each platform has another class above
 * this (e.g. PC games use DesktopLauncher) which serves as the true root.  However, 
 * those classes are unique to each platform, while this class is the same across all 
 * plaforms. In addition, this functions as the root class all intents and purposes, 
 * and you would draw it as a root class in an architecture specification.  
 */
public class GDXRoot extends Game implements edu.cornell.gdiac.util.ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	/** Player mode for level selecting menu (CONTROLLER CLASS) */
	private MenuMode menu;
	/** Player mode for the game proper (CONTROLLER CLASS) */
	private WorldController playing;
	/** Player mode for the settings mode (CONTROLLER CLASS) */
	private SettingsMode settings;
	/** Which level is currently loaded */
	private int currentLevel = 0;
	/** How many levels there are */
	public static int NUM_LEVELS;
	/** Exit code from loading = -1 */
	/** Exit code for quitting = 0 */
	public static int QUIT;
	/** Exit code for next level = 1 */
	public static int NEXT_LEVEL;
	/** Exit code for previous level = 2 */
	public static int PREV_LEVEL;
	/** Exit code for world to settings = 3 */
	public static int WORLD_TO_SETTINGS;
	/** Exit code for displaying menu screen */
	public static int TO_MENU;
	/** Exit code for displaying playing screen */
	public static int TO_WORLD;
	/** Exit code for menu to settings = 8 */
	public static int MENU_TO_SETTINGS;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot() {}

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas  = new GameCanvas();
		loading = new LoadingMode("assets.json",canvas,1);
		menu = new MenuMode(canvas);
		playing = new WorldController(canvas);
		settings = new SettingsMode(canvas);
		settings.setExitMenu(TO_MENU);
		setScreen(loading);
		loading.setScreenListener(this);
	}

	/**
	 * Called when the Application is destroyed.
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		Screen screen = getScreen();
		setScreen(null);
		screen.dispose();
		SoundController.getInstance().dispose();
		canvas.dispose();
		canvas = null;

		// Unload all of the resources
		if (directory != null) {
			directory.unloadAssets();
			directory.dispose();
			directory = null;
		}
		super.dispose();
	}

	/**
	 * Called when the Application is resized.
	 *
	 * This can happen at any point during a non-paused state but will never happen
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize();
		super.resize(width,height);
	}

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (screen == loading) {
			directory = loading.getAssets();
			// Load sounds and CONSTANTS
			JsonValue screenParams = directory.getEntry("screen_settings", JsonValue.class);
			NUM_LEVELS = screenParams.getInt("level count", 9);
			setExitCodes(screenParams.get("exit codes"));
			MenuMode.setConstants(screenParams.get("screen"));
			SettingsMode.setContants(screenParams.get("screen"));
			WorldController.setConstants(directory.getEntry("object_settings", JsonValue.class));
			InputController.setConstants(directory.getEntry("input_settings", JsonValue.class));
			InputController.getInstance();
			SoundController.getInstance().gatherAssets(directory);
			// Create menu
			menu.setScreenListener(this);
			menu.populate(directory);
			setScreen(menu);
			loading.dispose();
			loading = null;
		} else if (exitCode == PREV_LEVEL) {
			SoundController.getInstance().haltMusic();
			currentLevel = Math.max(0, currentLevel-1);
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if(exitCode == NEXT_LEVEL){
			SoundController.getInstance().haltMusic();
			currentLevel = Math.min(NUM_LEVELS -1, currentLevel+1);
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if (exitCode == WORLD_TO_SETTINGS) {
			settings.setScreenListener(this);
			settings.setPreviousMode(TO_WORLD);
			settings.resetPressedState();
			settings.populate(directory);
			setScreen(settings);
		} else if (exitCode == MENU_TO_SETTINGS) {
			settings.setScreenListener(this);
			settings.setPreviousMode(TO_MENU);
			settings.resetPressedState();
			settings.populate(directory);
			setScreen(settings);
		} else if (screen == settings) {
			settings.resetPressedState();
			menu.resetSettingsState();
			if(exitCode == TO_MENU) {
				menu.setScreenListener(this);
				setScreen(menu);
			} else if (exitCode == TO_WORLD) {
				playing.setScreenListener(this);
//					playing.setLevel(currentLevel);
				setScreen(playing);
			}
		} else if (screen == playing) {
			SoundController.getInstance().haltMusic();
			menu.resetPressedState();
			menu.resetSettingsState();
			menu.resetPlayState();
			menu.setScreenListener(this);
			setScreen(menu);
		} else if (screen == menu) {
			SoundController.getInstance().haltMusic();
			menu.resetPressedState();
			menu.resetSettingsState();
			// Load level
			playing.setScreenListener(this);
			playing.gatherAssets(directory);
			currentLevel = menu.getSelectedLevel() < NUM_LEVELS ? menu.getSelectedLevel() : 0;
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if (exitCode != QUIT) {
			Gdx.app.error("GDXRoot", "Exit with error code "+exitCode, new RuntimeException());
			Gdx.app.exit();
		} else {
			// We quit the main application
			Gdx.app.exit();
		}
	}

	/**
	 * Method to set the exit codes for the different screens.
	 * Found in object_parameters.json
	 * @param objParams JsonValue "exit codes"
	 */
	private static void setExitCodes(JsonValue objParams){
		QUIT = objParams.getInt("quit");
		NEXT_LEVEL = objParams.getInt("next level");
		PREV_LEVEL = objParams.getInt("previous level");
		WORLD_TO_SETTINGS = objParams.getInt("world to settings");
		TO_MENU = objParams.getInt("to menu");
		TO_WORLD = objParams.getInt("to world");
		MENU_TO_SETTINGS = objParams.getInt("menu to settings");
	}
}
