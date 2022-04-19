package edu.cornell.gdiac.raftoftheseus;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.steer.behaviors.FollowFlowField;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.lights.PointSource;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;

public class LevelModel {

    /*=*=*=*=*=*=*=*=*=* LEVEL CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Width and height of a single grid square in Box2d units */
    private static final float GRID_SIZE = 3.0f;
    /** Pixels per grid square */
    private static final float GRID_PIXELS = 100.0f;
    /** Pixels per Box2D unit */
    private static final float PIXELS_PER_UNIT = GRID_PIXELS/GRID_SIZE;
    /** Default boundary width and height of a single grid in Box2d units */
    private static final float DEFAULT_BOUNDARY = 1.0f;
    /** Default num of rows in the map (y, height) */
    private static final int DEFAULT_GRID_ROW = 8;
    /** Default num of columns in the map (x, width) */
    private static final int DEFAULT_GRID_COL = 10;
    /** Top-down game with no gravity */
    protected static final Vector2 NO_GRAVITY = new Vector2(0f ,0f );
    /** This is used as a level int representing restarting the level */
    protected static final int LEVEL_RESTART_CODE = -1;

    /*=*=*=*=*=*=*=*=*=* TILED CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Index of the representation of default in tile set texture */
    private static final int TILE_DEFAULT = 0;
    /** Index of the representation of start in tile set texture */
    private static final int TILE_START = 1;
    /** Index of the representation of goal in tile set texture */
    private static final int TILE_GOAL = 2;
    /** Index of the representation of rock in tile set texture */
    private static final int[] TILE_ROCK = {11, 12, 21, 22};
    /** Index of the representation of rock in tile set texture */
    private static final int TILE_ROCK_DEFAULT = TILE_ROCK[0];
    /** Index of the representation of enemy in tile set texture */
    private static final int[] TILE_ENEMY = {31, 32, 41, 42};
    /** Index of the representation of enemy in tile set texture */
    private static final int TILE_ENEMY_DEFAULT = TILE_ENEMY[0];
    /** Index of the representation of default treasure in tile set texture */
    private static final int[] TILE_TREASURE = {51, 52};
    /** Index of the representation of default treasure in tile set texture */
    private static final int TILE_TREASURE_DEFAULT = TILE_TREASURE[0];
    /** Index of the representation of default wood in tile set texture */
    private static final int TILE_WOOD_OFFSET = 60;
    /** Index of the representation of default current in tile set texture */
    private static final int TILE_CURRENT_DIVISION = 10;
    /** Index of the representation of default current in tile set texture */
    private static final int TILE_CURRENT_OFFSET = -10;

    /*=*=*=*=*=*=*=*=*=* TILED CURRENT DIRECTION CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Offset of north current in tile set index */
    private static final int TILE_NORTH = 0;
    /** Offset of north current in tile set index */
    private static final int TILE_NORTH_EAST = 3;
    /** Offset of east current in tile set index */
    private static final int TILE_EAST = 4;
    /** Offset of east current in tile set index */
    private static final int TILE_EAST_SOUTH = 5;
    /** Offset of south current in tile set index */
    private static final int TILE_SOUTH = 6;
    /** Offset of south current in tile set index */
    private static final int TILE_SOUTH_WEST = 7;
    /** Offset of west current in tile set index */
    private static final int TILE_WEST = 8;
    /** Offset of west current in tile set index */
    private static final int TILE_WEST_NORTH = 9;
    /** layer of environment */
    private static final int LAYER_ENV = 0;
    /** layer of collectables */
    private static final int LAYER_COL = 1;
    /** layer of enemies */
    private static final int LAYER_ENE = 2;
    private static final Vector2 ZERO_VECTOR_2 = new Vector2(0, 0);

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
    private AssetDirectory directory; // TODO: is this reference needed at all?
    /** Reference to the game canvas. */
    private GameCanvas canvas;
    /** The read-in level data */
    private JsonValue level_data;
    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    private Rectangle bounds;
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
    private PooledList<Hydra> hydras = new PooledList<>();
    private PooledList<Siren> sirens = new PooledList<>();
    /** Reference to the current field */
    private CurrentField currentField;
    /** The light source of this level */
    private PointSource light;
    /** The rayhandler for storing lights, and drawing them (SIGH) */
    protected RayHandler rayhandler;
    /** The camera defining the RayHandler view; scale is in physics coordinates */
    protected OrthographicCamera raycamera;

    /*=*=*=*=*=*=*=*=*=* Graphics assets for the entities *=*=*=*=*=*=*=*=*=*/
    /** Texture for all ships, as they look the same */
    private FilmStrip raftTexture;
    /** Texture for wood pieces that represent single pile of log */
    private TextureRegion woodTexture;
    /** Texture for wood pieces that represents double pile of logs */
    private TextureRegion doubleTexture;
    /** Texture for all target, as they look the same */
    private TextureRegion targetTexture;
    /** Texture for all treasures */
    private TextureRegion treasureTexture;
    /** Texture for all rock, as they look the same */
    private TextureRegion rockTexture;
    /** Texture for current placeholder: texture alas in future */
    private TextureRegion currentTexture;
    /** Texture for current placeholder: texture atlas in future */
    private FilmStrip enemyTexture;
    /** Texture for spear */
    private TextureRegion spearTexture;
    /** Texture for map background */
    protected Texture mapBackground;
    /** Texture for game background */
    protected Texture gameBackground;
    /** Texture for game background when using shader */
    protected Texture blueTexture;
    /** Texture for wall */
    private TextureRegion earthTile;
    /** The texture for the colored health bar */
    protected Texture colorBar;
    /** The texture for the health bar background */
    protected TextureRegion greyBar;
    /** Json information for light settings */
    private JsonValue lightSettings;

    /** Transform from Box2D coordinates to screen coordinates */
    private Affine2 cameraTransform;
    /** Whether to use shaders or not */
    private static boolean USE_SHADER_FOR_WATER = true;

    private GameObject[][] obstacles;

    /*=*=*=*=*=*=*=*=*=* INTERFACE: getter and setter *=*=*=*=*=*=*=*=*=*/
    /** Constructor call for this singleton class */
    public LevelModel(){}
    /** get the reference to the player avatar */
    public Raft getPlayer() { return raft; }
    /** get a reference to the goal */
    public Goal getGoal() { return goal; }
    /** get the objects (list) of the world */
    public PooledList<GameObject> getObjects() { return objects; }
    /** get the enemies (list) of the world */
    public PooledList<Shark> getEnemies() { return enemies; }
    /** This added queue is use for adding new project tiles */
    public PooledList<GameObject> getAddQueue() { return addQueue; }
    /** set directory */
    protected void setDirectory(AssetDirectory directory) { this.directory = directory; }
    /** @return the bounds of this world in rectangle */
    public Rectangle bounds() {return bounds;}
    /** @return the currents */
    public GameObject[][] obstacles() {return obstacles;}
    /** The number of columns in this map-grid */
    public int cols(){ return map_size.x; }
    /** The number of rows in this map-grid */
    public int rows(){ return map_size.y; }
    /** Getter for how long each tile is **/
    public float getTileSize() { return GRID_SIZE; }

    /** Get boundary wall vertices */
    public float[] getWallVertices() { return polygonVertices; }
//    /** Get boundary wall drawscale */
//        public Vector2 getWallDrawscale() { return this_wall.getDrawScale(); }
    /** Constructor call for this singleton class */
    public LevelModel(GameCanvas canvas){ this.canvas = canvas; }

    /* TODO: getter for new enemy lists */
    public PooledList<Hydra> getHydras() { return hydras; }
    public PooledList<Siren> getSirens() { return sirens; }


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

    /** @return if the given coordinate on grid is in bound. */
    public boolean inBounds(int col, int row) {
        boolean vert = (0<=row && row<rows());
        boolean horiz = (0<=col && col<cols());
        return horiz && vert;
    }

    /** @return the height and width of bounds only
     * width: GRID_SIZE.x * map_size.x
     * height: GRID_SIZE.y * map_size.y */
    public Vector2 boundsVector2(){ return new Vector2(bounds.width, bounds.height); }

    /** @return the height and width of bounds only
     * x = y = - DEFAULT_BOUNDARY;
     * width: GRID_SIZE.x * map_size.x + DEFAULT_BOUNDARY,
     * height: GRID_SIZE.y * map_size.y + DEFAULT_BOUNDARY */
    public Rectangle wallBounds(){
        return new Rectangle(bounds.x - DEFAULT_BOUNDARY, bounds.y - DEFAULT_BOUNDARY,
                bounds.width + DEFAULT_BOUNDARY, bounds.height + 2 * DEFAULT_BOUNDARY);
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


    /**
     * Returns the board cell index for a screen position.
     * <p>
     * While all positions are 2-dimensional, the dimensions to
     * the board are symmetric. This allows us to use the same
     * method to convert an x coordinate or a y coordinate to
     * a cell index.
     *
     * @param f Screen position coordinate
     * @return the board cell index for a screen position.
     */
    public int screenToBoard(float f) {
        return (int) (f / (getTileSize()));
    }

    /**
     * Returns the screen position coordinate for a board cell index.
     * <p>
     * While all positions are 2-dimensional, the dimensions to
     * the board are symmetric. This allows us to use the same
     * method to convert an x coordinate or a y coordinate to
     * a cell index.
     *
     * @param n Tile cell index
     * @return the screen position coordinate for a board cell index.
     */
    public float boardToScreen(int n) {
        return (float) (n + 0.5f) * (getTileSize());
    }

    public Affine2 getCameraTransform() {
        return new Affine2().set(cameraTransform);
    }

    // TODO Create enemy super class to reduce redundant code.
    protected void addHydraObject(Hydra obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        hydras.add(obj);
    }

    protected void addSirenObject(Siren obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        sirens.add(obj);
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
        world  = null;

        light.remove();
        if (rayhandler != null) {
            rayhandler.dispose();
            rayhandler = null;
        }
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
    public void loadLevel(int level_int, JsonValue level_data){
        if(level_int != LEVEL_RESTART_CODE && level_data != null){
            // Load in new level
            this.level_data = level_data;
            // Read in the grid map size
            map_size.x = level_data.getInt("width", DEFAULT_GRID_COL);
            map_size.y = level_data.getInt("height", DEFAULT_GRID_ROW);
            obstacles = new GameObject[cols()][rows()];

            // Reset boundary of world
            setBound();
        }
        // Add wall to the world
        computeWall(bounds.width, bounds.height);

        currentField = new CurrentField(bounds.width, bounds.height, 3);

        // Populate game objects
        populateLevel();

        final FollowFlowField<Vector2> followFlowFieldSB = new FollowFlowField<Vector2>(raft, currentField);
        raft.setSteeringBehavior(followFlowFieldSB);

        // the following could be changed so that it only recalculates a flowmap the first time it loads a level, if
        // this operation is found to be too slow. However, I've found that it's not that slow, so this is unnecessary.
        if (USE_SHADER_FOR_WATER)
            canvas.setFlowMap(recalculateFlowMap());
    }

    /** Calculate the world bounds base on the grid map. Set the physical boundary of the level and the world.
     * This boundary will be enforced when adding objects and checking bullets
     * Notice that the walls are not included in this boundary, i.e. all walls are out of bounds
     * To include the walls in the bounds, expand the size of rectangle by DEFAULT_BOUNDARY on each side */
    private void setBound() {
        this.bounds = new Rectangle(0,0,GRID_SIZE * cols() ,GRID_SIZE * rows() );
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
        for(int row_reversed = 0; row_reversed < rows(); row_reversed ++){
            int row = rows() - row_reversed - 1;
            for(int col = 0; col < cols(); col ++){
                int index = row_reversed * cols() + col;
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
        if (tile_int == TILE_DEFAULT) { return; }
        else if (tile_int == TILE_START) { addRaft(row, col); return; }
        else {
            for(int i = 0; i < TILE_ENEMY.length; i++){
                if(tile_int == TILE_ENEMY[i]){
                    addEnemy(row, col, i);
                    return;
                }
            }
        }
        // This function should never reach here.
        System.out.println("Un-parse-able information detected in enemy layer.");
        addEnemy(row, col, 0);
    }

    /** This is the level editor JSON parser that populate the collectable layer
     * @param row the row the collectable is in the world
     * @param col the column the collectable is in the world
     * @param tile_int whether this tile is a wood or treasure */
    private void populateCollect(int row, int col, int tile_int) {
        if(tile_int == TILE_DEFAULT){ return; }
        else if(tile_int == TILE_TREASURE[0]){ addTreasure(row, col); return; }
        else if (tile_int == TILE_TREASURE[1]){ addTreasure(row, col); return; }
        else if(tile_int > TILE_WOOD_OFFSET){
            addWood(row, col, tile_int - TILE_WOOD_OFFSET);
            return;
        }
        // This function should never reach here.
        System.out.println("Un-parse-able information detected in collectable layer.");
        addWood(row, col, 1);
    }

    /** This is the level editor JSON parser that populate the environment layer
     * @param row the row the environment element is in the world
     * @param col the column the environment element is in the world
     * @param tile_int whether this tile is a rock or a current or a goal */
    private void populateEnv(int row, int col, int tile_int) {
        currentField.field[col][row] = ZERO_VECTOR_2;
        if(tile_int == TILE_DEFAULT){ return; }
        else if(tile_int == TILE_GOAL){ addGoal(row, col); return; }
        for (int j : TILE_ROCK) {
            if (tile_int == j) { addRock(row, col); return; }
        }
        addCurrent(row, col, compute_direction(tile_int % TILE_CURRENT_DIVISION), compute_magnitude(tile_int));
    }

    /** Compute the magnitude of the current base on the level json input
     * @param tile_int level json input indicating object type
     * @return the magnitude of the current */
    private int compute_magnitude(int tile_int) {
        if(tile_int < 1 || tile_int > 60) { return 1; }
        return (tile_int - TILE_CURRENT_OFFSET) / TILE_CURRENT_DIVISION; }

    /** Compute the direction of the current base on the level json input
     * @param i level json input
     * @return the direction of the current */
    private Current.Direction compute_direction(int i) {
        switch (i){
            case TILE_NORTH: return Current.Direction.NORTH;
            case TILE_SOUTH: return Current.Direction.SOUTH;
            case TILE_EAST: return Current.Direction.EAST;
            case TILE_WEST: return Current.Direction.WEST;
            case TILE_NORTH_EAST: return Current.Direction.NORTH_EAST;
            case TILE_SOUTH_WEST: return Current.Direction.SOUTH_WEST;
            case TILE_EAST_SOUTH: return Current.Direction.EAST_SOUTH;
            case TILE_WEST_NORTH: return Current.Direction.WEST_NORTH;
            default: return Current.Direction.NONE;
        }
    }

    /*=*=*=*=*=*=*=*=*=* Level population: add objects *=*=*=*=*=*=*=*=*=*/

    /** Add Rock Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addRock(int row, int col) {
        computePosition(col, row);
        Rock this_rock = new Rock(compute_temp);
        this_rock.setTexture(rockTexture);
//        System.out.println(map_size);
        obstacles[col][row] = this_rock;
        addObject(this_rock);
    }

    /** Add Enemy Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addEnemy(int row, int col, int enemy_type) {
        computePosition(col, row);
        switch(enemy_type) {
            case 0: // Sharks
                Shark this_shark = new Shark(compute_temp, null, this);
                this_shark.setTexture(enemyTexture);
                addEnemyObject(this_shark);
                break;
            case 1: // Hydras
                Hydra th = new Hydra(compute_temp, null);
                th.setTexture(enemyTexture);
                addHydraObject(th);
                break;
            case 2: // Sirens
//                Siren ts = new Siren(compute_temp, null);
//                ts.setTexture(enemyTexture);
//                addSirenObject(ts);
                break;
        }
    }

    /** Add Treasure Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addTreasure(int row, int col) {
        computePosition(col, row);
        Treasure this_treasure = new Treasure(compute_temp);
        this_treasure.setTexture(treasureTexture);
        obstacles[col][row] = this_treasure;
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
    private void addCurrent(int row, int col, Current.Direction direction, int magnitude) {
        // TODO: the current object collision no longer needed, but texture is needed
        computePosition(col, row);
        Current this_current = new Current(compute_temp, direction, magnitude);
        this_current.setTexture(currentTexture);

        // Initialize the current field, used for current vector field
        currentField.field[col][row] = this_current.getDirectionVector();

        // Update the obstacles, used for enemy AI
        obstacles[col][row] = this_current;

        addCurrentObject(this_current);
    }

    /** Compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x_col the x grid value
     * @param y_row the y grid value */
    private void computePosition(int x_col, int y_row){
        compute_temp.x = ((float) x_col + 0.5f) * GRID_SIZE;
        compute_temp.y = ((float) y_row + 0.5f) * GRID_SIZE;
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
        prepareLights(this_raft);
    }

    /** Prepare the box2d light settings once raft is ready */
    private void prepareLights(Raft r){
        initLighting(lightSettings.get("init")); // Box2d lights initialization
        light = createPointLights(lightSettings.get("point")); // Box2d lights information
        attachLights(r);
    }

    /** Update the light effect of the world */
    public void updateLights(){ if (rayhandler != null) { rayhandler.update(); } }

    /** Render the shadow effects. This function should be called after all objects are drawn,
     * but before any health-bar, map, or de-bug information is drawn.
     * Precondition and Post-condition: canvas is closed */
    public void renderLights(){ if (rayhandler != null) {
        Vector2 lightTrans = lightTranslation();
        light.attachToBody(getPlayer().physicsObject.getBody(), lightTrans.x, lightTrans.y, light.getDirection());
        rayhandler.render();
    } }

    /** This calculates the box2d light position translation according to the screen (canvas) size,
     *  the player position, and the pixel per unit scale.
     * @return a Vector2 representing the translation that texture will go through */
    Vector2 lightTranslation() {
        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2), in pixels
        Vector2 translation = new Vector2((float)Gdx.graphics.getWidth()/2, (float)Gdx.graphics.getHeight()/2);
        translation.scl(0.09f);
        translation.sub(getPlayer().getPosition());

         // "Capped Camera": bound x and y within walls
        Rectangle wallBounds = wallBounds();
        translation.x = Math.min(translation.x, - wallBounds.x + 0.03f * canvas.getWidth());
        translation.x = Math.max(translation.x, canvas.getWidth() * 0.06f - wallBounds.width);
        translation.y = Math.min(translation.y, - wallBounds.y + 0.03f * canvas.getHeight());
        translation.y = Math.max(translation.y, canvas.getHeight() * 0.06f - wallBounds.height);

        // "Scaled Camera": adjust x and y scale
        float x_diff = 0.045f * canvas.getWidth() - (getPlayer().getPosition().x + translation.x);
        float y_diff = 0.045f * canvas.getHeight() - (getPlayer().getPosition().y + translation.y);
        if( x_diff != 0 ){ translation.x -= x_diff * 2f; }
        if( y_diff != 0 ){ translation.y -= y_diff * 2f; }
        return translation;
    }

    /*=*=*=*=*=*=*=*=*=* Texture assets and box2d lighting *=*=*=*=*=*=*=*=*=*/

    /** This gather the assets required for initializing the objects
     * @param directory the asset directory */
    public void gatherAssets(AssetDirectory directory) {
        raftTexture = new FilmStrip(directory.getEntry("raft", Texture.class), 4, 5, 19);// TODO: use data-driven design for rows/cols/size
        woodTexture = new TextureRegion(directory.getEntry("wood", Texture.class));
        doubleTexture = new TextureRegion(directory.getEntry("double", Texture.class));
        targetTexture = new TextureRegion(directory.getEntry("target", Texture.class));
        rockTexture = new TextureRegion(directory.getEntry("rock", Texture.class));
        treasureTexture = new TextureRegion(directory.getEntry("treasure", Texture.class));
        currentTexture = new TextureRegion(directory.getEntry("current", Texture.class));
        enemyTexture = new FilmStrip(directory.getEntry("enemy", Texture.class), 1, 17);
        earthTile = new TextureRegion(directory.getEntry("earth", Texture.class));
        spearTexture = new TextureRegion(directory.getEntry("bullet", Texture.class));
        mapBackground = directory.getEntry("map_background", Texture.class);
        gameBackground = directory.getEntry("background", Texture.class);
        blueTexture = directory.getEntry("blue_texture", Texture.class);
        greyBar = new TextureRegion(directory.getEntry( "grey_bar", Texture.class ));
        colorBar  = directory.getEntry( "white_bar", Texture.class );
        lightSettings = directory.getEntry("lights", JsonValue.class);
    }

    /**
     * Creates the points lights for the level
     *
     * Point lights show light in all direction.  We treat them differently from cone
     * lights because they have different defining attributes.  However, all lights are
     * added to the lights array.  This allows us to cycle through both the point lights
     * and the cone lights with activateNextLight().
     *
     * All lights are deactivated initially.  We only want one active light at a time.
     *
     * @param  lightJson	the JSON tree defining the list of point lights
     */
    private PointSource createPointLights(JsonValue lightJson) {
        float[] color = lightJson.get("color").asFloatArray();
        float[] pos = lightJson.get("pos").asFloatArray();
        float dist  = lightJson.getFloat("distance");
        int rays = lightJson.getInt("rays");

        PointSource point = new PointSource(rayhandler, rays, Color.WHITE, dist, pos[0], pos[1]);
        point.setColor(color[0],color[1],color[2],color[3]);
        point.setSoft(lightJson.getBoolean("soft"));

        // Create a filter to exclude see through items
        Filter f = new Filter();
        f.maskBits = (short) lightJson.getInt("excludeBits");
        point.setContactFilter(f);
        point.setActive(true); // TURN ON NOW
        return point;
    }

    /**
     * Creates the ambient lighting for the level
     *
     * This is the amount of lighting that the level has without any light sources.
     * However, if activeLight is -1, this will be ignored and the level will be
     * completely visible.
     *
     * @param  lightJson	the JSON tree defining the light
     */
    private void initLighting(JsonValue lightJson) {
        raycamera = new OrthographicCamera(bounds.width, bounds.height);
        raycamera.position.set(bounds.width/2.0f, bounds.height/2.0f, 0);
        raycamera.update();

        RayHandler.setGammaCorrection(lightJson.getBoolean("gamma"));
        RayHandler.useDiffuseLight(lightJson.getBoolean("diffuse"));
        rayhandler = new RayHandler(world, (int) bounds.width, (int) bounds.height);
        rayhandler.setCombinedMatrix(raycamera);

        float[] color = lightJson.get("color").asFloatArray();
        rayhandler.setAmbientLight(color[0], color[1], color[2], color[3]);
        int blur = lightJson.getInt("blur");
        rayhandler.setBlur(blur > 0);
        rayhandler.setBlurNum(blur);
    }

    /**
     * Attaches all lights to the avatar.
     * Lights are offset form the center of the avatar according to the initial position.
     * By default, a light ignores the body.  This means that putting the light inside
     * of these bodies fixtures will not block the light.  However, if a light source is
     * offset outside the bodies fixtures, then they will cast a shadow.
     * The activeLight is set to be the first element of lights, assuming it is not empty.
     */
    public void attachLights(Raft avatar) {
        light.attachToBody(avatar.physicsObject.getBody(), light.getX(), light.getY(), light.getDirection());
    }

    /*=*=*=*=*=*=*=*=*=* New-added current and wood methods *=*=*=*=*=*=*=*=*=*/

    /** Add wood Objects to random location in the world */
    protected void addRandomWood() {
        Wood this_wood = new Wood(boundsVector2());
        this_wood.setTexture(doubleTexture); // TODO use correct wood texture
        addQueuedObject(this_wood);
    }

    private Texture recalculateFlowMap() {
        Pixmap pix = new Pixmap(cols(), rows(),  Pixmap.Format.RGBA8888);
        pix.setColor(0.5f, 0.5f, 0.5f, 1); // 0.5 = no current
        pix.fill();
        for (GameObject o : getObjects()) {
            if (o.getType() == GameObject.ObjectType.CURRENT) {
                Current c = (Current)o;
                Vector2 p = c.getPosition(); // in box2d units (3 per tile)
                p.scl(1.0f/GRID_SIZE); // in tiles
                // TODO figure out a *good* way to represent current magnitude in the shader.
                Vector2 d = c.getDirectionVector().nor(); // length independent of magnitude
                d.add(1,1).scl(0.5f); // between 0 and 1
                pix.setColor(d.x, d.y, 0, 1);
                pix.drawPixel((int)p.x, (int)p.y);
            }
        }
        Texture t = new Texture(pix);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return t;
    }

    /** Apply current effect to all applicable objects. Linear Combination Model */
    public void updateAllCurrentEffects(){
        for(GameObject o : objects){ if(o.AffectedByCurrent()){ currentField.updateCurrentEffects(o); } }
    }

    /** TODO: There are two ways of applying vector field, through rotational physics or though movement linear
     * combinations. Both make use of CurrentField.java
     * Rotational physics is achieved by rotational movement of objects in current from direction and linear movement
     * from magnitude. Represented by act(). this require combining class Actor and class Obstacles since the two has
     * conflicting physics.
     * Linear combinations model does not deal with rotations and simply calculate the vector linear combinations of
     * the nearby vectors in the field. Might have error in corner cases. I have tested it pretty thoroughly and debugged.
     * I choose to implement the second one and we can switch to the first one if needed.  */
    public void updatePlayerCurrentEffects(float dt){
        raft.act(dt);
        /* OR */
        currentField.updateCurrentEffects(raft);
    }

    // BULLET MANIPULATION

    /**
     * Add a new bullet to the world based on clicked point.
     * @param firelocation where the player cliked in box2d coordinates
     */
    public void createSpear(Vector2 firelocation) {
        Vector2 facing = firelocation.sub(raft.getPosition()).nor();
        Spear bullet = new Spear(raft.getPosition());
        bullet.setTexture(spearTexture);
        bullet.physicsObject.setLinearVelocity(facing.scl(Spear.SPEAR_SPEED).mulAdd(raft.physicsObject.getLinearVelocity(), 0.5f));
        bullet.setAngle(facing.angleDeg()-90f);
        addQueuedObject(bullet);
    }

    /** Destroy if an object is a bullet and is out_of_bound. Could be extended to check for all objects
     * @param s the spear to check for in range */
    public boolean checkSpear(Spear s){
        if(s.outMaxDistance()){
           s.setDestroyed(true);
           return true;
        }
        return false;
    }

    public void draw(float time) {
        if (!canvas.shaderCanBeUsed)
            USE_SHADER_FOR_WATER = false; // disable shader if reading shader files failed (e.g. on Mac)

        canvas.begin(cameraTransform);
        drawWater(USE_SHADER_FOR_WATER, time);
        drawObjects(USE_SHADER_FOR_WATER);
        canvas.end();

        // reset camera transform (because health bar isn't in game units)
        canvas.begin();
        Vector2 playerPosOnScreen = getPlayer().getPosition();
        cameraTransform.applyTo(playerPosOnScreen);
        drawHealthBar(getPlayer().getHealthRatio(), playerPosOnScreen);
        canvas.end();

        // draw a circle showing how far the player can move before they die
        float r = getPlayer().getPotentialDistance() * PIXELS_PER_UNIT;
        canvas.drawHealthCircle((int)playerPosOnScreen.x, (int)playerPosOnScreen.y, r);
    }

    public void drawMap(){
        // translate center point of level to (0,0):
        Vector2 translation_1 = bounds().getCenter(new Vector2(0,0)).scl(-1);

        // scale down so that the whole level fits on the screen, with a margin:
        int pixelMargin = 150;
        float wr = (canvas.getWidth()-2*pixelMargin) / (bounds().width);
        float hr = (canvas.getHeight()-2*pixelMargin) / (bounds().height);
        float scale = Math.min(wr, hr);

        // translate center point of level to center of screen:
        Vector2 translation_2 = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2);

        Affine2 mapTransform = new Affine2();
        mapTransform.setToTranslation(translation_1).preScale(scale, scale).preTranslate(translation_2);

        canvas.begin(mapTransform);
        canvas.draw(mapBackground, Color.GRAY, mapBackground.getWidth() / 2, mapBackground.getHeight() / 2,
                bounds().width/2, bounds().height/2, 0.0f,
                bounds().width/mapBackground.getWidth(), bounds().height/mapBackground.getHeight());
        for(GameObject obj : getObjects()) {
            GameObject.ObjectType type = obj.getType();
            if (type != GameObject.ObjectType.TREASURE && type != GameObject.ObjectType.SHARK
                    && type != GameObject.ObjectType.WOOD) {
                obj.draw(canvas);
            }
        }
        canvas.end();
    }

    /**
     * draws background water (for the sea) and moving currents (using shader)
     * Precondition & post-condition: the game canvas is open */
    public void drawWater(boolean useShader, float time) {
        if (useShader) canvas.useShader(time);
        float pixel = 1;
        float x_scale = boundsVector2().x * pixel;
        float y_scale = boundsVector2().y * pixel;
        if (!useShader)
            canvas.draw(gameBackground, Color.WHITE, 0, 0,  x_scale, y_scale);
        else
            canvas.draw(blueTexture, Color.WHITE, 0, 0,  x_scale, y_scale);// blueTexture may be replaced with some better-looking tiles
        if (useShader)
            canvas.stopUsingShader();
    }

    /**
     *
     * @param useShader
     */
    public void drawObjects(boolean useShader){
        for(GameObject obj : getObjects()) {
            if (!useShader || obj.getType() != GameObject.ObjectType.CURRENT) {
                if (obj.getType() == GameObject.ObjectType.SHARK) {
                    obj.draw(canvas, ((Shark) obj).isEnraged() ? Color.RED : Color.WHITE);
                } else {
                    obj.draw(canvas);
                }
            }
        }
    }

    /** Precondition & post-condition: the game canvas is open
     * @param health the health percentage for the player */
    private void drawHealthBar(float health, Vector2 player_position) {
        Color c = new Color(makeColor((float)1/3, health), makeColor((float)2/3, health), 0.2f, 1);
        TextureRegion RatioBar = new TextureRegion(colorBar, (int)(colorBar.getWidth() * health), colorBar.getHeight());
        float x_origin = (player_position.x - greyBar.getRegionWidth()/2f);
        float y_origin = (player_position.y + 20);
        canvas.draw(greyBar,Color.WHITE,x_origin,y_origin,greyBar.getRegionWidth(),greyBar.getRegionHeight());
        if(health >= 0){canvas.draw(RatioBar,c,x_origin,y_origin,RatioBar.getRegionWidth(),RatioBar.getRegionHeight());}
    }

    /** This function calculate the correct health bar color
     * @param median for red color the median should be 1/3 and 2/3 for green color
     * @param health the health percentage for the player
     * @return the rgb code representing the red or green color
     * old color function: Color c = new Color(Math.min(1, 2 - health * 2), Math.min(health * 2f, 1), 0, 1);*/
    private float makeColor(float median, float health){ return Math.max(0, Math.min((1.5f - 3 * Math.abs(health - median)), 1)); }

    public void drawDebug() {
        canvas.beginDebug(cameraTransform);
        for(GameObject obj : getObjects()) {
            obj.drawDebug(canvas);
        }
        canvas.endDebug();
    }

    /** This function calculates the moving camera linear transformation according to the screen (canvas) size,
     * boundary of the world with walls, the player position, and the pixel per unit scale.
     * Update the "cameraTransform" with an affine transformation that texture will go through */
    public void updateCameraTransform() {
        Affine2 a = new Affine2().setToScaling(PIXELS_PER_UNIT, PIXELS_PER_UNIT);

        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2), in pixels
        Vector2 translation = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2)
                .sub(getPlayer().getPosition().add(0, 0.5f).scl(PIXELS_PER_UNIT));

        // "Capped Camera": bound x and y within walls
        Rectangle wallBounds = wallBounds();
        translation.x = Math.min(translation.x, - wallBounds.x * PIXELS_PER_UNIT);
        translation.x = Math.max(translation.x, canvas.getWidth() - wallBounds.width * PIXELS_PER_UNIT);
        translation.y = Math.min(translation.y, - wallBounds.y * PIXELS_PER_UNIT);
        translation.y = Math.max(translation.y, canvas.getHeight() - wallBounds.height * PIXELS_PER_UNIT);
        cameraTransform = a.preTranslate(translation);
    }

}
