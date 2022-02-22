package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

import edu.cornell.gdiac.assets.AssetDirectory;

/**
 * Controller to handle gameplay interactions.
 * </summary>
 * <remarks>
 * This controller also acts as the root class for all the models.
 */
public class GameplayController {
    // Graphics assets for the entities
    /** Texture for all ships, as they look the same */
    private Texture shipTexture;
    /** Texture for all driftwood, as they look the same */
    private Texture woodTexture;

    /** Reference to player (need to change to allow multiple players) */
    private Ship player;

    // List of objects with the garbage collection set.
    /** The currently active object */
    private Array<GameObject> objects;
    /** The backing set for garbage collection */
    private Array<GameObject> backing;

    /**
     * Creates a new GameplayController with no active elements.
     */
    public GameplayController() {
        player = null;
        objects = new Array<GameObject>();
        backing = new Array<GameObject>();
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
        shipTexture = directory.getEntry("ship", Texture.class);
        woodTexture = directory.getEntry("wood", Texture.class);
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
     * Returns a reference to the currently active player.
     *
     * This property needs to be modified if you want multiple players.
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
     * Starts a new game.
     * @param height game window height
     * @param width game window width
     */
    public void start(float width, float height) {
        // Create the player's ship
        player = new Ship(width*0.67f, height*0.5f);
        player.setTexture(shipTexture);

        // add 1 wood
        Wood someWood = new Wood(width*0.33f, height*0.5f);
		someWood.setTexture(woodTexture);

        // Add objects to object list.
        objects.add(player);
        objects.add(someWood);
    }

    /**
     * Resets the game, deleting all objects.
     */
    public void reset() {
        player = null;
        objects.clear();
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
                player = null;
                break;
            case WOOD:
                // TODO: Stuff that happens when wood is destroyed (i.e. collected)

                break;
            default:
                break;
        }
    }

    /**
     * Resolve the actions of all game objects.
     *
     * @param input  Reference to the input controller
     */
    public void resolveActions(InputController input) {
        // Process the player
        if (player != null) {
            resolvePlayer(input);
        }

        // Process the other (non-ship) objects.
        for (GameObject o : objects) {
            o.update();
        }
    }

    /**
     * Process the player's actions.
     *
     * @param input  Reference to the input controller
     */
    public void resolvePlayer(InputController input) {
        player.move(input.getForward(), input.getTurn());
        player.update();
    }
}