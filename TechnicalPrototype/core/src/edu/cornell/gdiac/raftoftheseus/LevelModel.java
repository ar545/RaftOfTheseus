package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.PooledList;

public class LevelModel {

    /*=*=*=*=*=*=*=*=*=* LEVEL CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Default width and height of a single grid in Box2d units */
    private static final float DEFAULT_GIRD_EDGE_LENGTH = 3.0f;
    /** Default boundary width and height of a single grid in Box2d units */
    private static final float DEFAULT_BOUNDARY = 3.0f;
    /** Default num of rows in the map (y, height) */
    private static final int DEFAULT_GRID_ROW = 8;
    /** Default num of columns in the map (x, width) */
    private static final int DEFAULT_GRID_COL = 10;
    /** Representing the length and width of a single gird unit on the board */
    private static final Vector2 GRID_SIZE = new Vector2(DEFAULT_GIRD_EDGE_LENGTH, DEFAULT_GIRD_EDGE_LENGTH);
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
    /** Reference to the game assets directory */
    private AssetDirectory directory;
    /** The read-in level data */
    private JsonValue level_data;
    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    protected Rectangle bounds;
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
    private PooledList<Enemy> enemies = new PooledList<>();

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
    public PooledList<Enemy> getEnemies() { return enemies; }
    /***/
    public PooledList<GameObject> getAddQueue() { return addQueue; }
    /** set directory */
    protected void setDirectory(AssetDirectory directory) { this.directory = directory; }
    /** Constructor call for this singleton class */
    public LevelModel(){}

    /**
     * Returns true if the object is in bounds.
     *
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     *
     * @return true if the object is in bounds.
     */
    private boolean inBounds(GameObject obj) {
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
    protected void addQueuedObject(GameObject obj) {
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
     * Immediately adds the object to the physics world
     *
     * param obj The object to add
     */
    protected void addObject(Enemy obj, int enemy_type) {
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
        for(GameObject obj : objects) {
            obj.deactivatePhysics(world);
        }
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
        for(GameObject obj : objects) {
            obj.deactivatePhysics(world);
        }
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
        // Populate game objects
        populateLevel();
    }

    /** Set the physical boundary of the level. This boundary will be enforced when adding objects */
    private void setBound() {
        // Read in the grid size and scale. This is not included in Tiled format
//        float col_width = world_data.getFloat("col_width", DEFAULT_GIRD_EDGE_LENGTH);
//        float row_height = world_data.getFloat("row_height", DEFAULT_GIRD_EDGE_LENGTH);
//        this.grid_size = new Vector2(col_width, row_height);
//        this.scale = new Vector2(world_data.getFloat("draw_scale_x", 1),
//                world_data.getFloat("draw_scale_y", 1));

        // Calculate the world bounds base on the grid map size
        this.bounds = new Rectangle(0,0,GRID_SIZE.x * map_size.x + 2 * DEFAULT_BOUNDARY,
                GRID_SIZE.y * map_size.y + 2 * DEFAULT_BOUNDARY);

        // Add wall to the world
        computeWall(GRID_SIZE.x * map_size.x, GRID_SIZE.y * map_size.y);
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
    }

    /** Add Wall Objects to the world, using the Json value for goal.
     */
    private void generateRectangle(float x1, float y1, float x2, float y2) {
        x1 += -DEFAULT_BOUNDARY;
        x2 += -DEFAULT_BOUNDARY;
        y1 += -DEFAULT_BOUNDARY;
        y2 += -DEFAULT_BOUNDARY;
//        float[] polygonVertices = { 16.0f, 18.0f, 16.0f, 17.0f,  1.0f, 17.0f,  1.0f,  1.0f, 16.0f,  1.0f, 16.0f,  0.0f,  0.0f,  0.0f,  0.0f, 18.0f};
//        System.out.println(x1 + "/" + y1 + "\\" + x2 + "/" + y2);
        float[] polygonVertices = {x1, y1, x2, y1, x2, y2, x1, y2};
        Wall this_wall = new Wall(polygonVertices);
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
                //  see the        Enemy this_enemy = new Enemy(compute_temp, null); on line 379
                populateEnemiesRaftField();
            }
        }
    }

    private void populateEnemiesRaftField(){
        for (Enemy enemy : enemies){
            enemy.setTargetRaft(raft);
        }
    }

    /***/
    private void populateEnemies(int row, int col, int tile_int) {
        switch (tile_int){
            case TILE_ENEMY:
                addEnemy(row, col, 0);
                break;
            case TILE_START:
                addRaft(row, col, 100f);
                break;
            default:
                break;
        }
    }

    /***/
    private void populateCollect(int row, int col, int tile_int) {
        if(tile_int == TILE_TREASURE){
            addTreasure(row, col);
        } else if(tile_int >= TILE_WOOD){
            addWood(row, col, tile_int - TILE_WOOD + 1);
        }
    }

    /***/
    private void populateEnv(int row, int col, int tile_int) {
        if(tile_int == TILE_ROCK){
            addRock(row, col);
        }else if(tile_int == TILE_GOAL){
            addGoal(row, col);
        }else if(tile_int >= TILE_CURRENT && tile_int <= TILE_CURRENT + TILE_WEST){
            addCurrent(row, col, compute_direction(tile_int - TILE_CURRENT), 10f);
        }
    }

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
        Enemy this_enemy = new Enemy(compute_temp, null);
        this_enemy.setTexture(enemyTexture);
        addObject(this_enemy, enemy_type);
    }

    /** Add Treasure Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addTreasure(int row, int col) {
        computePosition(col, row);
        Treasure this_treasure = new Treasure(compute_temp);
        this_treasure.setTexture(targetTexture); // TODO use correct texture
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
    private void addRaft(int row, int col, float speed) {
        computePosition(col, row);
        Raft this_raft = new Raft(compute_temp, speed);
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
        this_wood.setTexture(woodTexture); // TODO use correct texture
        addObject(this_wood);
    }

    /** Add current Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position
     * @param direction the direction
     * @param speed the speed */
    private void addCurrent(int row, int col, Current.Direction direction, float speed) {
        computePosition(col, row);
        Current this_current = new Current(compute_temp, direction, speed);
        this_current.setTexture(currentTextures[0]); // TODO set correct current texture
        addObject(this_current);
    }

    /** Compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x_col the x grid value
     * @param y_row the y grid value */
    private void computePosition(int x_col, int y_row){
        compute_temp.x = ((float) x_col + 0.5f) * GRID_SIZE.x;
        compute_temp.y = ((float) y_row + 0.5f) * GRID_SIZE.y;
    }

    public void gatherAssets(AssetDirectory directory) {
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
        earthTile = new TextureRegion(directory.getEntry( "earth", Texture.class ));
    }

    /*
//    private void addRocks(JsonValue rocks) {
//        for (JsonValue rock: rocks) {
//            computePosition(rock.getInt("x", 0), rock.getInt("y", 0));
//            GameObject this_rock = new Rock(compute_temp);
//            addObject(this_rock);
//        }
//    }
    */

}
