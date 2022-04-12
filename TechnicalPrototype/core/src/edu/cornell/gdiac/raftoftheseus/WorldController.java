package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.obstacle.Obstacle;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.gdiac.util.PooledList;

import java.util.Iterator;

import static edu.cornell.gdiac.raftoftheseus.GameObject.CATEGORY_PLAYER;

public class WorldController implements Screen, ContactListener {

    /**
     * Method to call after loading to set all constants in World Controller,
     * @param objParams JsonValue of object_parameters instance.
     */
    public static void setConstants(JsonValue objParams){
//        EXIT_QUIT = objParams.getInt("exit quit", 0);
        Bullet.setConstants(objParams.get("bullet"));
        Shark.setConstants(objParams.get("shark"));
        Hydra.setConstants(objParams.get("hydra"));
        Siren.setConstants(objParams.get("siren"));
    }

    // CONSTANTS
    /** Exit code for quitting the game */
    public static int EXIT_QUIT = 0;
    /** Exit code for advancing to next level */
    public static int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** Exit code for opening settings */
    public static final int EXIT_SETTINGS = 3;
    /** How many frames after winning/losing do we continue? */
    public static int EXIT_COUNT = 1000;

    /** Whether to use shaders or not */
    private static boolean USE_SHADER_FOR_WATER = true;

    /** The amount of time for a physics engine step. */
    public static float WORLD_STEP = 1/60.0f;
    /** Number of velocity iterations for the constraint solvers */
    public static int WORLD_VELOCITY = 6;
    /** Number of position iterations for the constraint solvers */
    public static int WORLD_POSIT = 2;

    /** Scale for the health bar */
    private static float HEALTH_BAR_SCALE = 0.6f;


    // FIELDS
    // CANVAS AND OBJECT LIST
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Reference to the game assets directory */
    private AssetDirectory directory;

    private Stage stage;
    private Table table;
    private Skin skin;

    //TEXTURE
    /** The texture for the colored health bar */
    protected Texture colorBar;
    /** The texture for the health bar background */
    protected TextureRegion greyBar;
    /** The texture for the star */
    protected TextureRegion star;
    /** The texture for the exit condition */
    protected Texture bullet_texture;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;
    /** Texture for map background */
    protected Texture mapBackground;
    /** Texture for GAME background */
    protected Texture gameBackground;
    /** Texture for game background when using shader */
    protected Texture blueTexture;
    /** Texture for failed level */
    protected Texture failedBackground;
    /** Texture for success backgrounds where the index corresponds to the score */
    protected Texture[] successBackgrounds;
    /** Whether the transition screen is built  */
    private boolean transitionBuilt;

    // WORLD
    protected LevelModel levelModel;

    // LEVEL STATUS
    /** Whether this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether the next button was clicked */
    private boolean nextPressed;
    /** Whether the settings button was clicked on the transition screen */
    private boolean settingsPressed;
    /** Player score */
    private int playerScore;
    /** Whether the map mode is active */
    private boolean map;
    /** Whether the debug draw mode is active */
    private boolean debug;
    /** Countdown active for winning or losing */
    private int countdown;
    /** array of controls for each enemy**/
    private SharkController[] controls;
    /** Find whether a hydra can see the player. */
    private HydraRayCast hydraSight;
    /** Whether the exit button was pressed */
    private boolean exitPressed;

    private final long startTime;

    /**
     * Creates a new game world
     *
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2d coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected WorldController(GameCanvas canvas) {
        this.canvas = canvas;
        levelModel = new LevelModel();
        this.complete = false;
        this.failed = false;
        this.map  = false;
        this.debug = false;
        this.active = false;
        this.countdown = -1;
        this.playerScore = 0;
        this.nextPressed = false;
        this.exitPressed = false;
        this.stage = new Stage();
        this.skin = new Skin();
        this.table = new Table();
        hydraSight = new HydraRayCast();
        startTime = System.currentTimeMillis();
        transitionBuilt = false;
    }

    /*=*=*=*=*=*=*=*=*=* Draw and Canvas *=*=*=*=*=*=*=*=*=*/

    /**
     * Returns the canvas associated with this controller
     *
     * The canvas is shared across all controllers
     *
     * @return the canvas associated with this controller
     */
    public GameCanvas getCanvas() {
        return canvas;
    }

    /**
     * Sets the canvas associated with this controller
     *
     * The canvas is shared across all controllers.  Setting this value will compute
     * the drawing scale from the canvas size.
     *
     * @param canvas the canvas associated with this controller
     */
    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
    }


    /**
     * Draw the physics objects to the canvas
     *
     * For simple worlds, this method is enough by itself.  It will need
     * to be overridden if the world needs fancy backgrounds or the like.
     *
     * The method draws all objects in the order that they were added.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void draw(float dt) {
//<<<<<<< HEAD
//        if(canvas == null){ return; } // return if no canvas pointer
//=======
        // return if no canvas pointer
        if(canvas == null)
            return;
        if (!canvas.shaderCanBeUsed)
            USE_SHADER_FOR_WATER = false; // disable shader if reading shader files failed (e.g. on Mac)

//>>>>>>> main
        float pixelsPerUnit = 100.0f/3.0f; // Tiles are 100 pixels wide, a tile is 3 units
        Affine2 cameraTransform = calculateMovingCamera(pixelsPerUnit);

        canvas.clear();
        canvas.begin(cameraTransform);
        drawWater();
        for(GameObject obj : levelModel.getObjects()) {

            if (!USE_SHADER_FOR_WATER || obj.getType() != GameObject.ObjectType.CURRENT) {
                if (obj.getType() == GameObject.ObjectType.ENEMY) {
                    obj.draw(canvas, ((Shark) obj).isEnraged() ? Color.RED : Color.WHITE);
                } else {
                    obj.draw(canvas);
                }
            }
        }
        canvas.end();
        // reset camera transform (because health bar isn't in game units)
        canvas.begin();
        Vector2 playerPosOnScreen = levelModel.getPlayer().getPosition();
        cameraTransform.applyTo(playerPosOnScreen);
        drawHealthBar(levelModel.getPlayer().getHealthRatio(), playerPosOnScreen);
        canvas.end();

        drawStar(levelModel.getPlayer().getStar());

        if (map) {
            // translate center point of level to (0,0):
            Vector2 translation_1 = levelModel.bounds().getCenter(new Vector2(0,0)).scl(-1);

            // scale down so that the whole level fits on the screen, with a margin:
            int pixelMargin = 150;
            float wr = (canvas.getWidth()-2*pixelMargin) / (levelModel.bounds().width);
            float hr = (canvas.getHeight()-2*pixelMargin) / (levelModel.bounds().height);
            float scale = Math.min(wr, hr);

            // translate center point of level to center of screen:
            Vector2 translation_2 = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2);

            Affine2 mapTransform = new Affine2();
            mapTransform.setToTranslation(translation_1).preScale(scale, scale).preTranslate(translation_2);

            canvas.begin(mapTransform);
            canvas.draw(mapBackground, Color.GRAY, mapBackground.getWidth() / 2, mapBackground.getHeight() / 2,
                    levelModel.bounds().width/2, levelModel.bounds().height/2, 0.0f,
                    levelModel.bounds().width/mapBackground.getWidth(), levelModel.bounds().height/mapBackground.getHeight());
            for(GameObject obj : levelModel.getObjects()) {
                GameObject.ObjectType type = obj.getType();
                if (type != GameObject.ObjectType.TREASURE && type != GameObject.ObjectType.ENEMY
                        && type != GameObject.ObjectType.WOOD) {
                    obj.draw(canvas);
                }
            }
            canvas.end();
        }

        if (debug) {
            canvas.beginDebug(cameraTransform);
            for(GameObject obj : levelModel.getObjects()) {
                obj.drawDebug(canvas);
            }
            canvas.endDebug();
        }

        // Final message
        if (complete && !failed) {
            drawTransition();
//            displayFont.setColor(Color.YELLOW);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
//            canvas.end();
        } else if (failed) {
            drawTransition();
//            displayFont.setColor(Color.RED);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
//            canvas.end();
        }

        // draw a circle showing how far the player can move before they die
        float r = levelModel.getPlayer().getPotentialDistance() * pixelsPerUnit;
        canvas.drawHealthCircle((int)playerPosOnScreen.x, (int)playerPosOnScreen.y, r);
    }

    private void drawTransition() {
        if (!transitionBuilt) {
            transitionBuilt = true;
            if (complete && !failed) {
                buildTransitionScreen(successBackgrounds[playerScore], false);
            } else {
                buildTransitionScreen(failedBackground, true);
            }
        }
        if (transitionBuilt) {
            Gdx.input.setInputProcessor(stage);
        }
        stage.act();
        stage.draw();
    }

    private void buildTransitionScreen(Texture background, boolean didFail) {
        Pixmap pixmap = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fillRectangle(0, 0, 1, 1);
        Texture darkBackgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Image transparentBackgroundTexture = new Image(darkBackgroundTexture);
        transparentBackgroundTexture.setSize(canvas.getWidth(),canvas.getHeight());
        transparentBackgroundTexture.setColor(0, 0, 0, 0.6f);
        stage.addActor(transparentBackgroundTexture);

        table.setFillParent(true);
        table.align(Align.center);
        stage.addActor(table);
        skin.add("background", background);
        skin.add("font", displayFont);
        table.setBackground(skin.getDrawable("background"));

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = skin.getFont("font");
        TextButton mainButton = new TextButton(didFail ? "RESTART" : "NEXT", buttonStyle);
        mainButton.getLabel().setFontScale(0.6f);
        float buttonWidth =  mainButton.getWidth();
        mainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (didFail) {
                    reset();
                } else {
                    nextPressed = true;
                }
            }
        });
        float padding = (canvas.getWidth() - buttonWidth) / 2f;
        table.add(mainButton).expandX().padLeft(padding - buttonWidth / 2f).padRight(-padding);
        table.row();

        if (!didFail) {
            TextButton replayButton = new TextButton("REPLAY", buttonStyle);
            replayButton.getLabel().setFontScale(0.4f);
            replayButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    reset();
                }
            });

            table.add(replayButton).expandX().padLeft(200);
        }

        TextButton settingsButton = new TextButton("SETTINGS", buttonStyle);
        settingsButton.getLabel().setFontScale(0.4f);
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settingsPressed = true;
            }
        });
        table.add(settingsButton).expandX().padLeft(didFail ? 325 : 0);

        TextButton exitButton = new TextButton("EXIT", buttonStyle);
        exitButton.getLabel().setFontScale(0.4f);
        exitButton.getLabel().setColor(Color.GOLD);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                exitPressed = true;
            }
        });
        table.add(exitButton).expandX().padRight(didFail ? 325 : 200);
        table.row();
    }

//<<<<<<< HEAD
    /** This function calculates the moving camera linear transformation according to the screen (canvas) size,
     * boundary of the world with walls, the player position, and the pixel per unit scale.
     * @param pixelsPerUnit scalar pixel per unit
     * @return an affine2 representing the affine transformation that texture will go through */
    private Affine2 calculateMovingCamera(float pixelsPerUnit) {
        Affine2 a = new Affine2().setToScaling(pixelsPerUnit, pixelsPerUnit);

        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2), in pixels
        Vector2 translation = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2)
                .sub(levelModel.getPlayer().getPosition().add(0, 0.5f).scl(pixelsPerUnit));

        // "Capped Camera": bound x and y within walls
        Rectangle wallBounds = levelModel.wallBounds();
        translation.x = Math.min(translation.x, - wallBounds.x * pixelsPerUnit);
        translation.x = Math.max(translation.x, canvas.getWidth() - wallBounds.width * pixelsPerUnit);
        translation.y = Math.min(translation.y, - wallBounds.y * pixelsPerUnit);
        translation.y = Math.max(translation.y, canvas.getHeight() - wallBounds.height * pixelsPerUnit);
        return a.preTranslate(translation);
    }

    /** This function calculate the correct health bar color
     * @param median for red color the median should be 1/3 and 2/3 for green color
     * @param health the health percentage for the player
     * @return the rgb code representing the red or green color
     * old color function: Color c = new Color(Math.min(1, 2 - health * 2), Math.min(health * 2f, 1), 0, 1);*/
    private float makeColor(float median, float health){ return Math.max(0, Math.min((1.5f - 3 * Math.abs(health - median)), 1)); }

    /** Precondition & post-condition: the game canvas is open
     * @param health the health percentage for the player */
    private void drawHealthBar(float health, Vector2 player_position) {
        Color c = new Color(makeColor((float)1/3, health), makeColor((float)2/3, health), 0.2f, 1);
        TextureRegion RatioBar = new TextureRegion(colorBar, (int)(colorBar.getWidth() * health), colorBar.getHeight());
        float x_origin = (player_position.x - greyBar.getRegionWidth()/2f);
        float y_origin = (player_position.y);
        canvas.draw(greyBar,Color.WHITE,x_origin,y_origin,greyBar.getRegionWidth(),greyBar.getRegionHeight());
        if(health >= 0){canvas.draw(RatioBar,c,x_origin,y_origin,RatioBar.getRegionWidth(),RatioBar.getRegionHeight());}
    }

//    /** draw a background for the sea
//     * Precondition & post-condition: the game canvas is open */
//    private void drawMovingBackground(float pixel) {
//=======
    /** draws background water and moving currents (using shader) */
    private void drawWater() {
        if (USE_SHADER_FOR_WATER)
            canvas.useShader((System.currentTimeMillis() - startTime) / 1000.0f);
//        float pixel = 100/3.0f;
        float pixel = 1;
//>>>>>>> main
        float x_scale = levelModel.boundsVector2().x * pixel;
        float y_scale = levelModel.boundsVector2().y * pixel;
        if (!USE_SHADER_FOR_WATER)
            canvas.draw(gameBackground, Color.WHITE, 0, 0,  x_scale, y_scale);
        else
            canvas.draw(blueTexture, Color.WHITE, 0, 0,  x_scale, y_scale);// blueTexture may be replaced with some better-looking tiles
        if (USE_SHADER_FOR_WATER)
            canvas.stopUsingShader();
    }

    /** Draw star at the up left corner
     * Precondition: the game canvas has not begun; Post-condition: the game canvas will end after this function */
    private void drawStar(int star) {
        canvas.begin();
        if(star > 0){
            canvas.draw(this.star, 0, canvas.getHeight() - this.star.getRegionHeight());
        }
        if(star > 1){
            canvas.draw(this.star, this.star.getRegionWidth(), canvas.getHeight() - this.star.getRegionHeight());
        }
        if(star > 2){
            canvas.draw(this.star, 2 * this.star.getRegionWidth(), canvas.getHeight() - this.star.getRegionHeight());
        }
        canvas.end();
    }



    /**
     * Gather the assets for this controller.
     *
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        // Allocate the tiles
        greyBar = new TextureRegion(directory.getEntry( "grey_bar", Texture.class ));
        star = new TextureRegion(directory.getEntry( "star", Texture.class ));
        colorBar  = directory.getEntry( "white_bar", Texture.class );
        displayFont = directory.getEntry( "diogenes" ,BitmapFont.class);
        mapBackground = directory.getEntry("map_background", Texture.class);
        gameBackground = directory.getEntry("background", Texture.class);
        blueTexture = directory.getEntry("blue_texture", Texture.class);
        bullet_texture = directory.getEntry( "bullet", Texture.class );
        failedBackground = directory.getEntry("failed_background", Texture.class);
        successBackgrounds = new Texture[4];
        for (int i = 0; i < 4; i++) {
            successBackgrounds[i] = directory.getEntry("success_background_" + i, Texture.class);
        }

        levelModel.setDirectory(directory);
        levelModel.gatherAssets(directory);
        this.directory = directory;
        if (USE_SHADER_FOR_WATER) {
            Texture waterTexture = directory.getEntry("water_texture", Texture.class);
            waterTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            canvas.setWaterTexture(waterTexture);
        }
    }

    /*=*=*=*=*=*=*=*=*=* Main Game Loop *=*=*=*=*=*=*=*=*=*/

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
        InputController input = InputController.getInstance();
        input.readInput();

        if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap()) { map = !map; } // Toggle map

//        if (listener == null) {
//            return true;
//        }

        // Now it is time to maybe switch screens.
        if (input.didExit() || exitPressed) {
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        } else if (input.didNext() || nextPressed) {
            pause();
            nextPressed = false;
            listener.exitScreen(this, EXIT_NEXT);
            return false;
        } else if (input.didSettings() || settingsPressed)  {
            pause();
            settingsPressed = false;
            listener.exitScreen(this, EXIT_SETTINGS);
            return false;
        }

        if ((complete && !failed) || failed) { return false; }

        // Handle resets
        if (input.didReset()) {
            reset();
        }

        if (input.didPrevious()) {
            pause();
            listener.exitScreen(this, EXIT_PREV);
            return false;
        } else if (countdown > 0) {
            countdown--;
        } else if (countdown == 0) {
            if (failed) {
                reset();
            } else if (complete) {
                pause();
                listener.exitScreen(this, EXIT_NEXT);
                return false;
            }
        }

        return true;
    }


    private void createBullet(Vector2 facing, Raft player){
        Bullet bullet = new Bullet(player.getPosition().mulAdd(facing, 0.5f), true);
        bullet.setTexture(bullet_texture);
//        bullet.setBullet(true); // this is unnecessary because our bullets travel fairly slowly
        bullet.setLinearVelocity(facing.scl(Bullet.BULLET_SPEED).mulAdd(player.getLinearVelocity(), 0.5f));
        bullet.setAngle(facing.angleDeg()+90f);
        levelModel.addQueuedObject(bullet);
        player.addHealth(Bullet.BULLET_DAMAGE);
    }

    /**
     * Add a new bullet to the world and send it in the right direction.
     */
    private void createBullet(Shark nearestShark) {
        Raft player = levelModel.getPlayer();
        // Compute position and velocity
        Vector2 facing = nearestShark.getPosition().sub(player.getPosition()).nor();
        createBullet(facing, player);
    }

    private void createBullet(Vector2 firelocation) {
        Raft player = levelModel.getPlayer();
        // Compute position and velocity
        Vector2 facing = firelocation.sub(player.getPosition()).nor();
        createBullet(facing, player);
    }

    /**
     * The core gameplay loop of this world.
     *
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt){
        // Process actions in object model

        InputController ic = InputController.getInstance();
        Raft player = levelModel.getPlayer();
        player.setMovementInput(ic.getMovement());
        player.setFire(ic.didFire());

        // Add a bullet if we fire
        if (player.isFire()) {
            // find nearest enemy to player
            if(ic.mouseActive()){
                Vector2 firePixel = ic.getFireDirection();
                Affine2 camera = calculateMovingCamera(100/3.0f);
                camera.inv().applyTo(firePixel);
                createBullet(firePixel);
            }
            else {
                Shark nearestShark = null;
                float nearestD2 = -1;
                for (Shark e : levelModel.getEnemies()) {
                    if (!e.isDestroyed()) {
                        float d2 = e.getPosition().dst2(player.getPosition());
                        if (nearestD2 == -1 || d2 < nearestD2) {
                            nearestD2 = d2;
                            nearestShark = e;
                        }
                    }
                }
                if (nearestShark != null) {
                    createBullet(nearestShark);
                }
            }
        }

        // update forces for enemies, players, objects
        resolveEnemies(dt);
        player.applyInputForce();
        for (GameObject o : levelModel.getObjects())
            o.applyDrag();
    }

    /** get enemies take actions according to their AI */
    private void resolveEnemies(float dt) {
        PooledList<Shark> el = levelModel.getEnemies();

        for (int i = 0; i< el.size(); i++) {
            Shark shark = el.get(i);
            shark.resolveAction(controls[i].getAction(), levelModel.getPlayer(), controls[i].getTicks());
        }

        for (Hydra h : levelModel.getHydras()) {
            levelModel.world.rayCast(hydraSight, h.getPosition(), levelModel.getPlayer().getPosition());
            h.setSee(hydraSight.getCanSee());
            if(h.willAttack()){
                createBullet(h.getPosition(), levelModel.getPlayer());
            }
            h.update(dt);
        }

        for(Siren s : levelModel.getSirens()){
            s.update(dt);
            if(s.willAttack()){
                levelModel.getPlayer().addHealth(s.getAttackDamage());
            }
        }
    }

    /**
     * Processes physics
     *
     * Once the update phase is over, but before we draw, we are ready to handle
     * physics.  The primary method is the step() method in world.  This implementation
     * works for all applications and should not need to be overwritten.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void postUpdate(float dt) {
        // Add any objects created by actions
        while (!levelModel.getAddQueue().isEmpty()) {
            levelModel.addObject(levelModel.getAddQueue().poll());
        }

        // Turn the physics engine crank.
        levelModel.world.step(WORLD_STEP, WORLD_VELOCITY,WORLD_POSIT);
        // update player health based on movement and distance, then check if dead
        Raft player = levelModel.getPlayer();
        player.applyMoveCost(dt);
        if(player.isDead() && !complete && !failed){
            setFailure(true);
        }

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<GameObject>.Entry> iterator = levelModel.getObjects().entryIterator();
        while (iterator.hasNext()) {
            PooledList<GameObject>.Entry entry = iterator.next();
            GameObject obj = entry.getValue();
            levelModel.checkBulletBounds(obj);
            if (obj.isDestroyed()) {
                obj.deactivatePhysics(levelModel.world);
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);
            }
        }
        resolveMusic();
    }

    private boolean wasInDanger = false;

    /** Update the level themed music according the game status */
    private void resolveMusic() {
        boolean nowInDanger = false;
        for(SharkController ai : controls){
            if(ai.isAlive() && ai.getState() == Shark.enemyState.CHASE){
                nowInDanger = true;
                break;
            }
        }
        if(!wasInDanger && nowInDanger){
            SoundController.getInstance().tradeMusic(false);
        }
        if(wasInDanger && !nowInDanger){
            SoundController.getInstance().tradeMusic(true);
        }
        SoundController.getInstance().updateMusic();
        wasInDanger = nowInDanger;
    }

    /**
     * Called when the Screen should render itself.
     *
     * We defer to the other methods update() and draw().  However, it is VERY important
     * that we only quit AFTER a draw.
     *
     * @param delta Number of seconds since last animation frame
     */
    @Override
    public void render(float delta) {
        if (active) {
            if (preUpdate(delta)) { // Check for level reset and win/lose condition
                update(delta); // Update player actions, set Forces, and update enemy AI
                postUpdate(delta); // Call Physics Engine
            }
            draw(delta); // Draw to canvas
        }
    }

    /**
     * Called when the Screen is resized.
     *
     * This can happen at any point during a non-paused state but will never happen
     * before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    @Override
    public void resize(int width, int height) {}

    /**
     * Called when the Screen is paused.
     *
     * This is usually when it's not active or visible on screen. An Application is
     * also paused before it is destroyed.
     */
    @Override
    public void pause() {}

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    @Override
    public void dispose() {
        levelModel.dispose();
        canvas = null;
    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    @Override
    public void resume() {}

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    @Override
    public void show() {
        // Useless if called in outside animation loop
        active = true;
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    @Override
    public void hide() {
        // Useless if called in outside animation loop
        active = false;
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }


    /*=*=*=*=*=*=*=*=*=* Physics *=*=*=*=*=*=*=*=*=*/
    /**
     * Remove a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(GameObject bullet) {
        bullet.setDestroyed(true);
//        plopId = playSound( plopSound, plopId );
    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();
        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            GameObject bd1 = (GameObject) body1.getUserData();
            GameObject bd2 = (GameObject) body2.getUserData();
            // Check for object interaction with current
            if(bd1.getType().equals(GameObject.ObjectType.CURRENT)){
                enterCurrent((Current) bd1, bd2);
            } else if(bd2.getType().equals(GameObject.ObjectType.CURRENT)){
                enterCurrent((Current) bd2, bd1);
            }
            // Check for bullet collision with object (terrain or enemy)
            else if (bd1.getType().equals(GameObject.ObjectType.BULLET)) {
                ResolveCollision((Bullet) bd1, bd2);
            }else if (bd2.getType().equals(GameObject.ObjectType.BULLET)) {
                ResolveCollision((Bullet) bd2, bd1);
            }
            // Check for player collision with wood (health+)
            else if(bd1.getType().equals(GameObject.ObjectType.RAFT) && bd2.getType().equals(GameObject.ObjectType.WOOD)){
                ResolveCollision((Raft)bd1, (Wood)bd2);
            } else if(bd1.getType().equals(GameObject.ObjectType.WOOD) && bd2.getType().equals(GameObject.ObjectType.RAFT)){
                ResolveCollision((Raft)bd2, (Wood)bd1);
            }
            // Check for player kill enemies (health-)
            else if(bd1.getType().equals(GameObject.ObjectType.RAFT) && bd2.getType().equals(GameObject.ObjectType.ENEMY)){
                ResolveCollision((Raft)bd1, (Shark) bd2);
            }else if(bd1.getType().equals(GameObject.ObjectType.ENEMY) && bd2.getType().equals(GameObject.ObjectType.RAFT)){
                ResolveCollision((Raft)bd2, (Shark) bd1);
            }
            // Check for player collision with treasure (star+)
            else if(bd1.getType().equals(GameObject.ObjectType.RAFT) && bd2.getType().equals(GameObject.ObjectType.TREASURE)){
                ResolveCollision((Raft)bd1, (Treasure) bd2);
            } else if(bd1.getType().equals(GameObject.ObjectType.TREASURE) && bd2.getType().equals(GameObject.ObjectType.RAFT)){
                ResolveCollision((Raft)bd2, (Treasure) bd1);
            }
            // Check for win condition
            else if ((bd1 == levelModel.getPlayer() && bd2 == levelModel.getGoal()) ||
                    (bd1 == levelModel.getGoal() && bd2 == levelModel.getPlayer())) {
                if (!complete && !failed)
                    setComplete(true);
            }
            // Check for hydra collision with bullet
            else if(bd1.getType().equals(GameObject.ObjectType.BULLET) && bd2.getType().equals(GameObject.ObjectType.HYDRA)){
                ResolveCollision((Hydra) bd2, (Bullet) bd1);
            } else if(bd1.getType().equals(GameObject.ObjectType.HYDRA) && bd2.getType().equals(GameObject.ObjectType.BULLET)){
                ResolveCollision((Hydra) bd1, (Bullet) bd2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method for the end of a collision
     *
     * This method is called when two objects cease to touch.
     */
    @Override
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();
        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            GameObject bd1 = (GameObject)body1.getUserData();
            GameObject bd2 = (GameObject)body2.getUserData();
            // Check for object interaction with current
            if(bd1.getType().equals(GameObject.ObjectType.CURRENT)){
                exitCurrent((Current) bd1, bd2);
            } else if(bd2.getType().equals(GameObject.ObjectType.CURRENT)){
                exitCurrent((Current) bd2, bd1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Push o according to c
     */
    private void enterCurrent(Current c, GameObject o) {
        // check for "ghost coll
        if(o.getType() == GameObject.ObjectType.RAFT) { // TODO I don't know how to solve this problem
            float dx = 2*Math.abs(c.getPosition().x - o.getPosition().x);
            float dy = 2*Math.abs(c.getPosition().y - o.getPosition().y);
            if (!(dx < c.getWidth() + ((Raft)o).getWidth() && dy < c.getHeight() + ((Raft)o).getHeight()))
                System.out.println("enterCurrent was called, but the raft wasn't in the current :( ");
            o.enterCurrent(c.getDirectionVector());
        } else {
            o.enterCurrent(c.getDirectionVector());
        }
    }

    /**
     * Push o according to c
     */
    private void exitCurrent(Current c, GameObject o) {
        o.exitCurrent(c.getDirectionVector());
    }

//    /** Place the object back to its position cache to revert its most recent movement
//     * @param o the game object to revert */
//    private void revertRecentMovement(GameObject o) { o.setPosition(o.getPositionCache()); }

    /** Resolve collision between two objects of specific types
     * @param b bullet
     * @param e enemy */
    private void ResolveCollision(Bullet b, GameObject e) {
        if(e.getType() == GameObject.ObjectType.ENEMY) {
            // destroy enemy
            e.setDestroyed(true);
        }
        // destroy bullet
        removeBullet(b);
    }

    /** Resolve collision between two objects of specific types
     * @param r raft
     * @param e enemy */
    private void ResolveCollision(Raft r, Shark e) {
        // update player health
        r.addHealth(Shark.ENEMY_DAMAGE);
        // destroy enemy
        e.setDestroyed(true);
    }

    /** Resolve collision between two objects of specific types
     * @param r raft
     * @param w wood */
    private void ResolveCollision(Raft r, Wood w) {
        // update player health
        r.addHealth(w.getWood());
        // destroy wood
        w.setDestroyed(true);
    }

    /** Resolve collision between two objects of specific types
     * @param r raft
     * @param t treasure */
    private void ResolveCollision(Raft r, Treasure t) {
        // update player health
        r.addStar();
        // destroy treasure
        t.setDestroyed(true);
        // add random wood
        levelModel.addRandomWood();
        // update player score
        playerScore++;
    }

    private void ResolveCollision(Hydra h, Bullet b){
        // Hydra gets stunned
        h.setHit(true);
        b.setDestroyed(true);
    }

    /** Unused ContactListener method. May be used to play sound effects */
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        float speed = 0;
        Vector2 cache = new Vector2();
        float bumpThresh = 5f;

        // Use Ian Par-berry's method to compute a speed threshold
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();
        WorldManifold worldManifold = contact.getWorldManifold();
        Vector2 wp = worldManifold.getPoints()[0];
        cache.set(body1.getLinearVelocityFromWorldPoint(wp));
        cache.sub(body2.getLinearVelocityFromWorldPoint(wp));
        speed = cache.dot(worldManifold.getNormal());

        // Play a sound if above threshold (otherwise too many sounds)
        GameObject.ObjectType s1 = ((GameObject)body1.getUserData()).getType();
        GameObject.ObjectType s2 = ((GameObject)body2.getUserData()).getType();
        if (s1 == GameObject.ObjectType.RAFT && s2 == GameObject.ObjectType.ENEMY
                || s1 == GameObject.ObjectType.ENEMY && s2 == GameObject.ObjectType.RAFT) {
            SoundController.getInstance().playSFX("raft_damage", false);
        }
        if ((s1 == GameObject.ObjectType.RAFT && s2 == GameObject.ObjectType.WOOD)
                || s1 == GameObject.ObjectType.WOOD && s2 == GameObject.ObjectType.RAFT) {
            SoundController.getInstance().playSFX("wood_pickup", false);
        }
        if (s1 == GameObject.ObjectType.RAFT && s2 == GameObject.ObjectType.TREASURE
                || s1 == GameObject.ObjectType.TREASURE && s2 == GameObject.ObjectType.RAFT) {
            SoundController.getInstance().playSFX("chest_collect", false);
        }
    }

    /** Play sound effect according to the situation */
    private void playSoundEffect() {

    }

    /** Unused ContactListener method */
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    /*=*=*=*=*=*=*=*=*=* Set and Reset Level *=*=*=*=*=*=*=*=*=*/
    /**
     * Sets whether the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    public void setComplete(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        complete = value;
    }

    /**
     * Returns true if the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure( ) {return failed;}

    /**
     * Sets whether the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @param value whether the level is failed.
     */
    public void setFailure(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        failed = value;
    }

    /** Empty the level game objects */
    public void emptyLevel() {
        levelModel.reset();
        levelModel.world.setContactListener(this);
        setComplete(false);
        setFailure(false);
    }

    private void populateControllers(PooledList<?> e){

    }


    /** Prepare the AI for the enemy in the level */
    public void prepareEnemy(){
        PooledList<Shark> enemies = levelModel.getEnemies();
        controls = new SharkController[enemies.size()];
        for (int i = 0; i < enemies.size(); i++) {
            controls[i] = new SharkController(i, enemies.get(i), levelModel.getPlayer());
        }
    }

    /** The current level id. */
    private int level_id = 0;

    /**
     * Populate the level according to the new level selection.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void setLevel(int level_int){
        level_id = level_int;
        JsonValue level_data = directory.getEntry("level:" + level_int, JsonValue.class);
        emptyLevel();
        levelModel.loadLevel(level_int, level_data);
        prepareEnemy();
        stage.clear();
        table = new Table();
        playerScore = 0;
        skin = new Skin();
        transitionBuilt = false;
        settingsPressed = false;
        SoundController.getInstance().setMusicPreset(level_data.getInt("music_preset", 1));
        SoundController.getInstance().startLevelMusic();

        // the following could be changed so that it only recalculates a flowmap the first time it loads a level, if
        // this operation is found to be too slow. However, I've found that it's not that slow, so this is unnecessary.
        if (USE_SHADER_FOR_WATER)
            canvas.setFlowMap(levelModel.recalculateFlowMap());
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        SoundController.getInstance().haltSounds();
        setLevel(level_id);
    }

}
