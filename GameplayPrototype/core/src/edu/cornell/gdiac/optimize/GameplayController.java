/*
 * GameplayController.java
 *
 * For many of you, this class will seem like the most unusual one in the entire project.  
 * It implements a lot of functionality that looks like it should go into the various 
 * GameObject subclasses. However, a lot of this functionality involves the creation or
 * destruction of objects.  We cannot do this without a lot of cyclic dependencies, 
 * which are bad.
 *
 * You will notice that gameplay-wise, most of the features in this class are 
 * interactions, not actions. This demonstrates why a software developer needs to 
 * understand the difference between these two.  
 *
 * You will definitely need to modify this file in Part 2 of the lab. However, you are
 * free to modify any file you want.  You are also free to add new classes and assets.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.Texture;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.optimize.entity.*;
import com.badlogic.gdx.math.Vector2;

/**
 * Controller to handle gameplay interactions.
 * </summary>
 * <remarks>
 * This controller also acts as the root class for all the models.
 */
public class GameplayController {

	// Graphics assets for the entities
	/** Texture for all ships, as they look the same */
	private Texture raftTexture;
	/** Texture for the ocean tiles */
	private Texture oceanTexture;
	/** Texture for wood pieces that represent single pile of log */
	private Texture woodTexture;
	/** Texture for wood pieces that represents double pile of logs */
	private Texture doubleTexture;
	/** Texture for all target, as they look the same */
	private Texture targetTexture;
	/** Texture for all rock, as they look the same */
	private Texture rockTexture;
	/** Texture for current placeholder: texture alas in future */
	private Texture currentTexture;
	/** Texture for current placeholder: texture alas in future */
	private Texture enemyTexture;

	/** Reference to player */
	private Ship player;

	/** Reference to target */
	private Target target;

	/** Reference to tile grid */
	private Grid grid;

	// List of objects with the garbage collection set.
	/** The currently active object */
	private Array<GameObject> objects;
	/** The backing set for garbage collection */
	private Array<GameObject> backing;
	/** The environments */
	private Array<Environment> envs;

	/** Health: increase player health when wood is destroyed */
	protected float player_health;
	/** The position where the player dead */
	private Vector2 player_dead_position;
	/** Maximum player health */
	protected static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
	/** initial player health */
	protected static final float INITIAL_PLAYER_HEALTH = 20.0f;

	/**
	 * Creates a new GameplayController with no active elements.
	 */
	public GameplayController() {
		player = null;
		target = null;
		objects = new Array<GameObject>();
		backing = new Array<GameObject>();
		envs = new Array<Environment>();
		player_health = INITIAL_PLAYER_HEALTH;
		// TODO replace with data centric constructor
		grid = new Grid(24, 20);
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
		raftTexture = directory.getEntry("raft", Texture.class);
		oceanTexture = directory.getEntry("water_tile", Texture.class);
		woodTexture = directory.getEntry("wood", Texture.class);
		doubleTexture = directory.getEntry("double", Texture.class);
		targetTexture = directory.getEntry("target", Texture.class);
		rockTexture = directory.getEntry("rock", Texture.class);
		currentTexture = directory.getEntry("current", Texture.class);
		enemyTexture = directory.getEntry("enemy", Texture.class);
	}

	/**
	 * Returns the list of the currently active (not destroyed) game objects
	 *
 	 * As this method returns a reference and Lists are mutable, other classes can 
 	 * technical modify this list.  That is a very bad idea.  Other classes should
	 * only mark objects as destroyed and leave list management to this class.
	 *
	 * @return the list of the currently active (not destroyed) game objects
	 */
	public Array<GameObject> getObjects() {
		return objects;
	}

	/**
	 * Returns the list of the environments objects (currents, rocks, and target)
	 *
	 * @return the list of the game environments objects
	 */
	public Array<Environment> getEnvs() {
		return envs;
	}

	/**
	 * Returns a reference to the currently active player.
	 *
	 * @return a reference to the currently active player.
	 */
	public Ship getPlayer() {
		return player;
	}

	/**
	 * Returns true if the currently active player is alive.
	 *
	 * This property needs to be modified if you want multiple players.
	 *
	 * @return true if the currently active player is alive.
	 */
	public boolean isAlive() {
		return player != null;
	}

	/**
	 * @return a reference to the current grid;
	 */
	public Grid getGrid() {
		return grid;
	}
	
	/**
	 * Starts a new game.
	 *
	 * This method creates a single player, and creates driftwood in random positions.
	 *
	 * @param width Canvas width
	 * @param height Canvas height
	 */
	public void start(float width, float height) {
		// Create the player's ship to objects
		player = new Ship();
		player.setTexture(raftTexture);

		// TODO: these values should be defined in level json file. Replace these after Technical prototype
		player.getPosition().set(getPlayer().getRadius(), getPlayer().getRadius()); // Initial player position: down-left
		player_health = GameplayController.INITIAL_PLAYER_HEALTH; // Initial player health: full health

		// Create the target to objects
		target = new Target();
		target.setTexture(targetTexture);

		// TODO: location of target should be in level json file. Replace these after Technical prototype
		int random_target_selection = RandomController.rollInt(0, 2); // Select one of the three corners
		if(random_target_selection == 0){
			target.getPosition().set(target.getRadius(), height - target.getRadius()); // Top-right corner
		}else if(random_target_selection == 1){
			target.getPosition().set(width - target.getRadius(), target.getRadius()); // Down-right corner
		}else{
			target.getPosition().set(width - target.getRadius(), height - target.getRadius()); // Top-left corner
		}

		// add driftwood to objects
		// TODO: Replace these after Technical prototype. location of wood should be in level json file
		for (int ii = 0; ii < 32; ii ++) {
			Wood wood;
			if(RandomController.rollInt(0, 1) == 0){
				wood = new Wood(true);
				wood.setTexture(doubleTexture);
			}else{
				wood = new Wood(false);
				wood.setTexture(woodTexture);
			}
			wood.getPosition().set((float)(width*Math.random()), (float)(height*Math.random()));
			objects.add(wood);
		}

		// Create the rock to environment
		// TODO: Replace these after Technical prototype. location of rock should be in level json file
		Obstacle rock = new Obstacle();
		rock.setTexture(rockTexture);
		rock.getPosition().set(width/2, height/2); // center of map
		envs.add(rock);

		// Create some currents to environment
		// TODO: Replace these after Technical prototype. location of current should be in level json file
		Current[] curs = new Current[4];
		for(int ii = 0; ii < 4; ii ++){
			curs[ii] = new Current();
			curs[ii].setTexture(currentTexture);
			envs.add(curs[ii]);
		}
		curs[0].getPosition().set(width/4, height/4);
		curs[1].getPosition().set(3 * width/4, height/4);
		curs[2].getPosition().set(width/4, 3 * height/4);
		curs[3].getPosition().set(3 * width/4, 3 * height/4);

		// Add some enemy to the environment
		// TODO: Replace these after Technical prototype. location of current should be in level json file
		Enemy e = new Enemy();
		e.getPosition().set((float)(width*Math.random()), (float)(height*Math.random()));
		e.setTexture(enemyTexture);
		objects.add(e);

		// target must be in the object list
		objects.add(target);

		// Player must be in object list.
		objects.add(player);

		// set grid Textures
		// TODO Find better way to set textures?
		grid.setOceanTexture(oceanTexture);

	}

	/**
	 * Resets the game, deleting all objects.
	 */
	public void reset() {
		player = null;
		target = null;
		objects.clear();
		envs.clear();
		backing.clear();
	}

	/**
	 * Garbage collects all deleted objects.
	 *
	 * This method works on the principle that it is always cheaper to copy live objects
	 * than to delete dead ones.  Deletion restructures the list and is O(n^2) if the 
	 * number of deletions is high.  Since Add() is O(1), copying is O(n).
	 */
	public void garbageCollect() {
		// INVARIANT: backing and objects are disjoint
		for (GameObject o : objects) {
			if (o.isDestroyed()) {
				destroy(o);
			} else {
				backing.add(o);
			}
		}

		// Swap the backing store and the objects.
		// This is essentially stop-and-copy garbage collection
		Array<GameObject> tmp = backing;
		backing = objects;
		objects = tmp;
		backing.clear();
	}
	
	/**
	 * Process specialized destruction functionality
	 *
	 * Some objects do something special (e.g. explode) on destruction. That is handled 
	 * in this method.
	 *
	 * Notice that this allocates memory to the heap.  If we were REALLY worried about 
	 * performance, we would use a memory pool here.
	 *
	 * @param o Object to destroy
	 */
	protected void destroy(GameObject o) {
		switch(o.getType()) {
		case SHIP:
			player_dead_position = player.getPosition();
			player = null;
			break;
		case WOOD:
			Wood w = (Wood) o;
			player_health += w.getWood();
			if(player_health > MAXIMUM_PLAYER_HEALTH){
				player_health = MAXIMUM_PLAYER_HEALTH;
			}
			break;
		case TARGET:
			target = null;
			break;
		case ENEMY:
		default:
			break;
		}
	}
	
	/**
	 * Resolve the actions of all game objects (player and shells)
	 *
	 * You will probably want to modify this heavily in Part 2.
	 *
	 * @param input  Reference to the input controller
	 * @param delta  Number of seconds since last animation frame
	 */
	public void resolveActions(InputController input, float delta) {
		// If player is alive and not reach the target Process the player
		if (player != null && target != null) {
			resolvePlayer(input,delta);
		}

		// If player is dead, kill the player
		if(player != null && player_health < 0){
			player.setDestroyed(true);
		}

		// Process the other (non-ship) objects.
		for (GameObject o : objects) {
			o.update(delta);
		}
	}

	/**
	 * Process the player's actions.
	 *
	 * Notice that firing bullets allocates memory to the heap.  If we were REALLY 
	 * worried about performance, we would use a memory pool here.
	 *
	 * @param input  Reference to the input controller
	 * @param delta  Number of seconds since last animation frame
	 */
	public void resolvePlayer(InputController input, float delta) {
		Vector2 movement = input.getMovement();
		player_health -= Math.abs(movement.len());
		player.setMovement(movement);
		player.update(delta);
	}

	/** @return float represent the health of the player */
	public float getProgress(){
		return player_health / MAXIMUM_PLAYER_HEALTH;
	}

	/** @return boolean represent if the player has won the game */
	public boolean isWin() {
		return target == null;
	}

	/** @return the player's last known position */
	public Vector2 getPlayerPosition() {
		if(player != null){
			return getPlayer().getPosition();
		}else{
			return player_dead_position;
		}
	}

	/** @return the player's star level */
	public int getStar(){
		if(player_health > 70){
			return 3;
		}else if(player_health > 25){
			return 2;
		}else{
			return 1;
		}
	}
}