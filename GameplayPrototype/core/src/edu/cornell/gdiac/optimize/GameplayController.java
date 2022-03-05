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
	private Texture currentTextures[];
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

	/** The position where the player dead */
	private Vector2 player_dead_position;
	/** The player's last known health value, if the ship has been deleted */
	private float player_cached_health;

	/**
	 * Creates a new GameplayController with no active elements.
	 */
	public GameplayController() {
		player = null;
		target = null;
		objects = new Array<GameObject>();
		backing = new Array<GameObject>();
		envs = new Array<Environment>();
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
		currentTextures = new Texture[] {
				directory.getEntry("east_current", Texture.class),
				directory.getEntry("west_current", Texture.class),
				directory.getEntry("north_current", Texture.class),
				directory.getEntry("south_current", Texture.class)
		};
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
	public void start(int width, int height, float tile_size, int wood_amount) {
		grid = new Grid(width, height, tile_size);

		// Create the player's ship to objects
		player = new Ship();
		player.setTexture(raftTexture);

		// TODO: these values should be defined in level json file. Replace these after Technical prototype
		player.getPosition().set(getPlayer().getRadius(), getPlayer().getRadius()); // Initial player position: down-left

		// Create the target to objects
		target = new Target();
		target.setTexture(targetTexture);

		// TODO: location of target should be in level json file. Replace these after Technical prototype
		int random_target_selection = RandomController.rollInt(0, 2); // Select one of the three corners
		Vector2 tilePosition;
		if(random_target_selection == 0){
			tilePosition = new Vector2(0, height-1); // Top-right corner
		}else if(random_target_selection == 1){
			tilePosition = new Vector2(width-1, 0); // Down-right corner
		}else{
			tilePosition = new Vector2(width-1, height-1); // Top-left corner
		}
		positionObjectOnTile(target, tilePosition, tile_size);

		// add driftwood to objects
		// TODO: Replace these after Technical prototype. location of wood should be in level json file
		for (int ii = 0; ii < wood_amount; ii ++) {
			Wood wood;
			if(RandomController.rollInt(0, 1) == 0){
				wood = new Wood(true);
				wood.setTexture(doubleTexture);
			}else{
				wood = new Wood(false);
				wood.setTexture(woodTexture);
			}
			tilePosition.set((int)(width*Math.random()), (int)(height*Math.random()));
			positionObjectOnTile(wood, tilePosition, tile_size);
			objects.add(wood);
		}

		// Create the rock to environment
		// TODO: Replace these after Technical prototype. location of rock should be in level json file
		Obstacle rock = new Obstacle();
		rock.setTexture(rockTexture);
		tilePosition.set((int)(width*0.5f), (int)(height*0.5f)); // center of map
		positionObjectOnTile(rock, tilePosition, tile_size);
		envs.add(rock);

		// Create some currents to environment
		// TODO: Replace these after Technical prototype. location of current should be in level json file
		int sep = 3;// creates currents in a loop which is 3 tiles away from the border
		int clockwise = RandomController.rollInt(0, 1);// controls direction of loop
		int num_currents = (width-2*sep)*(height-2*sep)-(width-2*sep-2)*(height-2*sep-2);
		if(num_currents >= 4) {
			if (clockwise == 1) {
				createLineOfCurrent(sep, sep, sep, height-sep-2, tile_size);
				createLineOfCurrent(sep, height-sep-1, width-sep-2, height-sep-1, tile_size);
				createLineOfCurrent(width-sep-1, height-sep-1, width-sep-1, sep+1, tile_size);
				createLineOfCurrent(width-sep-1, sep, sep+1, sep, tile_size);
			} else {
				createLineOfCurrent(width-sep-1, sep, width-sep-1, height-sep-2, tile_size);
				createLineOfCurrent(width-sep-1, height-sep-1, sep+1, height-sep-1, tile_size);
				createLineOfCurrent(sep, height-sep-1, sep, sep+1, tile_size);
				createLineOfCurrent(sep, sep, width-sep-2, sep, tile_size);
			}
		}

		// Add some enemy to the environment
		// TODO: Replace these after Technical prototype. location of current should be in level json file
		Enemy e = new Enemy(player);
		tilePosition.set((int)(width*Math.random()), (int)(height*Math.random()));
		positionObjectOnTile(e, tilePosition, tile_size);
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
	 * Creates a line of current tiles from the tile (x0, y0) to the tile (x1, y1) and adds them to envs.
	 * Automatically gives currents the right direction.
	 * Requires that either x0==x1 or y0==y1, but not both. Otherwise, nothing will happen.
	 */
	private void createLineOfCurrent(int x0, int y0, int x1, int y1, float tile_size) {
		if (x0 == x1 && y0 != y1) {
			int tilex = x0;
			Current.Direction dir = (y1 > y0) ? Current.Direction.NORTH : Current.Direction.SOUTH;
			for (int i = 0; i < Math.abs(y1-y0)+1; i++) {
				int tiley = y0 + i * (y1 > y0 ? 1 : -1);
				Current cur = new Current(dir);
				cur.setTexture(currentTextures[0]);// one texture for all directions
				positionObjectOnTile(cur, new Vector2(tilex, tiley), tile_size);
				envs.add(cur);
			}
		} else if (y0 == y1 && x0 != x1) {
			int tiley = y0;
			Current.Direction dir = (x1 > x0) ? Current.Direction.EAST : Current.Direction.WEST;
			for (int i = 0; i < Math.abs(x1-x0)+1; i++) {
				int tilex = x0 + i * (x1 > x0 ? 1 : -1);
				Current cur = new Current(dir);
				cur.setTexture(currentTextures[0]);// one texture for all directions
				positionObjectOnTile(cur, new Vector2(tilex, tiley), tile_size);
				envs.add(cur);
			}
		} else {
			System.out.println("The method createLineOfCurrent was used improperly and no currents were created.");
		}
	}

	/**
	 * Sets the position of the given object to be centered on the given tile (in integer tile coords).
	 * This modifies both objects.
	 */
	private void positionObjectOnTile(Environment obj, Vector2 tile, float tile_size) {
		obj.getPosition().set(tile.add(0.5f, 0.5f).scl(tile_size));
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
			if (player != null)
				player.addHealth(w.getWood());
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
	 * Resolve the actions of all game objects
	 *
	 * @param input  Reference to the input controller
	 * @param delta  Number of seconds since last animation frame
	 */
	public void resolveActions(InputController input, float delta) {
		if (isAlive()) {
			if (!isWin()) { // If player is alive and hasn't reached the target,
				resolvePlayer(input,delta); // process the player
				if (player.getHealth() <= 0) // destroy player if health is 0
					player.setDestroyed(true);
				// Process the other (non-player) objects.
				for (GameObject o : objects) {
					if (o.getType() != Environment.ObjectType.SHIP)
						o.update(delta);
				}
			}
			player_cached_health = player.getHealth(); // cache player health
		}
	}

	/**
	 * Process the player's actions.
	 *
	 * @param input  Reference to the input controller
	 * @param delta  Number of seconds since last animation frame
	 */
	public void resolvePlayer(InputController input, float delta) {
		Vector2 movement = input.getMovement();
		player.setMovement(movement);
		player.update(delta);
	}

	/** @return float represent the health of the player */
	public float getProgress(){
		return getPlayerHealth()/player.MAXIMUM_PLAYER_HEALTH;
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

	/** @return the player's last known health */
	public float getPlayerHealth() {
		if(player != null){
			return player.getHealth();
		}else{
			return player_cached_health;
		}
	}

	/** @return the player's star level */
	public int getStar(){
		float h = getPlayerHealth();
		if (h > 70) {
			return 3;
		} else if (h > 25) {
			return 2;
		} else {
			return 1;
		}
	}
}