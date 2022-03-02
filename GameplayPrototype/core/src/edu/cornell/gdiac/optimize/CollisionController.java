/*
 * CollisionController.java
 *
 * This controller implements basic collision detection as described in
 * the instructions.  All objects in this game are treated as circles,
 * and a collision happens when circles intersect.
 *
 * This controller is EXTREMELY inefficient.  To improve its performance,
 * you will need to use collision cells, as described in the instructions.
 * You should not need to modify any method other than the constructor
 * and processCollisions.  However, you will need to add your own methods.
 *
 * This is the only file that you need to modify as the first part of
 * the lab. 
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.optimize.entity.*;

import static edu.cornell.gdiac.optimize.Environment.ObjectType.*;

/**
 * Controller implementing simple game physics.
 */
public class CollisionController {
	// These cannot be modified after the controller is constructed.
	// If these change, make a new constructor.
	/** Width of the collision geometry */
	private final float width;
	/** Height of the collision geometry */
	private final float height;
	
	// Cache objects for collision calculations
	private Vector2 temp1;
//	private Vector2 temp2;

	/** Number of cells along x-axis of screen */
	private int cellsx;
	/** Number of cells along y-axis of screen */
	private int cellsy;
	/** Cells to check collision in */
	private Array<Array<GameObject>> cells;
	
	/// ACCESSORS
	
	/**
	 * Returns width of the game window (necessary to detect out of bounds)
	 *
	 * @return width of the game window
	 */
	public float getWidth() { return width; }
	
	/**
	 * Returns height of the game window (necessary to detect out of bounds)
	 *
	 * @return height of the game window
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * Creates a CollisionController for the given screen dimensions and collision check cells.
	 *
	 * @param width   Width of the screen
	 * @param height  Height of the screen
	 * @param cellsx  Number of cells along x-axis of screen
	 * @param cellsy  Number of cells along y-axis of screen
	 *
	 */
	public CollisionController(float width, float height, int cellsx, int cellsy) {
		this.width = width;
		this.height = height;

		// Initialize cache objects
		temp1 = new Vector2();
		this.cellsx=cellsx;
		this.cellsy=cellsy;
		this.cells = new Array<>(cellsx * cellsy);
		for (int i =0; i< cellsx * cellsy; i++){
			this.cells.add(new Array<GameObject>());
		}
//		temp2 = new Vector2();
	}
	
	/**
	 * Creates a CollisionController for the given screen dimensions.
	 *
	 * @param width   Width of the screen 
	 * @param height  Height of the screen 
	 */
	public CollisionController(float width, float height) {
		this(width, height, 100, 100);
	}

	/* IMPORTANT: All game objects are separated into moving objects and static environment,
	to reduce the burden of checking for collision between static objects.  */
	// TODO: It is efficient right now to O(n) through all object to find the player and compute collision between
	// TODO: the player and others. However, since we are implementing enemies, we should follow the Lab3 design where
	// TODO: all possible collision between object are checked but only those meaningful are handled. -- Leo
	/**
	 * Check for collision between player and dynamic objects (should update to check inter-dynamic obj collisions)
	 *
	 * @param objects List of live objects to check
	 * @param player pointer to player (should be removed in the future)
	 * @param total_time  time used to calculate big wave in Technical prototype
	 */
	public void processCollisions(Array<GameObject> objects, Ship player, int total_time) {
		// assert player != null, avoid null pointer exception
		if(player == null){
			return;
		}
		remakeCells(objects);
		// Process player bounds
		handleBounds(player);

		// For each dynamic object, check for collisions with the player (except the player)
//		for (GameObject o : objects) {
//			if (o != player) {
//				handleCollision(player, o);
//			}
//		}
		calcCellCollisions(player);
	}

	// TODO: someone add documentation for these please

	public void remakeCells(Array<GameObject> objects){
		for (Array<GameObject> cell : cells){
			cell.clear();
		}
		for (GameObject object : objects){
			int row = Math.max(Math.min((int) Math.floor(object.getY() / height * cellsy), cellsy - 1), 0);
			int col = Math.max(Math.min((int) Math.floor(object.getX() / width * cellsx), cellsx - 1), 0);
			if (!object.isDestroyed())
				cells.get(row * cellsx + col).add(object);
		}
	}

	public void calcCellCollisions(Ship player){
		for (int i = 0; i < cells.size; i++){
			Array<GameObject> cell = cells.get(i);
			Array<Array<GameObject>> checkCells = new Array<>();
			checkCells.add(cell);
			// dont add it if it's on the respective boundary
			if ((i + 1) % cellsx != 0)
				checkCells.add(cells.get(i + 1));
			if (i % cellsx != 0)
				checkCells.add(cells.get(i - 1));
			if (i + cellsx < cells.size)
				checkCells.add(cells.get(i + cellsx));
			if (i - cellsx > 0)
				checkCells.add(cells.get(i - cellsx));
			for (int k = 0; k < checkCells.size; k++){
				Array<GameObject> checkCell = checkCells.get(k);
				for (GameObject checkObject : checkCell){
					if (checkObject != player) {
						handleCollision(player, checkObject);
					}
				}
			}

			// TODO: Uncomment this for when we do object object collisions
//			for (int j = 0; j < cell.size; j++){
//				GameObject object = cell.get(j);
//				for (int k = 0; k < checkCells.size; k++){
//					Array<GameObject> checkCell = checkCells.get(k);
//					for (GameObject checkObject : checkCell){
//						if (checkObject != player)
//							processCollision(object, checkObject);
//					}
//				}
//			}

		}
	}

	// TODO: decide and implement how we will handle collisions between two game objs -> does wood colliding do anything?

	/** handleCollision overload to find correct call
	 * @param obj1 - obj1 to check collisions for
	 * @param obj2   - obj2 to check collisions for */
	private void handleCollision(GameObject obj1, GameObject obj2) {
		switch (obj1.getType()){
			case SHIP:
				handleCollision((Ship) obj1, obj2);
				break;
			default:
				throw new IllegalStateException("Unrecognized GameObject Type: " + obj1.getType());
		}

	}


	/** handleCollision overload to find correct call
	 * @param player - the ship/player
	 * @param obj   - the collision object */
	private void handleCollision(Ship player, GameObject obj) {
		switch (obj.getType()){
			case WOOD:
				handleCollision(player, (Wood)obj);
				break;
			case TARGET:
				handleCollision(player, (Target)obj);
				break;
			case ENEMY:
				handleCollision(player, (Enemy)obj);
				break;
			default:
				throw new IllegalStateException("Unrecognized GameObject Type: " + obj.getType());
		}

	}

	/**
	 * Check for collision between player and static objects
	 * @param envs List of env element to check
	 * @param player pointer to player (should be removed in the future)
	 **/
	public void processCollisions(Array<Environment> envs, Ship player){
		// assert player != null, avoid null pointer exception
		if(player == null){
			return;
		}

		// For each static env object, check for collisions with the player
		for(Environment e : envs){
			if(e.getType() == OBSTACLE){
				handleCollision(player, (Obstacle)e);
			}else if(e.getType() == CURRENT){
				handleCollision(player, (Current)e);
			}
		}
	}

	/** handel the collision between ship and wood, wood get destroyed and ship get extended life
	 * @param player - the ship to extend life
	 * @param wood   - the wood to destroy */
	private void handleCollision(Ship player, Wood wood) {
		if (player.isDestroyed() || wood.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(wood.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() + wood.getRadius()) {
			return;
		}

		// Destroy wood
		wood.setDestroyed(true);
	}

	/** destroy the target and declare the player win when the player clash the target */
	private void handleCollision(Ship player, Target t){
		if (player.isDestroyed() || t.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(t.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() /*+ t.getRadius()*/ ) {
			return;
		}

		// Destroy Target
		t.setDestroyed(true);

	}

	/** push the player away from the rock to the direction of coming
	 *  pre-assumption & post-condition: rock or any environment will never be destroyed or moved */
	private void handleCollision(Ship player, Obstacle o){
		if (player.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(o.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() /*+ o.getRadius()*/ ) {
			return;
		}

		// push the player away from rock
		player.getPosition().add(player.last_movement.scl(-4));

	}

	/** Handle the collision between player and current. Push the player toward the direction of the current
	 * pre-assumption & post-condition: current or any environment will never be destroyed or moved */
	private void handleCollision(Ship player, Current c){
		if (player.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(c.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius() + c.getRadius() ) {
			return;
		}

		player.getPosition().add(c.getDirectionVector());
	}

	/** Handle the collision between player and enemy */
	private void handleCollision(Ship player, Enemy e){
		if (player.isDestroyed() || e.isDestroyed()) {
			return;
		}

		temp1.set(player.getPosition()).sub(e.getPosition());
		float dist = temp1.len();

		// Too far away
		if (dist > player.getRadius()  /* + e.getRadius() */ ) {
			return;
		}

		// destroy the player
		//TODO: this is problematic because it shows that ship's health is not coupled with the ship itself.
		// currently we cannot easily set the ship's health from here since it's in GameplayController. refactor?
		player.setDestroyed(true);
	}

	/**
	 * Check a bullet for being out-of-bounds.
	 * both x and y-axis are implemented
	 *
	 * @param sh Ship to check 
	 */
	private void handleBounds(Ship sh) {
		// Do not let the ship go off-world on both-axis: x
		if (sh.getX() <= sh.getRadius()) {
			sh.setX(sh.getRadius());
		} else if (sh.getX() >= getWidth() - sh.getRadius()) {
			sh.setX(getWidth() - sh.getRadius());
		}

		// Do not let the ship go off-world on both-axis: y
		if (sh.getY() <= sh.getRadius()) {
			sh.setY(sh.getRadius());
		} else if (sh.getY() >= getHeight() - sh.getRadius()) {
			sh.setY(getHeight() - sh.getRadius());
		}
	}
}