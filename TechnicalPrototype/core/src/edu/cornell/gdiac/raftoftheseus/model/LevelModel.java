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

import static edu.cornell.gdiac.raftoftheseus.model.Stationary.StationaryType.*;

public class LevelModel {

    /*=*=*=*=*=*=*=*=*=* LEVEL CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** Width and height of a single grid square in Box-2d units. A grid is 3 meter wide. */
    private static final float GRID_SIZE = 3.0f;
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
    /** How many difficulty themes are there in the game? */
    private static final int DIFFICULTY_COUNT = 3;
    /** How fast do you want to lerp this camera? Fast: 0.1 or 0.2; Slow: 0.01 or 0.005 */ // TODO: factor out
    private static final float LERP_FACTOR = 0.05f;

    /*=*=*=*=*=*=*=*=*=* LEVEL Objects (clear after each level dispose) *=*=*=*=*=*=*=*=*=*/
    /** The Box2D world */
    public World world;
    /** The player of the level */
    private Raft raft;
    /** The goal of the level */
    private Goal goal;
    /** All the objects in the world. */
    private PooledList<GameObject> objects  = new PooledList<>();
    /** Queue for adding objects */
    private PooledList<GameObject> addQueue = new PooledList<>();
    /** All enemy objects in the world */
    private PooledList<Shark> sharks = new PooledList<>();
    /** All plant objects in the world */
    private PooledList<Plant> plants = new PooledList<>();
    /** All hydras objects in the world */
    private PooledList<Hydra> hydras = new PooledList<>();
    /** All siren in the world */
    private PooledList<Siren> sirens = new PooledList<>();
    /** All spears in the world */
    private PooledList<Spear> spears = new PooledList<>();
    private PooledList<GameObject> woodTreasureDrawList  = new PooledList<>();
    private PooledList<GameObject> currents  = new PooledList<>();
    private PooledList<GameObject> standardDrawList = new PooledList<>();
    /** List of treasure in this world */
    private Treasure[] treasure = new Treasure[3];
    private int treasureCount = 0;

    /*=*=*=*=*=*=*=*=*=* LEVEL Information (clear after each level dispose) *=*=*=*=*=*=*=*=*=*/
    /** The read-in level data */
    private JsonValue level_data;
    /** The boundary of the world */
    private Rectangle bounds;
    /** The difficulty of the level (0 = easy, 1 = medium, 2 = hard) */
    private int difficulty;
    /** The vertices of the wall */
    private float[] polygonVertices;
    /** The map size in grid */
    protected GridPoint2 map_size = new GridPoint2(DEFAULT_GRID_COL, DEFAULT_GRID_ROW);
    /** Reference to the current field */
    private CurrentField currentField;
    /** The light source coming from the player */
    private PointSource raftLight;
    /** The light source coming from the goal */
    private PointSource goalLight;
    /** The light source coming from the goal */
    private PointSource[] treasureLight = new PointSource[3];
    /** The ray-handler for storing lights, and drawing them (SIGH) */
    protected RayHandler rayhandler;

    /*=*=*=*=*=*=*=*=*=* Temp vector used for computation (not saved) *=*=*=*=*=*=*=*=*=*/
    /** Vector 2 holding the temp position vector for the game object to create */
    private Vector2 compute_temp = new Vector2(0, 0);
    /** Vector 2 holding the temp position vector for the siren to jump into */
    private Vector2 siren_compute_temp = new Vector2(0, 0);

    /*=*=*=*=*=*=*=*=*=* LEVEL FIELDS (constant through out game) *=*=*=*=*=*=*=*=*=*/
    /** Reference to the game assets directory */
    private AssetDirectory directory;
    /** Reference to the game canvas. */
    private final GameCanvas canvas;
    /** what light effect to show the player */
    private int light_effect = 0;
    /** the lerp-camera that allow camera retracing */
    Vector2 lerpCamera = new Vector2(0, 0);
    /** Json information for light settings */
    private JsonValue lightSettings;
    /** Transform from Box2D coordinates to screen coordinates */
    private Affine2 cameraTransform;

    private GameObject[][] obstacles;

    /*=*=*=*=*=*=*=*=*=* Graphics assets for the entities (constant through out game) *=*=*=*=*=*=*=*=*=*/
    /** Texture for all ships, as they look the same */
    private FilmStrip raftTexture;
    private FilmStrip raftAura;
    /** Texture for all treasures */
    private FilmStrip treasureTexture;
    private FilmStrip starburstTexture;
    private FilmStrip daisy;
    private FilmStrip yellowDaisy;
    private FilmStrip plantC;
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
    private TextureRegion[] plantTexture = new TextureRegion[Tiled.FIXED_PLANT_COUNT];
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
    /** Texture for map background */
    protected Texture seaBackground;
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
    public PooledList<Plant> getPlants() { return plants; }
    public Treasure[] getTreasure() { return treasure; }
    /** This added queue is use for adding new project tiles */
    public PooledList<GameObject> getAddQueue() { return addQueue; }
    /** set directory */
    public void setDirectory(AssetDirectory directory) { this.directory = directory; }
    /** @return the bounds of this world in rectangle */
    public Rectangle bounds() { return bounds; }
    /** @return the currents */
    public GameObject[][] obstacles() { return obstacles; }
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
        plantTexture[0] = new TextureRegion(directory.getEntry("plantB", Texture.class));
        treasureTexture = new FilmStrip(directory.getEntry("treasure", Texture.class), 1, 7);
        starburstTexture = new FilmStrip(directory.getEntry("treasure_starburst", Texture.class), 2, 5);
        plantC = new FilmStrip(directory.getEntry("plantC", Texture.class), 2, 4);
        daisy = new FilmStrip(directory.getEntry("plantD", Texture.class), 4, 4);
        yellowDaisy = new FilmStrip(directory.getEntry("plantA", Texture.class), 4, 4);
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
        seaBackground = directory.getEntry("background", Texture.class);
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
        terrain = new TextureRegion[2 * DIFFICULTY_COUNT][Tiled.TERRAIN_TYPES];
        int width = terrainTexture.getWidth() / Tiled.TERRAIN_TYPES;
        int height = width; // known to be square
        for(int row = 0; row < DIFFICULTY_COUNT; row ++) {
            for(int col = 0; col < Tiled.TERRAIN_TYPES; col++){
                terrain[row][col] = new TextureRegion(terrainTexture, width * col + 1, height * row * 2 + 1,
                        width - 2, height - 2); // low terrain
                terrain[row + DIFFICULTY_COUNT][col] = new TextureRegion(terrainTexture, width * col + 1, height * row * 2 + 1,
                        width - 2, height*(2) - 2); // high terrain
            }
        }
    }

    /* Todo: vary the grid_pixel per screen size */
    /** Pixels per grid square */
    private static final float GRID_PIXELS = 140.0f;
    /** Pixels per Box2D unit */
    private static final float PIXELS_PER_UNIT = GRID_PIXELS/GRID_SIZE;
    /** prepare the pixel size for the game screen */
    private float calculatePixels(int canvasHeight){ return Math.max(Math.min(120f + ((float) canvasHeight - 960f) / 16f, 160f), 108f); }

    /*=*=*=*=*=*=* Level Parser: bounds of the world *=*=*=*=*=*=*=*/

    /** Returns true if the object is in bounds.
     * This assertion is useful for debugging the physics.
     * @param obj The object to check.
     * @return true if the object is in bounds. */
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

    /*=*=*=*=*=*=* Converter between screen and board *=*=*=*=*=*=*=*/
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
    public int screenToBoard(float f) { return (int) (f / (getTileSize())); }

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
    public float boardToScreen(int n) { return (n + 0.5f) * (getTileSize()); }

    public Affine2 getCameraTransform() { return new Affine2().set(cameraTransform);}

    /*=*=*=*=*=*=*=*=*=* Level selection: dispose, reset, and select a level *=*=*=*=*=*=*=*=*=*/
    /** Dispose of all (non-static) resources allocated to this mode. Clear up all list of this singleton class. */
    public void dispose() {
        for(GameObject obj : objects) { obj.deactivatePhysics(world); }
        objects.clear();
        addQueue.clear();
        sharks.clear();
        hydras.clear();
        sirens.clear();
        plants.clear();
        spears.clear();
        woodTreasureDrawList.clear();
        standardDrawList.clear();
        currents.clear();
        treasureCount = 0; // setting counter to 0 will repopulate the array
        bounds = null;
        if (world != null) { world.dispose(); world  = null; }
        if (raftLight != null) { raftLight.remove(); raftLight = null; }
        if (goalLight != null) { goalLight.remove(); goalLight = null; }
        for(int i = 0; i < 3; i ++){if (treasureLight[i] != null) { treasureLight[i].remove(); treasureLight[i] = null; }}
        if (rayhandler != null) { rayhandler.dispose(); rayhandler = null; }
    }

    /** Resets the status of the game so that we can play again.
     * This method disposes of the world and creates a new one. */
    public void reset() {
        dispose();
        world = new World(ZERO_VECTOR_2,false);
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
        // set difficulty
        setDifficulty(level_int);
        // Add wall to the world
        computeWall(bounds.width, bounds.height);
        // Set current field
        currentField = new  CurrentField(bounds.width, bounds.height, 3);
        // Populate game objects
        populateLevel();
        prepareLights(level_int);

        // the following could be changed so that it only recalculates a flowmap the first time it loads a level, if
        // this operation is found to be too slow. However, I've found that it's not that slow, so this is unnecessary.
        if (canvas.USE_SHADER) {
            canvas.setDataMaps(recalculateFlowMap(), recalculateSurfMap());
        }
    }

    private void setDifficulty(int level_int) {
        difficulty = level_int < 6 ? 0 : (level_int < 13 ? 1 : 2);
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

    /*=*=*=*=*=*=* Level Parser: compute the object to add to the world *=*=*=*=*=*=*=*/

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
        JsonValue environment = layers.get(Tiled.LAYER_ENV);
        JsonValue collectables = layers.get(Tiled.LAYER_COL);
        JsonValue sirenLayer = layers.get(Tiled.LAYER_SIREN);
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
                if(isStart){ addDoubleSiren(position, altPosition); }else{ addDoubleSiren(altPosition, position); }
            }else{
                existingPositions.put(id, position);
            }
        }
        for(Vector2 vec : existingPositions.values()){ addSingleSiren(vec); } // SUPPORT STATIONARY SIREN
        existingPositions.clear();
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

    /** Add siren to this game world */
    private void addDoubleSiren(Vector2 startGridPos, Vector2 endGridPos) {
        computeSirenPosition(startGridPos.x, endGridPos.x, startGridPos.y, endGridPos.y);
        Siren this_siren;
        if(compute_temp.epsilonEquals(siren_compute_temp, 0.1f)){
            this_siren = new Siren(compute_temp, raft);
        } else {
            this_siren = new Siren(compute_temp, siren_compute_temp, raft);
        }
        addSiren(this_siren);
    }

    /** Add siren to this game world */
    private void addSingleSiren(Vector2 GridPos) {
        computeSirenPosition(GridPos.x, 0, GridPos.y, 0);
        Siren this_siren = new Siren(compute_temp, raft);
        addSiren(this_siren);
    }

    /** This is the level editor JSON parser that populate the collectable layer
     * @param row the row the collectable is in the world
     * @param col the column the collectable is in the world
     * @param tile_int whether this tile is a wood or treasure */
    private void populateCollect(int row, int col, int tile_int) {
        if (tile_int == Tiled.DEFAULT){ return; }
        if (tile_int == Tiled.TREASURE){ addTreasure(row, col); return; }
        if (tile_int == Tiled.ENEMY_SHARK){ addEnemy(row, col, true); return; }
        if (tile_int == Tiled.ENEMY_SIREN){ addSingleSiren(new Vector2(col, row)); return; }
        if (tile_int == Tiled.HYDRA){ addEnemy(row, col, false); return; }
        if (tile_int == Tiled.WRECK){ addWreck(row, col); return; }
        if (tile_int == Tiled.WOOD_LOW){ addWood(row, col, Wood.LOW_WOOD); return; }
        if (tile_int == Tiled.WOOD_MIDDLE){ addWood(row, col, Wood.MIDDLE_WOOD); return; }
        if (tile_int == Tiled.WOOD_DEFAULT){ addWood(row, col, Wood.REGULAR_WOOD); return; }
        if (tile_int == Tiled.WOOD_HIGH){ addWood(row, col, Wood.HIGH_WOOD); return; }
        // This function should never reach here.
        System.out.println("Un-parse-able information detected in collectable layer:" + tile_int);
    }

    /** This is the level editor JSON parser that populate the environment layer
     * @param row the row the environment element is in the world
     * @param col the column the environment element is in the world
     * @param tile_int whether this tile is a rock or a current or a goal */
    private void populateEnv(int row, int col, int tile_int, boolean top_row) {
        currentField.field[col][row] = ZERO_VECTOR_2;
        int rockInt = Tiled.computeRockInt(tile_int);
        if(rockInt != Stationary.NON_ROCK){
            Stationary.StationaryType type = Tiled.computeRockType(tile_int);
            if(top_row) { populateExtendLand(row, col, type, rockInt); }
            addRock(row, col, type, rockInt);
        }else{
            if (tile_int == Tiled.DEFAULT || tile_int == Tiled.SEA){ return; }
            if (tile_int == Tiled.START) { addRaft(row, col); return; }
            if (tile_int == Tiled.GOAL){ addGoal(row, col); return; }
            if (tile_int == Tiled.WRECK){ addWreck(row, col); return; }
            if (tile_int < Tiled.TREASURE) {addCurrent(row, col, Tiled.compute_direction(tile_int), false); return;}
            if (Tiled.isStrongCurrent(tile_int)) {addCurrent(row, col, Tiled.compute_direction(tile_int - Tiled.STRONG_CURRENT), true); return; }
            System.out.println("Un-parse-able information detected in environment layer:" + tile_int);
        }
    }

    /** Extend land and terrain into the top invisible border */
    private void populateExtendLand(int row, int col, Stationary.StationaryType type, int rock_int) {
        int extend = Tiled.computeExtend(rock_int);
        if(extend != Stationary.NON_ROCK){ addRock(row + 1, col, type, extend); }
    }

    /*=*=*=*=*=*=*=*=*=* Level population: add objects *=*=*=*=*=*=*=*=*=*/

    /** Compute the position of the object in the world given the grid location.
     * Result stored in compute_temp.
     * @param x_col the x grid value
     * @param y_row the y grid value */
    private void computePosition(int x_col, int y_row){
        compute_temp.x = ((float) x_col + 0.5f) * GRID_SIZE;
        compute_temp.y = ((float) y_row + 0.5f) * GRID_SIZE;
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

    /** Add Rock Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position
     * @param rock_int 0 if stand-alone, 1-13 if texture alas, -1 for sharp, -2 for plant */
    private void addRock(int row, int col, Stationary.StationaryType type, int rock_int) {
        computePosition(col, row);
        Stationary this_rock;
        if(rock_int == Stationary.REGULAR){ // rock or sharp rock
            this_rock = new Stationary(compute_temp, type);
            if(type == Stationary.StationaryType.SHARP_ROCK){
                this_rock.setTexture(sharpRockTexture);
            } else { this_rock.setTexture(regularRockTexture); }
        } else { // terrain or cliff terrain
            if(Stationary.isPlant(rock_int)){ // plant terrain: prepare animated object
                Plant plant = new Plant(compute_temp, type, rock_int);
                if(rock_int == Stationary.plantD) { plant.setTexture(daisy); }
                else if(rock_int == Stationary.plantC) { plant.setTexture(plantC); }
                else if(rock_int == Stationary.plantA) { plant.setTexture(yellowDaisy); }
                else{ plant.setTexture(plantTexture[0]); }
                addObject(plant);
                plants.add(plant);
                standardDrawList.add(plant);
                rock_int = Tiled.FULL_LAND; // Notice: plants are created as double-objects, so no break here.
            } // Each tiled.plant is parsed as a full_land terrain object AND a plant objects with corresponding texture.
            this_rock = new Stationary(compute_temp, type, rock_int);
            this_rock.setTexture(terrain[(type == Stationary.StationaryType.TERRAIN ? difficulty : difficulty + DIFFICULTY_COUNT)][rock_int - 1]);
        }
        if(row < obstacles[0].length){obstacles[col][row] = this_rock;}
        addObject(this_rock);
        standardDrawList.add(this_rock);
    }

    /** Add Treasure Objects to the world, using the Json value for goal.
     * @param row the row gird position
     * @param col the column grid position */
    private void addTreasure(int row, int col) {
        computePosition(col, row);
        Treasure this_treasure = new Treasure(compute_temp, raft);
        this_treasure.setTexture(treasureTexture);
        this_treasure.initSB(starburstTexture);
        obstacles[col][row] = this_treasure;
        treasure[treasureCount] = this_treasure;
        treasureCount++;
        addObject(this_treasure);
        woodTreasureDrawList.add(this_treasure);
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
        standardDrawList.add(this_goal);
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
        woodTreasureDrawList.add(wood);
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
        standardDrawList.add(sw);
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

        addObject(this_current);
        currents.add(this_current);
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
        populateEnemyRaftField();
        standardDrawList.add(this_raft);
    }

    /** populate the raft field for existing enemies */
    private void populateEnemyRaftField(){
        for(Shark s : getSharks()){ s.setRaft(getPlayer()); }
        for(Treasure t : getTreasure()){ if(t != null){ t.setRaft(getPlayer()); } }
    }

    /*=*=*=*=*=*=* Level Parser: adding objects to the world *=*=*=*=*=*=*=*/

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

    /** Immediately adds the object to the physics world and the enemy list
     * @param obj The enemy object to add */
    protected void addSharkObject(Shark obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        sharks.add(obj);
        standardDrawList.add(obj);
    }

    /** Immediately adds the object to the physics world and the enemy list
     * @param obj The enemy object to add */
    protected void addHydraObject(Hydra obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
        hydras.add(obj);
        standardDrawList.add(obj);
    }

    /** add siren to the world */
    protected void addSiren(Siren this_siren) {
        assert inBounds(this_siren) : "Object is not in bounds";
        this_siren.setTexture(sirenTexture);
        this_siren.setStunTexture(stunTexture);
        objects.add(this_siren);
        this_siren.activatePhysics(world);
        sirens.add(this_siren);
        standardDrawList.add(this_siren);
    }

    /** remove the deactivated object
     * @param obj The object to delete */
    public void removeObj(GameObject obj) {
        if(obj.getType() == GameObject.ObjectType.TREASURE || obj.getType() == GameObject.ObjectType.WOOD){
            woodTreasureDrawList.remove(obj);
        } else if(obj.getType() != GameObject.ObjectType.CURRENT){
            standardDrawList.remove(obj);
        }
    }

    /*=*=*=*=*=*=* Level Parser: prepare box2d light *=*=*=*=*=*=*=*/

    /** Prepare the box2d light settings once raft is ready */
    private void prepareLights(int level){
        initLighting(lightSettings.get("init"), level); // Box2d lights initialization
        raftLight = createPointLights(lightSettings.get(difficulty == 2 ? "hard_raft" : "raft")); // One light over the player
        goalLight = createPointLights(lightSettings.get("goal")); // Another light over the goal
        attachLights(raftLight, raft);
        attachLights(goalLight, goal);
        for(int i = 0; i < treasureCount; i ++ ) {
            treasureLight[i] = createPointLights(lightSettings.get("treasure"));
            attachLights(treasureLight[i], treasure[i]);
        }
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

    /*=*=*=*=*=*=*=*=*=* initialize box2d lighting *=*=*=*=*=*=*=*=*=*/
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
        point.setSoftnessLength(lightJson.getInt("softLength"));

        // Create a filter to exclude see through items
        Filter f = new Filter();
        if(lightJson.getBoolean("block")){ f.categoryBits = GameObject.CATEGORY_LIGHT_BLOCK; }
        else{ f.categoryBits = GameObject.CATEGORY_LIGHT_NON; }
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
    private void initLighting(JsonValue lightJson, int level) {
        RayHandler.setGammaCorrection(lightJson.getBoolean("gamma"));
        RayHandler.useDiffuseLight(lightJson.getBoolean("diffuse"));
        rayhandler = new RayHandler(world, (int) bounds.width, (int) bounds.height);
        float[] colorArray = lightJson.get("array").asFloatArray();
        float color = colorArray[Math.max(0, Math.min(level, colorArray.length - 1))];
        rayhandler.setAmbientLight(color, color, color, color);
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
    public void attachLights(PointSource source, GameObject go) {
        source.attachToBody(go.physicsObject.getBody(), source.getX(), source.getY(), source.getDirection());
    }

    /*=*=*=*=*=*=*=*=*=* in-game current and wood methods *=*=*=*=*=*=*=*=*=*/

    /** Add wood Objects to described location in the world */
    public void addWood(Vector2 pos, int value) {
        Wood this_wood = new Wood(pos, value);
        this_wood.setTexture(findWoodTexture(value));
        addQueuedObject(this_wood);
        woodTreasureDrawList.add(this_wood);
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
                Vector2 d = c.getDirectionVector().scl(0.5f/Current.getMaxMagnitude()); // length dependent on magnitude (in 0,1 range)
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
            // figure out whether this object should create a surf pattern
            GameObject.ObjectType oType = o.getType();
            boolean isStationary = (oType == GameObject.ObjectType.STATIONARY);
            boolean isGoal = (oType == GameObject.ObjectType.GOAL);
            boolean isRock = false;
            boolean isTerrain = false;
            if (isStationary) {
                Stationary.StationaryType st = ((Stationary)o).getStationaryType();
                if (st == SHARP_ROCK || st == REGULAR_ROCK)
                    isRock = true;
                if (st == TERRAIN || st == CLIFF_TERRAIN)
                    isTerrain = true;
            }
            boolean hasSurf = isGoal || isRock || isTerrain;
            // add the surf pattern to the texture
            if (hasSurf) {
                Vector2 pos = o.getPosition().scl(1.0f/GRID_SIZE).add(1, 1); // in tiles, offset
                // int position, in tiles:
                int rx = (int)pos.x;
                int ry = (int)pos.y;
                // object center, in tile coords:
                float cx = rx + 0.5f;
                float cy = ry + 0.5f;

                // determine surf shape
                boolean isRound = isGoal || isRock;
                // object radius (only used if isRound is true)
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
                                float d = 0.0f;
                                if (isRound) {
                                    float dx = x - cx;
                                    float dy = (y - cy)*sqrt2;
                                    d = (float)Math.sqrt(dx*dx+dy*dy);
                                    d = Math.max(0.0f, d - rockRadius);
                                } else {
                                    // nearest point in the rock to (x, y)
                                    float nx = Math.min(Math.max(cx-0.5f, x), cx+0.5f);
                                    float ny = Math.min(Math.max(cy-0.5f, y), cy+0.5f);
                                    // distance from pixel to nearest point in rock
                                    float dx = x - nx;
                                    float dy = y - ny;
                                    dy *= sqrt2;
                                    d = (float)Math.sqrt(dx*dx+dy*dy);
                                }
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
        addObject(s);
        raft.setSpear(s);
        standardDrawList.add(s);
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
        addObject(n);
        standardDrawList.add(n);
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

    /*=*=* DRAWING *=*=*/
    /** Set the animation frame for all objects in the level model
     * @param dt the time slice */
    public void setAnimationFrame(float dt) {
        getPlayer().setAnimationFrame(dt);
        for(Spear s : getSpears()){ s.setAnimationFrame(dt); }
        for(Siren s : getSirens()){ s.setAnimationFrame(dt); }
        for(Shark s : getSharks()){ s.setAnimationFrame(dt); }
        if(getTreasureCount() == 3){ for(Treasure s: getTreasure() ){ if(s != null){ s.setAnimationFrame(dt); } } }
        for(Plant s: getPlants()){ s.setAnimationFrame(dt); }
    }

    public void draw(float time, boolean isTutorial) {
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
        drawReticle();
        canvas.end();

        drawHealthCircle(playerPosOnScreen);
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
                obj.draw(canvas, Color.valueOf("a08962"));
            }
        }
        canvas.end();
    }

    /**
     * draws background water (for the sea) and moving currents (using shader)
     * Precondition & post-condition: the game canvas is open */
    public void drawWater(float time) {
        Rectangle eg = extraGrid(); // invisible border on top: use extra gird to don't mess up the scaling in the shader
        if (canvas.USE_SHADER) {
            canvas.useWaterShader(time);
            canvas.draw(waterTexture, Color.WHITE, eg.x,  eg.y, eg.width, eg.height);
            canvas.stopUsingShader();
        } else
            canvas.draw(seaBackground, Color.BLUE, eg.x,  eg.y, eg.width, eg.height);
    }

    private static class renderOrderComparator implements Comparator<GameObject>{
        public int compare(GameObject a, GameObject b) {
            // Put spear on terrain
            if (a.getType() == GameObject.ObjectType.SPEAR && b.getType() == GameObject.ObjectType.STATIONARY){
                return 1;
            } else if (b.getType() == GameObject.ObjectType.SPEAR && a.getType() == GameObject.ObjectType.STATIONARY){
                return -1;
            }
            // Put spear in front of everything else.
            else if(a.getType() == GameObject.ObjectType.SPEAR){
                return 1;
            } else if(b.getType() == GameObject.ObjectType.SPEAR){
                return -1;
            } else if (a.getType() == GameObject.ObjectType.SIREN || a.getType() == GameObject.ObjectType.NOTE) {
                return 1;
            } else if (b.getType() == GameObject.ObjectType.SIREN || b.getType() == GameObject.ObjectType.NOTE){
                return -1;
            } else if (a.getType() == GameObject.ObjectType.STATIONARY && b.getType() == GameObject.ObjectType.STATIONARY){
                Stationary sa = (Stationary) a; Stationary sb = (Stationary) b;
                if(sa.isPlant() && !sb.isPlant()){return 1;} if(sb.isPlant() && !sa.isPlant()){return -1;}
            } else if (a.getType() == GameObject.ObjectType.STATIONARY){
                Stationary sa = (Stationary) a;
                if(sa.isPlant()){ return 1; }
            } else if (b.getType() == GameObject.ObjectType.STATIONARY){
                Stationary sb = (Stationary) b;
                if(sb.isPlant()){ return -1; }
            }
            return (int) Math.signum(b.getY() - a.getY());
        }
    }

    /**
     *
     */
    public void drawObjects(float time){
        if (canvas.USE_SHADER) {
            canvas.useItemShader(time);
            for(GameObject obj : woodTreasureDrawList) { // id shader is on, draw floaty objects with shader
                    obj.draw(canvas);
            }
            canvas.stopUsingShader();
            // draw non-floaty objects
            standardDrawList.sort(new renderOrderComparator()); // sort objects by y value, so that they are drawn in the correct order
            // (note: almost-sorted lists are sorted in O(n) time by Java, so this isn't too slow, but it could still probably be improved.)
            for(GameObject obj : standardDrawList) { // if shader is on, don't draw currents and floaty obj (wood and TR)
                    obj.draw(canvas);
            }
        } else {
            getObjects().sort(new renderOrderComparator());
            for(GameObject obj : getObjects())
                obj.draw(canvas);
        }
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
        if (health < 0.3 && health > 0) {
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
//        mouseGamePos.scl(Math.min(Spear.getSpearRange(),  mouseGamePos.len()) / mouseGamePos.len());
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
        if( light_effect == 2 ){ activateTreasureLight(true); } // activate treasure-chest light
        else if( light_effect == 4 ){ light_effect = 0; activateTreasureLight(false); } // deactivate
    }

    private void activateTreasureLight(boolean b) { for(int i = 0; i < treasureCount; i ++){
        if(treasureLight[i] != null) { treasureLight[i].setActive(b); }
    } }

    /** Set a treasure to be collected. deactivate the treasure light */
    public void treasureCollected(Treasure g) {
        raft.halfLife();
        for(int i = 0; i < treasureCount; i ++){
            if(treasure[i] == g){
                treasureLight[i].setActive(false);
                treasureLight[i] = null;
            }
        }
        g.setCollected(true);
    }

    /** Draw a circle showing how far the player can move before they die (only if light setting is odd).
     * @param playerPosOnScreen the camera-transformed player position */
    public void drawHealthCircle(Vector2 playerPosOnScreen){
        if(light_effect % 2 == 1) {
            float r = getPlayer().getPotentialDistance() * PIXELS_PER_UNIT;
            canvas.drawHealthCircle((int)playerPosOnScreen.x, (int)playerPosOnScreen.y, r);
        }
    }
}
