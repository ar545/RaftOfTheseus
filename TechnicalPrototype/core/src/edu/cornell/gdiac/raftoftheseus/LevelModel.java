package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.PooledList;

public class LevelModel {


    // TODO: class Raft/Player
    private GameObject raft;
    // TODO: class Goal/Target
    private GameObject goal;
    public AssetDirectory directory;
    JsonValue level_data;
    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;

    /** All the objects in the world. */
    private PooledList<GameObject> objects  = new PooledList<GameObject>();
    /** Queue for adding objects */
    private PooledList<GameObject> addQueue = new PooledList<GameObject>();

    public static final Vector2 NO_GRAVITY = new Vector2(0f ,0f );


    public GameObject getPlayer() {
        return raft;
    }

    public GameObject getGoal() {
        return goal;
    }


    public PooledList<GameObject> getObjects() {
        return objects;
    }

    public PooledList<GameObject> getAddQueue() {
        return addQueue;
    }


    /**
     * Returns true if the object is in bounds.
     *
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     *
     * @return true if the object is in bounds.
     */
    public boolean inBounds(GameObject obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
        return horiz && vert;
    }

    /**
     *
     * Adds a physics object in to the insertion queue.
     *
     * Objects on the queue are added just before collision processing.  We do this to
     * control object creation.
     *
     * param obj The object to add
     */
    public void addQueuedObject(GameObject obj) {
        assert inBounds(obj) : "Object is not in bounds";
        addQueue.add(obj);
    }

    /**
     * Immediately adds the object to the physics world
     *
     * param obj The object to add
     */
    protected void addObject(GameObject obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        for(GameObject obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();
        objects = null;
        addQueue = null;
        bounds = null;
        scale  = null;
        world  = null;
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        for(GameObject obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();

        world = new World(NO_GRAVITY,false);

    }


    /** Precondition: gameObject list has been cleared. */
    public void populateLevel() {
        addCurrent(level_data.get("current"), objects);
        addWood(level_data.get("wood"), objects);
        addRocks(level_data.get("rocks"), objects);
        addObject(addRaft(level_data.get("raft")));
        addObject(addGoal(level_data.get("goal")));
    }

    public void loadLevel(int level_int){
        level_data = directory.getEntry("levels:" + level_int, JsonValue.class);
        setWorld(level_data.get("world"), world);
        populateLevel();
    }

    private GameObject addGoal(JsonValue goal) {
        return null;
    }

    private GameObject addRaft(JsonValue raft) {
        return null;
    }

    private void addRocks(JsonValue rocks, PooledList<GameObject> objs) {
        for (JsonValue rock: rocks) {
//            GameObject this_rock = new Rock(rock.get("x"), rock.get("y"));
//            addObject(this_rock);
        }
    }

    private void addWood(JsonValue woods, PooledList<GameObject> objs) {
        for (JsonValue wood: woods) {
//            GameObject this_wood = new Wood(wood.get("x"), wood.get("y"), wood.get("value"));
//            addObject(this_wood);
        }
    }

    private void addCurrent(JsonValue currents, PooledList<GameObject> objs) {
        for (JsonValue current : currents) {
//            GameObject this_current = new Current(current.get("x"), current.get("y"), current.get("direction"), current.get("speed"));
//            addObject(this_current);
        }
    }

    private void setWorld(JsonValue world, World world1) {

    }
}
