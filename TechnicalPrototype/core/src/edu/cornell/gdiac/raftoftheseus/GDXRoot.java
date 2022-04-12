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
import com.badlogic.gdx.graphics.g2d.freetype.*;
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
	private int numLevels = 9;
	/** Exit code for displaying start screen */
	private static final int DISPLAY_START = 4;
	/** Exit code for displaying menu screen */
	private static final int DISPLAY_MENU = 5;
	/** Exit code for displaying playing screen */
	private static final int DISPLAY_WORLD = 6;


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
		constantsLoaded = false;
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

	private boolean constantsLoaded;

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (exitCode == WorldController.EXIT_PREV){
			SoundController.getInstance().haltSounds();
			currentLevel = Math.max(0, currentLevel-1);
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if(exitCode == WorldController.EXIT_NEXT){
			SoundController.getInstance().haltSounds();
			currentLevel = Math.min(numLevels-1, currentLevel+1);
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if (exitCode == WorldController.EXIT_SETTINGS) {
			settings.setScreenListener(this);
			settings.setPreviousMode(DISPLAY_WORLD);
			settings.resetPressedState();
			settings.populate(directory);
			setScreen(settings);
		} else if (exitCode == MenuMode.EXIT_SETTINGS) {
			settings.setScreenListener(this);
			settings.setPreviousMode(DISPLAY_MENU);
			settings.resetPressedState();
			settings.populate(directory);
			setScreen(settings);
		} else if (screen == settings) {
			switch (exitCode) {
				case DISPLAY_START:
					// TODO
					break;
				case DISPLAY_MENU:
					menu.setScreenListener(this);
					menu.resetPressedState();
					setScreen(menu);
					break;
				case DISPLAY_WORLD:
					playing.setScreenListener(this);
					playing.setLevel(currentLevel);
					setScreen(playing);
					break;
			}

		} else if (screen == loading) {
			directory = loading.getAssets();
			// Stop load sounds and CONSTANTS
			SoundController.getInstance().gatherAssets(directory);
			if(!constantsLoaded) {
				constantsLoaded = true;
				JsonValue objParams = directory.getEntry("object_parameters", JsonValue.class);
				WorldController.setConstants(objParams);
//				MenuMode.setConstants(objParams);
				JsonValue keys = directory.getEntry("control_settings", JsonValue.class);
				InputController.setConstants(keys);
				InputController.getInstance();
			}
			// Create menu
			menu.setScreenListener(this);
			menu.populate(directory);
			setScreen(menu);
			loading.dispose();
			loading = null;
		} else if (screen == playing){
			SoundController.getInstance().haltSounds();
			menu.setScreenListener(this);
			setScreen(menu);
		} else if (screen == menu) {
			SoundController.getInstance().haltSounds();
			menu.resetPressedState();
			// Load level
			playing.setScreenListener(this);
			playing.gatherAssets(directory);
			currentLevel = menu.getSelectedLevel() < numLevels ? menu.getSelectedLevel() : 0;
			playing.setLevel(currentLevel);
			setScreen(playing);
		} else if (exitCode != WorldController.EXIT_QUIT) {
			Gdx.app.error("GDXRoot", "Exit with error code "+exitCode, new RuntimeException());
			Gdx.app.exit();
		} else {
			// We quit the main application
			Gdx.app.exit();
		}
	}

}
