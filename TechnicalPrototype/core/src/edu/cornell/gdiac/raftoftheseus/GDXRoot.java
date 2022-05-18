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
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.raftoftheseus.singleton.MusicController;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import org.lwjgl.Sys;

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
	/** Save game data */
	JsonValue saveData;
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
	/** How many levels there are per page in the level select screen */
	public static int LEVELS_PER_PAGE;
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
		loading = new LoadingMode("assets.json", "save_data.json", canvas,1);
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
		SfxController.getInstance().dispose();
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
		super.resize(width,height);
		canvas.resize();
		if (loading != null) {
			loading.resize(width, height);
		}
		if (menu != null) {
			menu.resize(width, height);
		}
		if (playing != null) {
			playing.resize(width, height);
		}
		if (settings != null) {
			settings.resize(width, height);
		}
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
			// Load the rest of the constants
			directory = loading.getAssets();
			saveData = loading.getSaveGameData();
			setConstants();
			populateScreens();
			// Create the menu
			menu.initButtons();
			setMenuScreen(false);
			menu.setSaveData(saveData);
			// Start the music
			SfxController.getInstance().startMenuMusic();
			loading.dispose();
			loading = null;
		}
		else if (exitCode == PREV_LEVEL) setPlayScreen(Math.max(0, currentLevel-1));
		else if(exitCode == NEXT_LEVEL) {
			if(currentLevel == NUM_LEVELS - 1) {
				setSettingsScreen(TO_MENU);
				setScreen(menu);
				menu.changeScreenTo(MenuMode.MenuScreen.CREDITS);
			} // special: where finishing the last level goes to credit screen
			else { setPlayScreen(Math.min(NUM_LEVELS - 1, currentLevel + 1)); } // standard next-level
		}
		else if (exitCode == WORLD_TO_SETTINGS) setSettingsScreen(TO_WORLD);
		else if (exitCode == MENU_TO_SETTINGS) setSettingsScreen(TO_MENU);
		else if (screen == settings) {
			if(exitCode == TO_MENU) setMenuScreen(false);
			else if (exitCode == TO_WORLD) setPlayScreen();
		}
		else if (screen == playing) setMenuScreen(true);
		else if (screen == menu) setPlayScreen(menu.getSelectedLevel() < NUM_LEVELS ? menu.getSelectedLevel() : 0);
		else if (exitCode != QUIT) {
			Gdx.app.error("GDXRoot", "Exit with error code "+exitCode, new RuntimeException());
			Gdx.app.exit();
		}
		else Gdx.app.exit();
	}

	/**
	 * Set all constants in the game before anything starts.
	 */
	private void setConstants(){
		JsonValue screenParams = directory.getEntry("screen_settings", JsonValue.class);
		NUM_LEVELS = screenParams.getInt("level count", 20);
		LEVELS_PER_PAGE = screenParams.getInt("level per page", 10);
		setExitCodes(screenParams.get("exit codes"));
		MenuMode.setConstants(screenParams.get("screen"), NUM_LEVELS, LEVELS_PER_PAGE);
		JsonValue keyParams = directory.getEntry("input_settings", JsonValue.class);
		SettingsMode.setContants(screenParams.get("screen"));
		SettingsMode.setKeyParams(keyParams);
		WorldController.setConstants(directory.getEntry("object_settings", JsonValue.class));
		WorldController.setKeyParams(keyParams);
		InputController.setConstants(keyParams);
		InputController.getInstance();
		SfxController.getInstance().gatherAssets(directory);
		MusicController.getInstance().gatherAssets(directory);
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

	/**
	 * Method to populate all screens with their appropriate assets.
	 */
	private void populateScreens(){
		settings.populate(directory);
		menu.populate(directory);
		playing.gatherAssets(directory);
	}

	/**
	 * Factored out code for creating the settings screen.
	 */
	private void setSettingsScreen(int KEY){
		settings.setPreviousMode(KEY);
		settings.setScreenListener(this);
		setScreen(settings);
	}

	/**
	 * Factored out code for creating the menu screen.
	 */
	private void setMenuScreen(boolean stopMusic){
		if(stopMusic) {
			SfxController.getInstance().haltMusic();
			SfxController.getInstance().haltSFX();
		}
		SfxController.getInstance().startMenuMusic();
		menu.setScreenListener(this);
		setScreen(menu);
	}

	/**
	 * Set the playing screen when going from everything but settings.
	 */
	private void setPlayScreen(int currentLevel){
		SfxController.getInstance().haltMusic();
		SfxController.getInstance().haltSFX();
		this.currentLevel = currentLevel;
		playing.setLevel(this.currentLevel);
		setPlayScreen();
		playing.setSaveData(saveData);
	}

	/**
	 * Set the playing screen when nothing has changed i.e. coming from settings.
	 */
	private void setPlayScreen(){
		playing.setScreenListener(this);
		setScreen(playing);
	}
}
