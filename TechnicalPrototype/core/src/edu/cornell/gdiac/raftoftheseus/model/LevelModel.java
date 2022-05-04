package edu.cornell.gdiac.raftoftheseus.model;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.enemy.Hydra;
import edu.cornell.gdiac.raftoftheseus.model.enemy.Shark;
import edu.cornell.gdiac.raftoftheseus.model.enemy.Siren;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Note;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Spear;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import java.util.Comparator;
import java.util.HashMap;

public class LevelModel {

    public static class Tiled {
        /** Index of the representation of default in tile set texture */
        private static final int DEFAULT = 0;
        /** Index of the representation of start in tile set texture */
        private static final int START = 1;
        /** Index of the representation of enemy in tile set texture */
        private static final int ENEMY_SIREN = 2;
        /** Index of the representation of enemy in tile set texture */
        private static final int ENEMY_SHARK = 3;
        /** Offset of north current in tile set index */
        private static final int NORTH_EAST = 4;
        /** Offset of east current in tile set index */
        private static final int EAST = 5;
        /** Offset of east current in tile set index */
        private static final int EAST_SOUTH = 6;
        /** Offset of south current in tile set index */
        private static final int SOUTH = 7;
        /** Index of the representation of rock in tile set texture */
        private static final int ROCK_ALONE = 8;
        /** Index of the representation of rock in tile set texture */
        private static final int ROCK_SHARP = 9;
        /** Index of the representation of goal in tile set texture */
        private static final int GOAL = 10;
        /** Offset of south current in tile set index */
        private static final int SOUTH_WEST = 11;
        /** Offset of west current in tile set index */
        private static final int WEST = 12;
        /** Offset of west current in tile set index */
        private static final int WEST_NORTH = 13;
        /** Offset of north current in tile set index */
        private static final int NORTH = 14;
        /** Strong current offset */
        private static final int STRONG_CURRENT = 14;
        /** Index of the representation of treasure in tile set texture */
        private static final int TREASURE = 15;
        /** Index of the representation of wood with the lowest amount in tile set texture */
        private static final int WOOD_LOW = 16;
        /** Index of the representation of wood with the second-lowest amount  in tile set texture */
        private static final int WOOD_MIDDLE = 17;
        /** Index of the representation of wreck in tile set texture */
        private static final int WRECK = 22;
        /** Index of the representation of wood with the default amount  in tile set texture */
        private static final int WOOD_DEFAULT = 23;
        /** Index of the representation of wood with the highest amount  in tile set texture */
        private static final int WOOD_HIGH = 24;
        /** Index of the representation of default current in tile set texture */
        private static final int LAND_OFFSET = 28;
        /** Index of the representation of default current in tile set texture */
        private static final int SEA = 42;
        /** the offset for sand textures */
        private static final int SAND_OFFSET = 42;
        /** Index of the representation of plant in tile set texture */
        private static final int PLANT = 56;
        /** Index of the representation of plant in tile set texture */
        private static final int PLANT_END = 62;
        /** Index of the representation of plant in tile set texture */
        private static final int HYDRA = 63;
    }

    /*=*=*=*=*=*=*=*=*=* LEVEL CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Width and height of a single grid square in Box-2d units. A grid is 3 meter wide. */
    private static final float GRID_SIZE = 3.0f;
    /** Pixels per grid square */
    private static final float GRID_PIXELS = 150.0f;
    /** Pixels per Box2D unit */
    private static final float PIXELS_PER_UNIT = GRID_PIXELS/GRID_SIZE;
    /** Default boundary width and height of a single grid in Box-2d units. Borders are 1 meter wide. */
    private static final float DEFAULT_BOUNDARY = 1.0f;
    /** Default num of rows in the map (y, height) */
    private static final int DEFAULT_GRID_ROW = 16;
    /** Default num of columns in the map (x, width) */
    private static final int DEFAULT_GRID_COL = 24;
    /** a final vector 2 with both x and y as 0, e.g. Top-down game with no gravity */
    private static final Vector2 ZERO_VECTOR_2 = new Vector2(0, 0);
    /** the height offset between the health bar and the player height */
    public static final int BAR_PLAYER_OFFSET = 70;
    /** This is used as a level int representing restarting the level */
    protected static final int LEVEL_RESTART_CODE = -1;

    /*=*=*=*=*=*=*=*=*=* TILED CONSTANTS *=*=*=*=*=*=*=*=*=*/

    /** Total variation of terrains */
    private static final int TERRAIN_TYPES = Tiled.SEA - Tiled.LAND_OFFSET - 1;

    /*=*=*=*=*=*=*=*=*=* TILED CURRENT DIRECTION CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** layer of environment and land */
    private static final int LAYER_ENV = 0;
    /** layer of collectables and shark */
    private static final int LAYER_COL = 1;
    /** layer of siren */
    private static final int LAYER_SIREN = 2;
    /** How fast do you want to lerp this camera? Fast: 0.1 or 0.2; Slow: 0.01 or 0.005 */ // TODO: factor out
    private static final float LERP_FACTOR = 0.05f;
    private static final int ROCK_NOT = Integer.MAX_VALUE;
    private static final int ROCK_REGULAR = 0;
    private static final int ROCK_SHARP = -1;
    private static final int ROCK_PLANT = -2;

    /*=*=*=*=*=*=*=*=*=* LEVEL FIELDS *=*=*=*=*=*=*=*=*=*/
    /** The player of the level */
    private Raft raft;
    /** The vertices of the wall */
    private float[] polygonVertices;
    /** Reference to the game assets directory */
    private AssetDirectory directory;
    /** Reference to the game canvas. */
    private final GameCanvas canvas;
    /** The read-in level data */
    private JsonValue level_data;
    /** The Box2D world */
    public World world;
    /** The boundary of the world */
    private Rectangle bounds;
    /** The amount of treasure in the level */
    private int treasureCount;
    /** The difficulty of the level (0 = easy, 1 = medium, 2 = hard) */
    private int difficulty;
    /** The map size in grid */
    protected GridPoint2 map_size = new GridPoint2(DEFAULT_GRID_COL, DEFAULT_GRID_ROW);
    /** Vector 2 holding the temp position vector for the game object to create */
    private Vector2 compute_temp = new Vector2(0, 0);
    /** Vector 2 holding the temp position vector for the siren to jump into */
    private Vector2 siren_compute_temp = new Vector2(0, 0);
    /** All the objects in the world. */
    private PooledList<GameObject> objects  = new PooledList<>();
    /** Queue for adding objects */
    private PooledList<GameObject> addQueue = new PooledList<>();
    /** All enemy objects in the world */
    private PooledList<Shark> sharks = new PooledList<>();
    /** All hydras objects in the world */
    private PooledList<Hydra> hydras = new PooledList<>();
    /** All siren in the world */
    private PooledList<Siren> sirens = new PooledList<>();
    /** All spears in the world */
    private PooledList<Spear> spears = new PooledList<>();
    private PooledList<Treasure> treasure = new PooledList<>();
    /** Reference to the current field */
    private CurrentField currentField;
    /** The light source of this level */
    private PointSource light;
    /** The ray-handler for storing lights, and drawing them (SIGH) */
    protected RayHandler rayhandler;
    /** what light effect to show the player */
    private int light_effect = 0;
    /** the lerp-camera that allow camera retracing */
    Vector2 lerpCamera = new Vector2(0, 0);

    /*=*=*=*=*=*=*=*=*=* Graphics assets for the entities *=*=*=*=*=*=*=*=*=*/
    /** Texture for all ships, as they look the same */
    private FilmStrip raftTexture;
    private FilmStrip raftAura;
    /** Texture for all treasures */
    private FilmStrip treasureTexture;
    /** Texture for wood pieces that represent single pile of log */
    private TextureRegion woodSTexture;
    /** Texture for wood pieces that represents double pile of logs */
    private TextureRegion woodMTexture;
    /** Texture for wood pieces that represent single pile of log */
    private TextureRegion woodRTexture;
    /** Texture for wood pieces that represents double pile of logs */
    private TextureRegion woodLTexture;
    /** Texture for all target, as they look the same */
    private TextureRegion targetTexture;
    /** Texture for all rock, as they look the same */
    private TextureRegion regularRockTexture;
    /** Texture for all rock, as they look the same */
    private TextureRegion sharpRockTexture;
    /** Texture for all the plant which has the same hit-box as the rock */
    private TextureRegion plantTexture;
    /** Texture for current placeholder: texture alas in future */
    private TextureRegion currentTexture;
    /** Stun overlay for enemies */
    private FilmStrip stunTexture;
    /** Texture for current placeholder: texture atlas in future */
    private FilmStrip sharkTexture;
    /** Texture for the Sirens */
    private FilmStrip sirenTexture;
    /** Texture for Water Splash*/
    private FilmStrip splashTexture;
    /** Texture for spear */
    private FilmStrip spearTexture;
    /** Texture for note */
    private TextureRegion noteTexture;
    /** Texture for map background */
    protected Texture mapBackground;
    /** an array of texture region representing the terrain */
    protected TextureRegion[][] terrain;
    /** Texture for water */
    protected Texture waterTexture;
    /** Texture for wall */
    private TextureRegion earthTile;
    /** Texture for fuel */
    private TextureRegion fuelTexture;
    /** The texture for the colored health bar */
    protected Texture colorBar;
    /** The texture for the health bar background */
    protected TextureRegion greyBar;
    /** The texture for the aiming reticle */
    private TextureRegion reticleTexture;
    /** The shipwreck texture. */
    private FilmStrip shipwreckTexture;
    /** Json information for light settings */
    private JsonValue lightSettings;

    /** Transform from Box2D coordinates to screen coordinates */
    private Affine2 cameraTransform;
    /** Whether to use shaders or not */
    private static boolean USE_SHADER = true;

    private GameObject[][] obstacles;

    /** The number of ticks since we were beginning the level */ // TODO: Is this needed?
    private long ticks;

    /*=*=*=*=*=*=*=*=*=* INTERFACE: getter and setter *=*=*=*=*=*=*=*=*=*/
    /** get the reference to the player avatar */
    public Raft getPlayer() { return raft; }
    /** get the objects (list) of the world */
    public PooledList<GameObject> getObjects() { return objects; }
    /** get the shark (list) of the world */
    public PooledList<Shark> getSharks() { return sharks; }
    /** get the hydra (list) of the world */
    public PooledList<Hydra> getHydras() { return hydras; }
    /** get the list of sirens in the world */
    public PooledList<Siren> getSirens() { return sirens; }
    /** get the list of sirens in the world */
    public PooledList<Spear> getSpears() { return spears; }
    public PooledList<Treasure> getTreasure() { return treasure; }
    /** This added queue is use for adding new project tiles */
    public PooledList<GameObject> getAddQueue() { return addQueue; }
    /** set directory */
    public void setDirectory(AssetDirectory directory) { this.directory = directory; }
    /** @return the bounds of this world in rectangle */
    public Rectangle bounds() {return bounds;}
    /** @return the currents */
    public GameObject[][] obstacles() {return obstacles;}
    /** The number of columns in this map-grid */
    public int cols(){ return map_size.x; }
    /** The number of rows in this map-grid */
    public int rows(){ return map_size.y; }
    /** The number of columns in this map-grid */
    public int extraCols(){ return map_size.x + 2; }
    /** The number of rows in this map-grid */
    public int extraRows(){ return map_size.y + 2; }
    /** Getter for how long each tile is **/
    public float getTileSize() { return GRID_SIZE; }
    /** Get boundary wall vertices */
    public float[] getWallVertices() { return polygonVertices; }
    /** Get treasure count for the level */
    public int getTreasureCount() { return treasureCount; }
    /** Constructor call for this singleton class */
    public LevelModel(GameCanvas canvas){ this.canvas = canvas; }


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
        return new Rectangle(bounds.x - 0, bounds.y - 0, bounds.width + 0, bounds.height + GRID_SIZE);
    }

    /** @return the height and width of bounds only
     * x = y = - DEFAULT_BOUNDARY;
     * width: GRID_SIZE.x * map_size.x + DEFAULT_BOUNDARY,
     * height: GRID_SIZE.y * map_size.y + DEFAULT_BOUNDARY */
    public Rectangle extraGrid(){
        return new Rectangle(bounds.x - GRID_SIZE, bounds.y - GRID_SIZE,
                bounds.width + 2 * GRID_SIZE, bounds.height + 2 * GRID_SIZE);
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
    public void addObject(GameObject obj) {
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
    protected void addSharkObject(Shark obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        sharks.add(obj);
    }

    /** Immediately adds the object to the physics world and the enemy list
     * @param obj The enemy object to add */
    protected void addHydraObject(Hydra obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        hydras.add(obj);
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
        return (n + 0.5f) * (getTileSize());
    }

    public Affine2 getCameraTransform() {
        return new Affine2().set(cameraTransform);
    }


    /*=*=*=*=*=*=*=*=*=* Level selection: dispose, select, and reset *=*=*=*=*=*=*=*=*=*/

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        for(GameObject obj : objects) { obj.deactivatePhysics(world); }
        objects.clear();
        sharks.clear();
        treasure.clear();
        hydras.clear();
        sirens.clear();
        addQueue.clear();
        world.dispose();
        objects = null;
        addQueue = null;
        bounds = null;
        world  = null;
        if (light != null) { light.remove(); light = null; }
        if (rayhandler != null) { rayhandler.dispose(); rayhandler = null; }
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        for(GameObject obj : objects) { obj.deactivatePhysics(world); }
        objects.clear();
        sharks.clear();
        hydras.clear();
        sirens.clear();
        addQueue.clear();
        if (world != null)
            world.dispose();
        world = new World(ZERO_VECTOR_2,false);
        if (light != null) { light.remove(); light = null; }
        if (rayhandler != null) { rayhandler.dispose(); rayhandler = null; }
    }

    /** Load the level representing by the parameter level_int.
     * Read the level from the json file and call corresponding functions.
     * Precondition: gameObject list has been cleared.
     *
     * @param level_int an integer representing the level selection, i.e. which json file to read from. */
    public void loadLevel(int level_int, JsonValue level_data){
        treasureCount = 0;
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
        // set difficulty
        setDifficulty(level_int);
        // Add wall to the world
        computeWall(bounds.width, bounds.height);
        // Set current field
        currentField = new  CurrentField(bounds.width, bounds.height, 3);
        // Populate game objects
        populateLevel();

        // the following could be changed so that it only recalculates a flowmap the first time it loads a level, if
        // this operation is found to be too slow. However, I've found that it's not that slow, so this is unnecessary.
        if (USE_SHADER) {
            canvas.setDataMaps(recalculateFlowMap(), recalculateSurfMap());
        }
    }

    private void setDifficulty(int level_int) {
        difficulty = level_int < 6 ? 0 : (level_int < 10 ? 1 : 2);
    }

    public int getDifficulty() {
        return difficulty;
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
        Stationary this_wall = new Stationary(polygonVertices); /* The wall of the level */
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
        JsonValue sirenLayer = layers.get(LAYER_SIREN);
        for(JsonValue layer : layers){
            if(layer.getString("name").equals("Environment")){ environment = layer;}
            else if(layer.getString("name").equals("Collectable")){ collectables = layer;}
            else if(layer.getString("name").equals("Siren")){ sirenLayer = layer;}
            else { System.out.println("Un-parse-able information: layer name not recognized." + layer.getString("name"));}
        }

        int[] env_array = environment.get("data").asIntArray();
        int[] col_array = collectables.get("data").asIntArray();
        // Loop through all index: for(int index = 0; index < map_size.x * map_size.y; index++)
        for(int row_reversed = 0; row_reversed < rows(); row_reversed ++){
            int row = rows() - row_reversed - 1;
            for(int col = 0; col < cols(); col ++){
                int index = row_reversed * cols() + col;
                populateEnv(row, col, env_array[index], row_reversed == 0);
                populateCollect(row, col, col_array[index]);
            }
        }
        populateSiren(sirenLayer.get("objects"));
    }

    /** Populate the new created siren layer. This is the level editor JSON parser that populate the enemy layer
     * @param objects the list of sirens */
    private void populateSiren(JsonValue objects) {
        HashMap<Integer, Vector2> existingPositions = new HashMap<>();
        for(JsonValue obj : objects){
            JsonValue properties = obj.get("properties");
            int id = 0;
            boolean isStart = false;
            if(properties != null){
                for(JsonValue property : properties){
                    if(property.getString("name").equals("ID")){
                        id = property.getInt("value");
                    }else if(property.getString("name").equals("isStart")){
                        isStart = property.getBoolean("value"); // always true
                    }
                }
            }
            Vector2 position = new Vector2((obj.getFloat("x") + 25f) / 50f, rows() - 1 - ((obj.getFloat("y") - 25f) / 50f));

            // Now, add this siren by finding another copy of it
            if(existingPositions.containsKey(id)){
                Vector2 altPosition = existingPositions.remove(id);
                if(isStart){ addSiren(position, altPosition); }else{ addSiren(altPosition, position); }
            }else{
                existingPositions.put(id, position);
            }
        }
        existingPositions.clear();
    }

    /** Add siren to this game world */
    private void addSiren(Vector2 startGridPos, Vector2 endGridPos) {
        computeSirenPosition(startGridPos.x, endGridPos.x, startGridPos.y, endGridPos.y);
        Siren this_siren;
        if(compute_temp.equals(siren_compute_temp)){
            this_siren = new Siren(compute_temp, raft);
        } else {
            this_siren = new Siren(compute_temp, siren_compute_temp, raft);
        }
        this_siren.setTexture(sirenTexture);
        this_siren.setStunTexture(stunTexture);
        addSiren(this_siren);
    }

    /** add siren to the world */
    protected void addSiren(Siren this_siren) {
        assert inBounds(this_siren) : "Object is not in bounds";
        objects.add(this_siren);
        this_siren.activatePhysics(world);
        sirens.add(this_siren);
    }

    /** This is the level editor JSON parser that populate the collectable layer
     * @param row the row the collectable is in the world
     * @param col the column the collectable is in the world
     * @param tile_int whether this tile is a wood or treasure */
    private void populateCollect(int row, int col, int tile_int) {
        if (tile_int == Tiled.DEFAULT){ return; }
        if (tile_int == Tiled.TREASURE){ addTreasure(row, col); return; }
        if (tile_int == Tiled.ENEMY_SHARK){ addEnemy(row, col, true); return; }
        if (tile_int == Tiled.ENEMY_SIREN){ return; } // TODO: undefined behavior
        if (tile_int == Tiled.HYDRA){ addEnemy(row, col, false); return; }
        if (tile_int == Tiled.WRECK){ addWreck(row, col); return; }
        if (tile_int == Tiled.WOOD_LOW){ addWood(row, col, Wood.LOW_WOOD); return; }
        if (tile_int == Tiled.WOOD_MIDDLE){ addWood(row, col, Wood.MIDDLE_WOOD); return; }
        if (tile_int == Tiled.WOOD_DEFAULT){ addWood(row, col, Wood.REGULAR_WOOD); return; }
        if (tile_int == Tiled.WOOD_HIGH){ addWood(row, col, Wood.HIGH_WOOD); return; }
        // This function should never reach here.
        System.out.println("Un-parse-able information detected in collectable layer:" + tile_int);
        addWood(row, col, 1);
    }

    /** This is the level editor JSON parser that populate the environment layer
     * @param row the row the environment element is in the world
     * @param col the column the environment element is in the world
     * @param tile_int whether this tile is a rock or a current or a goal */
    private void populateEnv(int row, int col, int tile_int, boolean top_row) {
        currentField.field[col][row] = ZERO_VECTOR_2;
        int rockInt = computeRockInt(tile_int);
        if(rockInt != ROCK_NOT){
            if(top_row) { populateExtendLand(row, col, rockInt); }
            addRock(row, col, rockInt);
        }else{
            if (tile_int == Tiled.DEFAULT || tile_int == Tiled.SEA){ return; }
            if (tile_int == Tiled.START) { addRaft(row, col); return; }
            if (tile_int == Tiled.GOAL){ addGoal(row, col); return; }
            if (tile_int == Tiled.WRECK){ addWreck(row, col); return; }
            if (tile_int < Tiled.TREASURE) {addCurrent(row, col, compute_direction(tile_int), false); return;}
            if (isStrongCurrent(tile_int)) {addCurrent(row, col, compute_direction(tile_int - Tiled.STRONG_CURRENT), true); return; }
            System.out.println("Un-parse-able information detected in environment layer:" + tile_int);
        }
    }

    private boolean isStrongCurrent(int tile_int){
        int div = tile_int % 7;
        if(div == 1 || div == 2 || div == 3) {return false;}
        return (tile_int <= Tiled.LAND_OFFSET && tile_int > Tiled.TREASURE);
    }

    /** Compute the rock_int according to the tile_int: 0 for reg, 1-13 for land, -1 for sharp, -2 for plant */
    private int computeRockInt(int tile_int){
        if (tile_int > Tiled.LAND_OFFSET && tile_int < Tiled.SEA){ return tile_int - Tiled.LAND_OFFSET; }
        if (tile_int > Tiled.SAND_OFFSET && tile_int < Tiled.PLANT){ return tile_int - Tiled.SAND_OFFSET; } // TODO: differ
        if (tile_int >= Tiled.PLANT && tile_int <= Tiled.PLANT_END){ return ROCK_PLANT; } // TODO: differ
        if (tile_int == Tiled.ROCK_ALONE){ return ROCK_REGULAR; }
        if (tile_int == Tiled.ROCK_SHARP){ return ROCK_SHARP; }
        return ROCK_NOT;
    }

    /** Compute the direction of the current base on the level json input
     * @param i level json input
     * @return the direction of the current */
    private Current.Direction compute_direction(int i) {
        switch (i){
            case Tiled.NORTH: return Current.Direction.NORTH;
            case Tiled.SOUTH: return Current.Direction.SOUTH;
            case Tiled.EAST: return Current.Direction.EAST;
            case Tiled.WEST: return Current.Direction.WEST;
            case Tiled.NORTH_EAST: return Current.Direction.NORTH_EAST;
            case Tiled.SOUTH_WEST: return Current.Direction.SOUTH_WEST;
            case Tiled.EAST_SOUTH: return Current.Direction.EAST_SOUTH;
            case Tiled.WEST_NORTH: return Current.Direction.WEST_NORTH;
            default: return Current.Direction.NONE;
        }
    }

    /*=*=*=*=*=*=*=*=*=* Level population: add objects *=*=*=*=*=*=*=*=*=*/

    /** Add Rock Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position
     * @param tile_int 0 if stand-alone, 1-13 if texture alas, -1 for sharp, -2 for plant */
    private void addRock(int row, int col, int tile_int) {
        computePosition(col, row);
        Stationary.StationaryType rt;
        TextureRegion temp;
        switch(tile_int){
            case ROCK_PLANT:
                rt = Stationary.StationaryType.PLANT;
                temp = plantTexture;
                break;
            case ROCK_SHARP:
                rt = Stationary.StationaryType.SHARP_ROCK;
                temp = sharpRockTexture;
                break;
            case ROCK_REGULAR:
                rt = Stationary.StationaryType.REGULAR_ROCK;
                temp = regularRockTexture;
                break;
            default:
                rt = Stationary.StationaryType.TERRAIN;
                temp = terrain[difficulty][tile_int - 1];
                break;
        }
        Stationary this_rock = new Stationary(compute_temp, rt);
        this_rock.setTexture(temp);
        if(row < obstacles[0].length){obstacles[col][row] = this_rock;}
        addObject(this_rock);
    }

    /** Add Enemy Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addEnemy(int row, int col, boolean is_shark) {
        computePosition(col, row);
        if(is_shark){
            Shark this_shark = new Shark(compute_temp, getPlayer());
            this_shark.setTexture(sharkTexture);
            this_shark.setStunTexture(stunTexture);
            addSharkObject(this_shark);
        } else {
            Hydra h = new Hydra(compute_temp, getPlayer());
            h.setTexture(sharkTexture);
            h.setStunTexture(stunTexture);
            addHydraObject(h);
        }
    }

    /** Add Treasure Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addTreasure(int row, int col) {
        computePosition(col, row);
        Treasure this_treasure = new Treasure(compute_temp, raft);
        this_treasure.setTexture(treasureTexture);
        obstacles[col][row] = this_treasure;
        treasure.add(this_treasure);
        addObject(this_treasure);
        treasureCount++;
    }

    /** Add Goal Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addGoal(int row, int col) {
        computePosition(col, row);
        Goal this_goal = new Goal(compute_temp);
        this_goal.setTexture(targetTexture);
        addObject(this_goal);
    }

    /** Add wood Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position
     * @param value the JS value that represents the goal */
    private void addWood(int row, int col, int value) {
        computePosition(col, row);
        Wood wood = new Wood(compute_temp, value);
        wood.setTexture(findWoodTexture(value));
        addObject(wood);
    }

    private TextureRegion findWoodTexture(int value){
        switch (value){
            case Wood.LOW_WOOD:
                return woodSTexture;
            case Wood.MIDDLE_WOOD:
                return woodMTexture;
            case Wood.HIGH_WOOD:
                return woodLTexture;
            case Wood.REGULAR_WOOD: default:
                return woodRTexture;
        }
    }

    /** Add wood Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position */
    private void addWreck(int row, int col) {
        computePosition(col, row);
        Shipwreck sw = new Shipwreck(compute_temp);
        sw.setTexture(shipwreckTexture);
        addObject(sw);
    }

    /** Add current Objects to the world, using the Json value for goal
     * @param row the row gird position
     * @param col the column grid position
     * @param direction the direction */
    private void addCurrent(int row, int col, Current.Direction direction, boolean isStrong) {
        // TODO: the current object collision no longer needed, but texture is needed
        computePosition(col, row);
        Current this_current = new Current(compute_temp, direction, isStrong);
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

    /** Compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x1 the x grid value of start position
     * @param x2 the y grid value of end position
     * @param y1 the y grid value of start position
     * @param y2 the y grid value of end position */
    private void computeSirenPosition(float x1, float x2, float y1, float y2){
        compute_temp.x = (x1 + 0.5f) * GRID_SIZE;
        compute_temp.y = (y1 + 0.5f) * GRID_SIZE;
        siren_compute_temp.x = (x2 + 0.5f) * GRID_SIZE;
        siren_compute_temp.y = (y2 + 0.5f) * GRID_SIZE;
    }

    /** Add Raft Objects to the world, using the Json value for raft
     * @param row the row gird position
     * @param col the column grid position */
    private void addRaft(int row, int col) {
        computePosition(col, row);
        Raft this_raft = new Raft(compute_temp);
        this_raft.setTexture(raftTexture, raftAura);
        addObject(this_raft);
        raft = this_raft;
        prepareLights(this_raft);
        populateEnemyRaftField();
    }

    /** populate the raft field for existing enemies */
    private void populateEnemyRaftField(){ for(Shark s : getSharks()){ s.setRaft(getPlayer()); } }

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
        canvas.begin(cameraTransform);
        canvas.end();
        rayhandler.setCombinedMatrix(canvas.getCameraMatrix(), bounds.width/2.0f, bounds.height/2.0f, bounds.width, bounds.height);
        rayhandler.render();
    } }

    /** Render the shadow effects. */
    public void renderLightsAlternative(){ if (rayhandler != null) {
        Vector2 lightTrans = lightTranslation();
        light.attachToBody(getPlayer().physicsObject.getBody(), lightTrans.x, lightTrans.y, light.getDirection());
        rayhandler.render();
    } }

    /** This calculates the box-2d light position translation according to the screen (canvas) size,
     *  the player position, and the pixel per unit scale.
     * @return a Vector2 representing the translation that texture will go through */
    Vector2 lightTranslation() {
        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2), in pixels
        Vector2 mapCenter = new Vector2(cols(), rows()).scl(GRID_SIZE).scl(0.5f); // center-of-map coordination
        Vector2 translation = new Vector2(mapCenter); // except for capped camera, light always at center of map
        translation.sub(getPlayer().getPosition());

         // "Capped Camera": bound x and y within walls
        Rectangle wallBounds = wallBounds();
        float half = (GRID_SIZE / GRID_PIXELS) * 0.5f; // 0.015f is the size of half-screen transformed into b2d coordinates
        translation.x = Math.min(translation.x, mapCenter.x + canvas.getWidth() * (-half) - wallBounds.x);
        translation.x = Math.max(translation.x, mapCenter.x + canvas.getWidth() * (half) - wallBounds.width);
        translation.y = Math.min(translation.y, mapCenter.y + canvas.getHeight() * (-half) - wallBounds.y);
        translation.y = Math.max(translation.y, mapCenter.y + canvas.getHeight() * (half) - wallBounds.height);

        // "Scaled Camera": adjust x and y scale
        float x_diff = mapCenter.x - (getPlayer().getPosition().x + translation.x);
        float y_diff = mapCenter.y - (getPlayer().getPosition().y + translation.y);
        if( x_diff != 0 ){ translation.x -= x_diff * ((mapCenter.x / (canvas.getWidth() * half)) - 1); }
        if( y_diff != 0 ){ translation.y -= y_diff * ((mapCenter.y / (canvas.getWidth() * half)) - 1); }
        return translation;
    }

    /*=*=*=*=*=*=*=*=*=* Texture assets and box2d lighting *=*=*=*=*=*=*=*=*=*/

    /** This gather the assets required for initializing the objects. Should be called after directory is set. */
    public void gatherAssets() {
        raftTexture = new FilmStrip(directory.getEntry("raft", Texture.class), 8, 5, 40);// TODO: use data-driven design for rows/cols/size
        raftAura = new FilmStrip(directory.getEntry("raft_aura", Texture.class), 2, 5);
        woodSTexture = new TextureRegion(directory.getEntry("woodS", Texture.class));
        woodMTexture = new TextureRegion(directory.getEntry("woodM", Texture.class));
        woodRTexture = new TextureRegion(directory.getEntry("woodR", Texture.class));
        woodLTexture = new TextureRegion(directory.getEntry("woodL", Texture.class));
        targetTexture = new TextureRegion(directory.getEntry("target", Texture.class));
        regularRockTexture = new TextureRegion(directory.getEntry("regular_rock", Texture.class));
        sharpRockTexture = new TextureRegion(directory.getEntry("sharp_rock", Texture.class));
        plantTexture = new TextureRegion(directory.getEntry("plant", Texture.class));
        treasureTexture = new FilmStrip(directory.getEntry("treasure", Texture.class), 1, 7);
        currentTexture = new TextureRegion(directory.getEntry("current", Texture.class));
        stunTexture = new FilmStrip(directory.getEntry("stun_overlay", Texture.class), 1, 4);
        sharkTexture = new FilmStrip(directory.getEntry("shark", Texture.class), 1, 17);
        sirenTexture = new FilmStrip(directory.getEntry("siren", Texture.class), 4, 5);
        earthTile = new TextureRegion(directory.getEntry("earth", Texture.class));
        splashTexture = new FilmStrip(directory.getEntry("splash", Texture.class), 1, 15);
        spearTexture = new FilmStrip(directory.getEntry("spear", Texture.class), 5, 5);
        noteTexture = new TextureRegion(directory.getEntry("note", Texture.class));
        mapBackground = directory.getEntry("map_background", Texture.class);
//        blueTexture = directory.getEntry("blue_texture", Texture.class);
        waterTexture = directory.getEntry("water_diffuse", Texture.class);
        greyBar = new TextureRegion(directory.getEntry( "grey_bar", Texture.class ));
        colorBar  = directory.getEntry( "white_bar", Texture.class );
        reticleTexture = new TextureRegion(directory.getEntry("reticle", Texture.class));
        lightSettings = directory.getEntry("lights", JsonValue.class);
        canvas.setRadialHealth(directory.getEntry("radial_bar",Texture.class));
        fuelTexture = new TextureRegion(directory.getEntry("fuel", Texture.class));
        shipwreckTexture = new FilmStrip(directory.getEntry("shipwreck", Texture.class), 3, 1);
        gatherTerrainAssets(directory.getEntry("terrain",Texture.class));
    }

    private void gatherTerrainAssets(Texture terrainTexture) {
        terrain = new TextureRegion[3][TERRAIN_TYPES];
        int width = terrainTexture.getWidth() / TERRAIN_TYPES;
        int height = width;
        for(int row = 0; row < 3; row ++) {
            boolean high_terrain = (row == 2);
            for(int col = 0; col < TERRAIN_TYPES; col++){
                terrain[row][col] = new TextureRegion(terrainTexture, width * col + 1, height * row + 1,
                        width - 2, height*(high_terrain ? 2 : 1) - 2);
            }
        }
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
        point.setSoftnessLength(6);

        // Create a filter to exclude see through items
        Filter f = new Filter();
        f.categoryBits = GameObject.CATEGORY_LIGHT_BLOCK;
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
        RayHandler.setGammaCorrection(lightJson.getBoolean("gamma"));
        RayHandler.useDiffuseLight(lightJson.getBoolean("diffuse"));
        rayhandler = new RayHandler(world, (int) bounds.width, (int) bounds.height);

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
    public void addWood(Vector2 pos, int value) {
        Wood this_wood = new Wood(pos, value);
        this_wood.setTexture(findWoodTexture(value));
        addQueuedObject(this_wood);
    }

    /**
     Creates a texture with the same pixel dimensions as the level's dimensions in tiles.
     Each pixel's R and G values represent the X and Y components of the current vector at the corresponding tile.
     These components are scaled so that the range [-1.0, 1.0] maps linearly to [0, 255].
     The B and A values of the texture are unused.
     */
    private Texture recalculateFlowMap() {
        Pixmap pix = new Pixmap(extraCols(), extraRows(),  Pixmap.Format.RGBA8888);
        pix.setColor(0.5f, 0.5f, 0.5f, 1); // 0.5 = no current
        pix.fill();
        for (GameObject o : getObjects()) {
            if (o.getType() == GameObject.ObjectType.CURRENT) {
                Current c = (Current)o;
                Vector2 p = c.getPosition(); // in box2d units (3 per tile)
                p.scl(1.0f/GRID_SIZE); // in tiles
                p.add(1, 1); // offset one tile
                Vector2 d = c.getDirectionVector().scl(0.05f); // length dependent on magnitude (assumes maximum magnitude 20)
                d.add(1,1).scl(0.5f); // between 0 and 1
                pix.setColor(d.x, d.y, 0, 1);
                pix.drawPixel((int)p.x, (int)p.y);
            }
        }
        Texture t = new Texture(pix);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return t;
    }

    /**
     * Creates a texture with pixel dimensions which are [res] times the size of the level in tiles.
     * Groups of res x res pixels map to their corresponding tile in the level.
     * Each pixel's R value represents the "rough average" distance to the nearest shore, across all points in the area
     * of the level covered by that pixel. This can be approximate, because the texture is interpolated later.
     * Distances are clamped to the range [0, 1] in tile space, and mapped to pixel values [0, 255]. A distance of 0
     * means the pixel is inside terrain. A distance of 1 means the pixel is at least 1 tile away from the nearest
     * terrain, so no surf is added.
     * The G, B, and A values of the texture are unused.
     */
    private Texture recalculateSurfMap() {
        int res = 5;
        float sqrt2 = 1.414f; // for 3/4 perspective
        Pixmap pix = new Pixmap(res*extraCols(), res*extraRows(),  Pixmap.Format.RGBA8888);
        pix.setColor(1.0f, 1.0f, 0.5f, 1.0f); // R = 1 = no terrain nearby
        pix.fill();
        for (GameObject o : getObjects()) {
            GameObject.ObjectType oType = o.getType();
            if (oType == GameObject.ObjectType.STATIONARY || oType == GameObject.ObjectType.GOAL) {
                boolean isRock = (oType == GameObject.ObjectType.STATIONARY);
                Vector2 pos = o.getPosition(); // in box2d units (3 per tile)
                pos.scl(1.0f/GRID_SIZE); // in tiles
                pos.add(1, 1); // offset one tile
                // rock position, in tiles:
                int rx = (int)pos.x;
                int ry = (int)pos.y;
                // rock center, in tile coords:
                float cx = rx + 0.5f;
                float cy = ry + 0.5f;
                // rock radius
                float rockRadius = isRock ? 0.6f : 0.97f;

                // iterate through neighboring tiles (but don't go OOB)
                for (int tx = Math.max(0, rx-1); tx <= Math.min(map_size.x+1, rx+1); tx++) {
                    for (int ty = Math.max(0, ry-1); ty <= Math.min(map_size.y+1, ry+1); ty++) {
                        // iterate through the pixels covering that tile
                        for (int px = tx*res; px < (tx+1)*res; px ++) {
                            for (int py = ty*res; py < (ty+1)*res; py ++) {
                                // center of pixel, in tile coords
                                float x = (px+0.5f)/res;
                                float y = (py+0.5f)/res;
                                /*
                                // nearest point in the rock to (x, y)
                                float nx = Math.min(Math.max(cx-0.5f, x), cx+0.5f);
                                float ny = Math.min(Math.max(cy-0.5f, y), cy+0.5f);
                                // distance from pixel to nearest point in rock
                                float dx = x - nx;
                                float dy = y - ny;
                                dy *= sqrt2;
                                 */
                                float dx = x - cx;
                                float dy = (y - cy)*sqrt2;
                                float d = (float)Math.sqrt(dx*dx+dy*dy);
                                d = Math.max(0.0f, d - rockRadius);
                                d = Math.min(1.0f, d); // clamp to 1
                                // if this distance is smaller than what's already in the texture, replace it
                                float d_old = (pix.getPixel(px, py) >>> 24)/255.0f; // red value only
                                d = Math.min(d, d_old);

                                pix.setColor(d, 0.5f, 0.5f, 1.0f);
                                pix.drawPixel(px, py);
                            }
                        }
                    }
                }
            }
        }
        Texture t = new Texture(pix);
//        t.setAnisotropicFilter(1.0f);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return t;
    }

    /** Apply current effect to all applicable objects. Linear Combination Model */
    public void updateAllCurrentEffects(float dt){
        for(GameObject o : objects){ if(o.AffectedByCurrent()){ currentField.updateCurrentEffects(o, dt); } }
    }
    /** @return the current velocity that the player is experiencing */
    public Vector2 getPlayerCurrentVelocity(){ return currentField.getCurrentVelocity(raft.getPosition()); }
    /** @return true if the current velocity where the player is non-zero. */
    public boolean playerOnCurrent(){ return !getPlayerCurrentVelocity().equals(Vector2.Zero); }
    // PROJECTILE MANIPULATION

    /**
     * Add a new bullet to the world based on clicked point.
     */
    public void createSpear() {
        Spear s = new Spear(raft.getPosition());
        s.setTexture(spearTexture);
        spears.add(s);
        addQueuedObject(s);
        raft.setSpear(s);
    }

    /**
     * Remove the spear from raft ownership, giving it velocity.
     * @param firelocation where the mouse was pointing upon release
     */
    public void fireSpear(Vector2 firelocation){
        Vector2 facing = firelocation.sub(raft.getSpear().getPosition()).nor();
        Vector2 raft_speed = raft.physicsObject.getLinearVelocity().cpy().scl(0.5f);
        raft.getSpear().fire(facing, raft_speed);
        raft.setSpear(null);
    }

    /** Destroy if an object is a bullet and is out_of_bound. Could be extended to check for all objects
     * @param s the spear to check for in range */
    public boolean checkProjectile(Spear s){
        if(s.outMaxDistance()){
            s.deactivate();
            return true;
        } else if (s.isToDestroy()){
            s.setDestroyed(true);
            return false;
        }
        return false;
    }

    /**
     * Create a new Note when a Siren is firing at a player in range.
     * @param pos the siren location which fired this note.
     * @param dir the direction towards to player.
     */
    public void createNote(Vector2 pos, Vector2 dir){
        Note n = new Note(pos, dir);
        n.setTexture(noteTexture);
        addQueuedObject(n);
    }

    /** Destroy if an object is a bullet and is out_of_bound. Could be extended to check for all objects
     * @param n the spear to check for in range */
    public boolean checkProjectile(Note n){
        if(n.outMaxDistance()){
            n.setDestroyed(true);
            return true;
        }
        return false;
    }

    // DRAWING

    public void draw(float time, boolean isTutorial) {
        if (!canvas.shaderCanBeUsed)
            USE_SHADER = false; // disable shader if reading shader files failed (e.g. on Mac)

        canvas.begin(cameraTransform);
        drawWater(time);
        drawObjects(time);
        canvas.end();

        // reset camera transform for other player-centered texture (because health bar isn't in game units)
        canvas.begin();
        Vector2 playerPosOnScreen = getPlayer().getPosition().cpy();
        cameraTransform.applyTo(playerPosOnScreen);
        drawHealthBar(getPlayer().getHealthRatio(), playerPosOnScreen);
        if(isTutorial){ drawFuel(getPlayer().getHealthRatio(), playerPosOnScreen, time); } // fuel icon in tutorial only
//        drawReticle();
        canvas.end();

        setLightAndCircle(playerPosOnScreen);
        if(!isTutorial){ renderLights(); } // Draw the light effects if level is not tutorial
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
//        canvas.draw(mapBackground, Color.WHITE, mapBackground.getWidth() / 2, mapBackground.getHeight() / 2,
//                bounds().width/2, bounds().height/2, 0.0f,
//                bounds().width/mapBackground.getWidth(), bounds().height/mapBackground.getHeight());
        // hardcoded values for the new map texture (to avoid things appearing "off the map"
        canvas.draw(mapBackground, Color.WHITE, 382, 255,
                bounds().width/2, bounds().height/2, 0.0f,
                bounds().width/mapBackground.getWidth()*1.349f, bounds().height/mapBackground.getHeight()*1.149f);
        for(GameObject obj : getObjects()) {
            GameObject.ObjectType type = obj.getType();
            if (type == GameObject.ObjectType.CURRENT || type == GameObject.ObjectType.STATIONARY
                    || type == GameObject.ObjectType.GOAL) {
                System.out.println(type);
                obj.draw(canvas, Color.valueOf("a08962"));
            }
        }
        canvas.end();
    }

    /**
     * draws background water (for the sea) and moving currents (using shader)
     * Precondition & post-condition: the game canvas is open */
    public void drawWater(float time) {
        Rectangle eg = extraGrid(); // TODO: invisible border: don't mess up the scaling on everything in the shader
        if (USE_SHADER) {
            canvas.useWaterShader(time);
            canvas.draw(waterTexture, Color.WHITE, eg.x,  eg.y, eg.width, eg.height);
            canvas.stopUsingShader();
        } else
            canvas.draw(waterTexture, Color.BLUE, eg.x,  eg.y, eg.width, eg.height);
    }

    private static class renderOrderComparator implements Comparator<GameObject>{
        public int compare(GameObject a, GameObject b) {
            if(a.getType() == GameObject.ObjectType.SPEAR && b.getType() == GameObject.ObjectType.RAFT){
                return 1;
            } else if (b.getType() == GameObject.ObjectType.SPEAR && a.getType() == GameObject.ObjectType.RAFT){
                return -1;
            } else if (a.getType() == GameObject.ObjectType.SIREN || a.getType() == GameObject.ObjectType.NOTE){
                return 1;
            } else if (b.getType() == GameObject.ObjectType.SIREN || b.getType() == GameObject.ObjectType.NOTE){
                return -1;
            }
            return (int) Math.signum(b.getY() - a.getY());
        }
    }

    /**
     *
     */
    public void drawObjects(float time){
        getObjects().sort(new renderOrderComparator()); // sort objects by y value, so that they are drawn in the correct order
        // (note: almost-sorted lists are sorted in O(n) time by Java, so this isn't too slow, but it could still probably be improved.)
        if (USE_SHADER) {
            // draw floaty objects with shader
            canvas.useItemShader(time);
            for(GameObject obj : getObjects()) {
                if (obj.getType() == GameObject.ObjectType.WOOD || obj.getType() == GameObject.ObjectType.TREASURE) // don't draw currents with the shader on
                    obj.draw(canvas);
            }
            canvas.stopUsingShader();
            // draw non-floaty objects
            for(GameObject obj : getObjects()) {
                if (obj.getType() != GameObject.ObjectType.CURRENT && obj.getType() != GameObject.ObjectType.WOOD
                        && obj.getType() != GameObject.ObjectType.TREASURE) // don't draw currents with the shader on
                    obj.draw(canvas);
            }
        } else {
            for(GameObject obj : getObjects())
                obj.draw(canvas);
        }
    }

    /** Precondition & post-condition: the game canvas is open
     * @param health the health percentage for the player */
    private void drawLinearHealthBar(float health, Vector2 player_position) {
        canvas.draw(greyBar, Color.WHITE, (player_position.x - greyBar.getRegionWidth()/2f), (player_position.y + 20),
                greyBar.getRegionWidth(), greyBar.getRegionHeight());
        canvas.drawLinearHealth(health, player_position, colorBar);
    }

    /** Precondition & post-condition: the game canvas is open
     * @param health the health percentage for the player */
    private void drawHealthBar(float health, Vector2 player_position) {
        canvas.draw(greyBar, Color.WHITE, (player_position.x - greyBar.getRegionWidth()/2f),
                (player_position.y + BAR_PLAYER_OFFSET), greyBar.getRegionWidth(), greyBar.getRegionHeight());
        canvas.drawRadialHealth(new Vector2(player_position.x, player_position.y + 6 + BAR_PLAYER_OFFSET), health);
    }

    /** draw the fuel sign if the health is below a certain level */
    private void drawFuel(float health, Vector2 player_position, float time) {
        if (health < 0.2 && health > 0) {
            int health_int = Math.max(1, (int) (health * 50) * (int) (health * 50));
            if (((int) (time * 100) / health_int) % 2 == 0) {
                canvas.draw(fuelTexture, player_position.x - 50, player_position.y);
            }
        }
    }

    private void drawReticle() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        Vector2 mouseGamePos = new Vector2(mouseX, mouseY);
        getCameraTransform().inv().applyTo(mouseGamePos);
        mouseGamePos.sub(getPlayer().getPosition());
        mouseGamePos.scl(Math.min(Spear.getSpearRange(),  mouseGamePos.len()) / mouseGamePos.len());
        mouseGamePos.add(getPlayer().getPosition());
        getCameraTransform().applyTo(mouseGamePos);

        float modifiedX = mouseGamePos.x;
        float modifiedY = mouseGamePos.y;

        canvas.draw(reticleTexture, new Color(0.82f, 0.70f, 0.03f, 1f), reticleTexture.getRegionWidth()*0.5f, reticleTexture.getRegionHeight()*0.5f,
                modifiedX, modifiedY, reticleTexture.getRegionWidth(), reticleTexture.getRegionHeight());
    }

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
        if (Float.isFinite(lerpCamera.x) && Float.isFinite(lerpCamera.y)) {
            lerpCamera.scl(1 - LERP_FACTOR)
                    .add(getPlayer().getPosition().add(0, 0.5f).scl(PIXELS_PER_UNIT).scl(LERP_FACTOR));
        } else { lerpCamera.setZero(); }

        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2), in pixels
        Vector2 translation = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2).sub(lerpCamera);

        // "Capped Camera": bound x and y within walls
        Rectangle wallBounds = wallBounds();
        translation.x = Math.min(translation.x, - wallBounds.x * PIXELS_PER_UNIT);
        translation.x = Math.max(translation.x, canvas.getWidth() - wallBounds.width * PIXELS_PER_UNIT);
        translation.y = Math.min(translation.y, - wallBounds.y * PIXELS_PER_UNIT);
        translation.y = Math.max(translation.y, canvas.getHeight() - wallBounds.height * PIXELS_PER_UNIT);
        cameraTransform = a.preTranslate(translation);
    }

    /** reset the most recent lerp position */
    public void resetLerp() { lerpCamera.set(getPlayer().getPosition().scl(PIXELS_PER_UNIT)); }

    /** change the level light effect, for testing purposes only */
    public void change(boolean debug) {
        light_effect ++;
        if( light_effect == 4 ){ light_effect = 0; if(debug) { raft.addHealth(200); } } // for testing purposes
    }

    /** draw a circle showing how far the player can move before they die */
    public void setLightAndCircle(Vector2 playerPosOnScreen){
        float r = getPlayer().getPotentialDistance() * PIXELS_PER_UNIT;
        canvas.drawHealthCircle((int)playerPosOnScreen.x, (int)playerPosOnScreen.y, r);
        if(light_effect == 0){ // constant light
            light.setDistance(45);
            light.setSoftnessLength(12);
            light.getContactFilter().categoryBits = GameObject.CATEGORY_LIGHT_BLOCK;
        }else if(light_effect == 1){ // short constant light
            light.setDistance(20);
            light.setSoftnessLength(6);
        }else if(light_effect == 2){ // non-block light
            light.setDistance(30);
            light.getContactFilter().categoryBits = GameObject.CATEGORY_LIGHT_NON;
        }else if(light_effect == 3){ // health light
            float d = getPlayer().getPotentialDistance() * 2;
            light.setDistance(d);
        }
    }

    /* Fix extend land into invisible border */
    private void populateExtendLand(int row, int col, int tile_int) {
        int extend = computeExtend(tile_int);
        if(extend != -1){
            addRock(row + 1, col, extend);
        }
    }

    /**  */
    private int computeExtend(int tile_int){
        switch (tile_int){
            case 1: case 2:
                return tile_int + 7;
            case 3: case 4: case 7: case 13: case ROCK_PLANT:
                return 7;
            case 5:
                return 13;
            case 6: case 9: case 10:
                return 6;
            case 8: case 11: case 12:
                return 12;
            case ROCK_REGULAR: case ROCK_SHARP:
                return 0;
            default:
                return -1;
        }
    }
}
