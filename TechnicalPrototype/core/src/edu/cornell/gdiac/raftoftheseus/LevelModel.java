package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.PooledList;

public class LevelModel {


    // TODO: class Raft/Player
    /** The player of the level */
    private GameObject raft;
    // TODO: class Goal/Target
    /** The goal of the level */
    private GameObject goal;
    /** Reference to the game assets directory */
    public AssetDirectory directory;
    JsonValue level_data;
    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;
    /** Representing the length and width of a single gird unit on the board */
    protected Vector2 grid_size;
    /** Vector 2 holding the temp position vector for the game object to create */
    private Vector2 compute_temp = new Vector2(0, 0);

    /** All the objects in the world. */
    private PooledList<GameObject> objects  = new PooledList<GameObject>();
    /** Queue for adding objects */
    private PooledList<GameObject> addQueue = new PooledList<GameObject>();
    /** Top-down game with no gravity */
    public static final Vector2 NO_GRAVITY = new Vector2(0f ,0f );

    /** Constructor call for this singleton class */
    public LevelModel(){}

    /** get the reference to the player avatar */
    public GameObject getPlayer() { return raft; }

    /** get a reference to the goal */
    public GameObject getGoal() { return goal; }

    /** get the objects (list) of the world */
    public PooledList<GameObject> getObjects() { return objects; }

    public PooledList<GameObject> getAddQueue() { return addQueue; }


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

    /** Load the level representing by the parameter level_int.
     * Read the level from the json file and call corresponding functions.
     * Precondition: gameObject list has been cleared.
     *
     * @param level_int an integer representing the level selection, i.e. which json file to read from. */
    public void loadLevel(int level_int){
        level_data = directory.getEntry("levels:" + level_int, JsonValue.class);
        setBound(level_data.get("bounds"));
        populateLevel();
    }

    /** Set the physical boundary of the level. This boundary will be enforced when adding objects */
    private void setBound(JsonValue world_data) {
        int num_col = world_data.getInt("num_col", 1);
        int num_row = world_data.getInt("num_row", 1);
        float col_width = world_data.getFloat("col_width", 1f);
        float row_height = world_data.getFloat("row_height", 1f);
        this.grid_size = new Vector2(col_width, row_height);
        this.bounds = new Rectangle(0,0,col_width * num_col,row_height * num_row);
        this.scale = new Vector2(world_data.getFloat("draw_scale_x", 1), world_data.getFloat("draw_scale_y", 1));

    }

    /** Populate the level with the game objects
     * Precondition: gameObject list has been cleared. */
    public void populateLevel() {
        addCurrent(level_data.get("current"));
        addWood(level_data.get("wood"));
        addRocks(level_data.get("rocks"));
        addRaft(level_data.get("raft"));
        addGoal(level_data.get("goal"));

    }

    /** Add Goal Objects to the world, using the Json value for goal.
     * @param JSGoal the JS value that represents the goal */
    private void addGoal(JsonValue JSGoal) {
        computePosition(JSGoal.getInt("x", 0), JSGoal.getInt("y", 0));
//        GameObject this_goal = new Goal(compute_temp);
//        addObject(this_goal);
//        goal = this_goal;
    }

    /** Add Raft Objects to the world, using the Json value for raft
     * @param JSRaft the JS value that represents the raft */
    private void addRaft(JsonValue JSRaft) {
        computePosition(JSRaft.getInt("x", 0), JSRaft.getInt("y", 0));
//        GameObject this_raft = new Raft(compute_temp, JSRaft.getInt("speed", 1));
//        addObject(this_raft);
//        raft = this_raft;
    }

    /** Add Goal Objects to the world, using the Json value for goal
     * @param rocks the JS value that represents the goal */
    private void addRocks(JsonValue rocks) {
        for (JsonValue rock: rocks) {
            computePosition(rock.getInt("x", 0), rock.getInt("y", 0));
//            GameObject this_rock = new Rock(compute_temp);
//            addObject(this_rock);
        }
    }

    /** Add Goal Objects to the world, using the Json value for goal
     * @param woods the JS value that represents the goal */
    private void addWood(JsonValue woods) {
        for (JsonValue wood: woods) {
            computePosition(wood.getInt("x", 0), wood.getInt("y", 0));
//            GameObject this_wood = new Wood(compute_temp, wood.getInt("value", 1));
//            addObject(this_wood);
        }
    }

    /** Add Goal Objects to the world, using the Json value for goal
     * @param currents the JS value that represents the goal */
    private void addCurrent(JsonValue currents) {
        for (JsonValue current : currents) {
            computePosition(current.getInt("x", 0), current.getInt("y", 0));
//            GameObject this_current = new Current(compute_temp, current.getString("direction"), current.getInt("speed", 1));
//            addObject(this_current);
        }
    }

    /** compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x_col the x grid value
     * @param y_row the y grid value */
    private void computePosition(int x_col, int y_row){
        compute_temp.x = ((float) x_col + 0.5f) * grid_size.x;
        compute_temp.y = ((float) y_row + 0.5f) * grid_size.y;
    }


}
