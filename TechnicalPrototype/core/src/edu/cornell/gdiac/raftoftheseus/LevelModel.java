package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.PooledList;

public class LevelModel {

    /*=*=*=*=*=*=*=*=*=* LEVEL CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Default width and height of a single grid in Box2d units */
    private static final float DEFAULT_GRID_EDGE_LENGTH = 3.0f;
    /** Default boundary width and height of a single grid in Box2d units */
    private static final float DEFAULT_BOUNDARY = 3.0f;
    /** Default num of rows in the map (y, height) */
    private static final int DEFAULT_GRID_ROW = 8;
    /** Default num of columns in the map (x, width) */
    private static final int DEFAULT_GRID_COL = 10;
    /** Representing the length and width of a single gird unit on the board */
    private static final Vector2 GRID_SIZE = new Vector2(DEFAULT_GRID_EDGE_LENGTH, DEFAULT_GRID_EDGE_LENGTH);
    /** Top-down game with no gravity */
    protected static final Vector2 NO_GRAVITY = new Vector2(0f ,0f );
    /** This is used as a level int representing restarting the level */
    protected static final int LEVEL_RESTART_CODE = -1;

    /*=*=*=*=*=*=*=*=*=* TILED CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Index of the representation of default in tile set texture */
    private static final int TILE_DEFAULT = 0;
    /** Index of the representation of background in tile set texture */
    private static final int TILE_BACKGROUND = 1;
    /** Index of the representation of rock in tile set texture */
    private static final int TILE_ROCK = 2;
    /** Index of the representation of default treasure in tile set texture */
    private static final int TILE_TREASURE = 3;
    /** Index of the representation of goal in tile set texture */
    private static final int TILE_GOAL = 4;
    /** Index of the representation of default current in tile set texture */
    private static final int TILE_CURRENT = 5;
    /** Index of the representation of start in tile set texture */
    private static final int TILE_START = 9;
    /** Index of the representation of enemy in tile set texture */
    private static final int TILE_ENEMY = 10;
    /** Index of the representation of default wood in tile set texture */
    private static final int TILE_WOOD = 11;
    /** Offset of north current in tile set index */
    private static final int TILE_NORTH = 0;
    /** Offset of east current in tile set index */
    private static final int TILE_EAST = 1;
    /** Offset of south current in tile set index */
    private static final int TILE_SOUTH = 2;
    /** Offset of west current in tile set index */
    private static final int TILE_WEST = 3;
    /** layer of environment */
    private static final int LAYER_ENV = 0;
    /** layer of collectables */
    private static final int LAYER_COL = 1;
    /** layer of enemies */
    private static final int LAYER_ENE = 2;

    /*=*=*=*=*=*=*=*=*=* LEVEL FIELDS *=*=*=*=*=*=*=*=*=*/
    /** The player of the level */
    private Raft raft;
    /** The goal of the level */
    private Goal goal;
    /** The wall of the level */
    private Wall this_wall;
    /** The vertices of the wall */
    private float[] polygonVertices;
    /** Reference to the game assets directory */
    private AssetDirectory directory;
    /** The read-in level data */
    private JsonValue level_data;
    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    private Rectangle bounds;
    /** The world scale */
    protected Vector2 scale = new Vector2(1, 1);
    /** The map size in grid */
    protected GridPoint2 map_size = new GridPoint2(DEFAULT_GRID_COL, DEFAULT_GRID_ROW);
    /** Vector 2 holding the temp position vector for the game object to create */
    private Vector2 compute_temp = new Vector2(0, 0);
    /** All the objects in the world. */
    private PooledList<GameObject> objects  = new PooledList<>();
    /** Queue for adding objects */
    private PooledList<GameObject> addQueue = new PooledList<>();
    /** All enemy objects in the world */
    private PooledList<Shark> enemies = new PooledList<>();

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
    /** Texture for all treasures */
    private Texture treasureTexture;
    /** Texture for all rock, as they look the same */
    private Texture rockTexture;
    /** Texture for current placeholder: texture alas in future */
    private Texture currentTexture;
    /** Texture for current placeholder: texture alas in future */
    private Texture enemyTexture;
    /** Texture for wall */
    private TextureRegion earthTile;

    /*=*=*=*=*=*=*=*=*=* INTERFACE: getter and setter *=*=*=*=*=*=*=*=*=*/
    /** get the reference to the player avatar */
    public Raft getPlayer() { return raft; }
    /** get a reference to the goal */
    public Goal getGoal() { return goal; }
    /** get the objects (list) of the world */
    public PooledList<GameObject> getObjects() { return objects; }
    /** get the enemies (list) of the world */
    public PooledList<Shark> getEnemies() { return enemies; }
    /***/
    public PooledList<GameObject> getAddQueue() { return addQueue; }
    /** set directory */
    protected void setDirectory(AssetDirectory directory) { this.directory = directory; }
    /** Get boundary wall vertices */
    public float[] getWallVertices() { return polygonVertices; }
    /** Get boundary wall drawscale */
    public Vector2 getWallDrawscale() { return this_wall.getDrawScale(); }
    /** Constructor call for this singleton class */
    public LevelModel(){}

    /**
     * Returns true if the object is in bounds.
     * This assertion is useful for debugging the physics.
     * @param obj The object to check.
     * @return true if the object is in bounds.
     */
    private boolean inBounds(GameObject obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
        return horiz && vert;
    }

    /** @return the bounds of this world in rectangle */
    public Rectangle bounds() {
        return bounds;
    }

    /** @return the height and width of bounds only
     * width: GRID_SIZE.x * map_size.x + 2 * DEFAULT_BOUNDARY,
     * height: GRID_SIZE.y * map_size.y + 2 * DEFAULT_BOUNDARY */
    public Vector2 boundsVector2(){
        return new Vector2(bounds.width, bounds.height);
    }

    /** Adds a physics object in to the insertion queue.
     * Objects on the queue are added just before collision processing.  We do this to
     * control object creation.
     * @param obj The object to add */
    protected void addQueuedObject(GameObject obj) {
        assert inBounds(obj) : "Object is not in bounds";
        addQueue.add(obj);
    }

    /** Immediately adds the object to the physics world
     * @param obj The object to add */
    protected void addObject(GameObject obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
    }

    /** Destroy if an object is a bullet and is out_of_bound. Could be extended to check for all objects
     * @param obj the object to check for bounds */
    protected void checkBulletBounds(GameObject obj){
        if(obj.getType() == GameObject.ObjectType.BULLET && !inBounds(obj)){
            obj.setDestroyed(true);
        }
    }

    /** Immediately adds the object to the physics world, adding it to the front of the list, so it gets draw the first
     * @param obj The environmental object (usually are currents only) to add */
    protected void addCurrentObject(Current obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(0, obj);
        obj.activatePhysics(world);
    }

    /** Immediately adds the object to the physics world and the enemy list
     * @param obj The enemy object to add */
    protected void addEnemyObject(Shark obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        enemies.add(obj);
    }

    /*=*=*=*=*=*=*=*=*=* Level selection: dispose, select, and reset *=*=*=*=*=*=*=*=*=*/

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        for(GameObject obj : objects) { obj.deactivatePhysics(world); }
        objects.clear();
        enemies.clear();
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
        for(GameObject obj : objects) { obj.deactivatePhysics(world); }
        objects.clear();
        enemies.clear();
        addQueue.clear();
        if (world != null)
            world.dispose();
        world = new World(NO_GRAVITY,false);

    }

    /** Load the level representing by the parameter level_int.
     * Read the level from the json file and call corresponding functions.
     * Precondition: gameObject list has been cleared.
     *
     * @param level_int an integer representing the level selection, i.e. which json file to read from. */
    public void loadLevel(int level_int){
        if(level_int != LEVEL_RESTART_CODE){
            // Load in new level
            level_data = directory.getEntry("level:"+level_int, JsonValue.class);

            // Read in the grid map size
            map_size.x = level_data.getInt("width", DEFAULT_GRID_COL);
            map_size.y = level_data.getInt("height", DEFAULT_GRID_ROW);

            // Reset boundary of world
            setBound();
        }
        // Add wall to the world
        computeWall(bounds.width, bounds.height);

        // Populate game objects
        populateLevel();
    }

    /** Calculate the world bounds base on the grid map. Set the physical boundary of the level and the world.
     * This boundary will be enforced when adding objects and checking bullets
     * Notice that the walls are not included in this boundary, i.e. all walls are out of bounds
     * To include the walls in the bounds, expand the size of rectangle by DEFAULT_BOUNDARY on each side */
    private void setBound() {
        this.bounds = new Rectangle(0,0,GRID_SIZE.x * map_size.x ,GRID_SIZE.y * map_size.y );
    }

    /**
     * Add four wall (static objects) surrounding the box2d world to not let object go off-screen
     * South wall rectangle: (0, 0, x+k, k)
     * West wall rectangle: (0, k, k, y+2*k)
     * North wall rectangle: (k, y+k, x+2*k, y+2*k)
     * East wall rectangle: (x+k, 0, x+2*k, y+k)
     * */
    private void computeWall(float x, float y) {
        generateRectangle(0f, 0f, x+ LevelModel.DEFAULT_BOUNDARY, LevelModel.DEFAULT_BOUNDARY); // south_wall
        generateRectangle(0, LevelModel.DEFAULT_BOUNDARY, LevelModel.DEFAULT_BOUNDARY, y+2* LevelModel.DEFAULT_BOUNDARY); // west_wall
        generateRectangle(LevelModel.DEFAULT_BOUNDARY, y+ LevelModel.DEFAULT_BOUNDARY,
                x+2* LevelModel.DEFAULT_BOUNDARY, y+2* LevelModel.DEFAULT_BOUNDARY); // north_wall
        generateRectangle(x+ LevelModel.DEFAULT_BOUNDARY, 0, x+2* LevelModel.DEFAULT_BOUNDARY, y+ LevelModel.DEFAULT_BOUNDARY); // east_wall
        float x2 = x+ LevelModel.DEFAULT_BOUNDARY;
        float y2 = y+ LevelModel.DEFAULT_BOUNDARY;
        polygonVertices = new float[] { x2, y2 };
    }

    /** Add Wall Objects to the world, using the Json value for goal.
     * The wall is shifted to the south-west by DEFAULT_BOUNDARY to overlap with the world origin */
    private void generateRectangle(float x1, float y1, float x2, float y2) {
        x1 += -DEFAULT_BOUNDARY;
        x2 += -DEFAULT_BOUNDARY;
        y1 += -DEFAULT_BOUNDARY;
        y2 += -DEFAULT_BOUNDARY;
        float[] polygonVertices = new float[] {x1, y1, x2, y1, x2, y2, x1, y2};
        this_wall = new Wall(polygonVertices);
        this_wall.setTexture(earthTile);
        addObject(this_wall);
    }

    /**
     * Populate the level with the game objects.
     * Precondition: gameObject list has been cleared. */
    private void populateLevel() {
        JsonValue layers = level_data.get("layers");
        JsonValue environment = layers.get(LAYER_ENV);
        JsonValue collectables = layers.get(LAYER_COL);
        JsonValue enemies = layers.get(LAYER_ENE);

        // Loop through all index: for(int index = 0; index < map_size.x * map_size.y; index++)
        for(int row_reversed = 0; row_reversed < map_size.y; row_reversed ++){
            int row = map_size.y - row_reversed - 1;
            for(int col = 0; col < map_size.x; col ++){
                int index = row_reversed * map_size.x + col;
                populateEnv(row, col, environment.get("data").getInt(index));
                populateCollect(row, col, collectables.get("data").getInt(index));
                populateEnemies(row, col, enemies.get("data").getInt(index));
                // TODO: if we populate the raft field before instantiating enemies,
                //  we can properly instantiate instead of putting null for target raft field
                //  see the Enemy this_enemy = new Enemy(compute_temp, null);
                //  on private void addEnemy(int row, int col, int enemy_type){}
                populateEnemiesRaftField();
            }
        }
    }

    /** This is a temporary function that help all enemies target the raft */
    private void populateEnemiesRaftField(){
        for (Shark shark : enemies){
            shark.setTargetRaft(raft);
        }
    }

    /** This is the level editor JSON parser that populate the enemy layer
     * @param row the row the enemy is in the world
     * @param col the column the enemy is in the world
     * @param tile_int whether this tile is an emery or the player */
    private void populateEnemies(int row, int col, int tile_int) {
        switch (tile_int){
            case TILE_ENEMY:
                addEnemy(row, col, 0);
                break;
            case TILE_START:
                addRaft(row, col);
                break;
            default:
                break;
        }
    }

    /** This is the level editor JSON parser that populate the collectable layer
     * @param row the row the collectable is in the world
     * @param col the column the collectable is in the world
     * @param tile_int whether this tile is a wood or treasure */
    private void populateCollect(int row, int col, int tile_int) {
        if(tile_int == TILE_TREASURE){
            addTreasure(row, col);
        } else if(tile_int >= TILE_WOOD){
            addWood(row, col, tile_int - TILE_WOOD + 1);
        }
    }

    /** This is the level editor JSON parser that populate the environment layer
     * @param row the row the environment element is in the world
     * @param col the column the environment element is in the world
     * @param tile_int whether this tile is a rock or a current or a goal */
    private void populateEnv(int row, int col, int tile_int) {
        if(tile_int == TILE_ROCK){
            addRock(row, col);
        }else if(tile_int == TILE_GOAL){
            addGoal(row, col);
        }else if(tile_int >= TILE_CURRENT && tile_int <= TILE_CURRENT + TILE_WEST){
            addCurrent(row, col, compute_direction(tile_int - TILE_CURRENT));
        }
    }

    /** Compute the direction of the current base on the level json input
     * @param i level json input
     * @return the direction of the current */
    private Current.Direction compute_direction(int i) {
        switch (i){
            case TILE_NORTH: return Current.Direction.NORTH;
            case TILE_SOUTH: return Current.Direction.SOUTH;
            case TILE_EAST: return Current.Direction.EAST;
            case TILE_WEST: return Current.Direction.WEST;
            default: return Current.Direction.NONE;
        }
    }

    /** Add Rock Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addRock(int row, int col) {
        computePosition(col, row);
        Rock this_rock = new Rock(compute_temp);
        this_rock.setTexture(rockTexture);
        addObject(this_rock);
    }

    /** Add Enemy Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addEnemy(int row, int col, int enemy_type) {
        computePosition(col, row);
        Shark this_shark = new Shark(compute_temp, null);
        this_shark.setTexture(enemyTexture);
        addEnemyObject(this_shark);
    }

    /** Add Treasure Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addTreasure(int row, int col) {
        computePosition(col, row);
        Treasure this_treasure = new Treasure(compute_temp);
        this_treasure.setTexture(treasureTexture);
        addObject(this_treasure);
    }

    /** Add Goal Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addGoal(int row, int col) {
        computePosition(col, row);
        Goal this_goal = new Goal(compute_temp);
        this_goal.setTexture(targetTexture);
        addObject(this_goal);
        goal = this_goal;
    }

    /** Add Raft Objects to the world, using the Json value for raft
     * @param row the row gird position
     * @param col the column grid position */
    private void addRaft(int row, int col) {
        computePosition(col, row);
        Raft this_raft = new Raft(compute_temp);
        this_raft.setTexture(raftTexture);
        addObject(this_raft);
        raft = this_raft;
    }

    /** Add wood Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position
     * @param value the JS value that represents the goal */
    private void addWood(int row, int col, int value) {
        computePosition(col, row);
        Wood this_wood = new Wood(compute_temp, value);
        this_wood.setTexture(woodTexture); // TODO use correct wood texture
        if(value > 3){
            this_wood.setTexture(doubleTexture);
        }
        addObject(this_wood);
    }

    /** Add current Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position
     * @param direction the direction */
    private void addCurrent(int row, int col, Current.Direction direction) {
        computePosition(col, row);
        Current this_current = new Current(compute_temp, direction);
        this_current.setTexture(currentTexture);
        addCurrentObject(this_current);
    }

    /** Compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x_col the x grid value
     * @param y_row the y grid value */
    private void computePosition(int x_col, int y_row){
        compute_temp.x = ((float) x_col + 0.5f) * GRID_SIZE.x;
        compute_temp.y = ((float) y_row + 0.5f) * GRID_SIZE.y;
    }

    /** This gather the assets required for initializing the objects
     * @param directory the asset directory */
    public void gatherAssets(AssetDirectory directory) {
        raftTexture = directory.getEntry("raft", Texture.class);
        oceanTexture = directory.getEntry("water_tile", Texture.class);
        woodTexture = directory.getEntry("wood", Texture.class);
        doubleTexture = directory.getEntry("double", Texture.class);
        targetTexture = directory.getEntry("target", Texture.class);
        rockTexture = directory.getEntry("rock", Texture.class);
        treasureTexture = directory.getEntry("treasure", Texture.class);
        currentTexture = directory.getEntry("current", Texture.class);
        enemyTexture = directory.getEntry("enemy", Texture.class);
        earthTile = new TextureRegion(directory.getEntry("earth", Texture.class));
    }

    /** Add wood Objects to random location in the world */
    protected void addRandomWood() {
        Wood this_wood = new Wood(boundsVector2());
        this_wood.setTexture(doubleTexture); // TODO use correct wood texture
        addQueuedObject(this_wood);
    }
}
