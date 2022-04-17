package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.gdiac.util.PooledList;

import java.util.Iterator;

public class WorldController implements Screen, ContactListener {


    // TODO -- add enums to switch between regular play mode, settings, pause, and transition

    /**
     * Method to call after loading to set all constants in World Controller,
     * @param objParams JsonValue of object_parameters instance.
     */
    public static void setConstants(JsonValue objParams){
//        EXIT_QUIT = objParams.getInt("exit quit", 0);
        Spear.setConstants(objParams.get("bullet"));
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
    private static final float HEALTH_BAR_SCALE = 0.6f;


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
    private InputMultiplexer plexer;

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
    /** Texture for game background */
    protected Texture gameBackground;
    /** Texture for pause screen */
    protected Texture pauseBackground;
    /** Texture for game background when using shader */
    protected Texture blueTexture;
    /** Texture for failed level */
    protected Texture failedBackground;
    /** Texture for success backgrounds where the index corresponds to the score */
    protected Texture[] successBackgrounds;
    /** Whether the pause screen is built */
    private boolean pauseBuilt;
    /** Whether the transition screen is built  */
    private boolean transitionBuilt;

    // WORLD
    protected LevelModel levelModel;

    // LEVEL STATUS
    /** Whether this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** The long id for the current sfx playing to prevent duplication. */
    private boolean wasComplete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether the next button was clicked */
    private boolean nextPressed;
    /** Whether the pause button was clicked on the transition screen */
    private boolean pausePressed;
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
    /** Whether the settings button was pressed */
    private boolean settingsPressed;
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
        this.wasComplete = false;
        this.failed = false;
        this.map  = false;
        this.debug = false;
        this.active = false;
        this.countdown = -1;
        this.playerScore = 0;
        this.nextPressed = false;
        this.exitPressed = false;
        this.stage = new Stage();
        this.skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        this.table = new Table();
        this.plexer = new InputMultiplexer();
        hydraSight = new HydraRayCast();
        startTime = System.currentTimeMillis();
        pauseBuilt = false;
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
        if(canvas == null)
            return; // return if no canvas pointer
        if (!canvas.shaderCanBeUsed)
            USE_SHADER_FOR_WATER = false; // disable shader if reading shader files failed (e.g. on Mac)

        // update animations
        float time = (System.currentTimeMillis() - startTime)/1000.0f;
        int frame = (int)(time*15.0f);// TODO don't hardcode animation speed
        levelModel.getPlayer().animationFrame = frame % 19; // TODO don't hardcode number of frames in animation
        float pixelsPerUnit = 100.0f/3.0f; // Tiles are 100 pixels wide, a tile is 3 units
        Affine2 cameraTransform = calculateMovingCamera(pixelsPerUnit);

        canvas.clear();
        canvas.begin(cameraTransform);
        drawWater();
        for(GameObject obj : levelModel.getObjects()) {

            if (!USE_SHADER_FOR_WATER || obj.getType() != GameObject.ObjectType.CURRENT) {
                if (obj.getType() == GameObject.ObjectType.SHARK) {
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
        // draw a circle showing how far the player can move before they die
        float r = levelModel.getPlayer().getPotentialDistance() * pixelsPerUnit;
        canvas.drawHealthCircle((int)playerPosOnScreen.x, (int)playerPosOnScreen.y, r);

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
                if (type != GameObject.ObjectType.TREASURE && type != GameObject.ObjectType.SHARK
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
        if (pausePressed) {
            drawPause();
        }
        if (complete && !failed || failed) {
            if(!wasComplete && complete) {
                SoundController.getInstance().playSFX("level_complete");
                SoundController.getInstance().setLevelComplete();
                wasComplete = true;
            }
            SoundController.getInstance().fadeMusic();
            drawTransition();
        }
    }

    private void drawTransparentOverlay() {
        Pixmap pixmap = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fillRectangle(0, 0, 1, 1);
        Texture darkBackgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Image transparentBackgroundTexture = new Image(darkBackgroundTexture);
        transparentBackgroundTexture.setSize(canvas.getWidth(),canvas.getHeight());
        transparentBackgroundTexture.setColor(0, 0, 0, 0.6f);
        stage.addActor(transparentBackgroundTexture);
    }

    private void drawPause() {
        if (!pauseBuilt) {
            pauseBuilt = true;
            drawTransparentOverlay();
            table.setFillParent(true);
            table.align(Align.center);
            stage.addActor(table);
            skin.add("pause_background", pauseBackground);
            table.setBackground(skin.getDrawable("pause_background"));

            TextButton resumeButton = new TextButton("RESUME", skin);
            resumeButton.getLabel().setFontScale(0.4f);
            resumeButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    resumeButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    resumeButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SoundController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    pausePressed = false;
                }
            });
            table.add(resumeButton);
            table.row();

            TextButton restartButton = new TextButton("RESTART", skin);
            restartButton.getLabel().setFontScale(0.4f);
            restartButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    restartButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    restartButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SoundController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    reset();
                }
            });
            table.add(restartButton);
            table.row();

            TextButton settingsButton = new TextButton("SETTINGS", skin);
            settingsButton.getLabel().setFontScale(0.4f);
            table.add(settingsButton);
            settingsButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    settingsButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    settingsButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SoundController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    settingsPressed = true;

                }
            });
            table.row();

            TextButton exitButton = new TextButton("EXIT", skin);
            exitButton.getLabel().setFontScale(0.4f);
            exitButton.getLabel().setColor(Color.GOLD);
            exitButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    exitButton.getLabel().setColor(Color.GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    exitButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SoundController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    exitPressed = true;
                }
            });
            table.add(exitButton);
        }
        stage.act();
        stage.draw();
    }

    private void drawTransition() {
        if (!transitionBuilt) {
            transitionBuilt = true;
            if (complete && !failed) {
                buildTransitionScreen(successBackgrounds[Math.min(playerScore, successBackgrounds.length-1)], false);
            } else {
                buildTransitionScreen(failedBackground, true);
            }
        }
        stage.act();
        stage.draw();
    }

    private void buildTransitionScreen(Texture background, boolean didFail) {
        table.clear();
        stage.clear();
        drawTransparentOverlay();
        table.setFillParent(true);
        table.align(Align.center);
        stage.addActor(table);
        skin.add("transition_background", background);
        table.setBackground(skin.getDrawable("transition_background"));

        Table part1 = new Table();
        TextButton mainButton = new TextButton(didFail ? "RESTART" : "NEXT", skin);
        mainButton.getLabel().setFontScale(0.5f);
        float buttonWidth =  mainButton.getWidth();
        mainButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                mainButton.getLabel().setColor(Color.GOLD);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                mainButton.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SoundController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                if (didFail) {
                    reset();
                } else {
                    nextPressed = true;
                }
            }
        });
        part1.add(mainButton).expandX().align(Align.center);
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.row().colspan(didFail ? 2 : 3);
        if (!didFail) {
            TextButton replayButton = new TextButton("REPLAY", skin);
            replayButton.getLabel().setFontScale(0.4f);
            replayButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    replayButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    replayButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SoundController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    reset();
                }
            });
            part2.add(replayButton).expandX().padRight(70);
        }

        TextButton settingsButton = new TextButton("SETTINGS", skin);
        settingsButton.getLabel().setFontScale(0.4f);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                settingsButton.getLabel().setColor(Color.GOLD);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                settingsButton.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SoundController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                settingsPressed = true;
            }
        });
        part2.add(settingsButton).expandX();

        TextButton exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(0.4f);
        exitButton.getLabel().setColor(Color.GOLD);
        exitButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SoundController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                exitButton.getLabel().setColor(Color.GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                exitButton.getLabel().setColor(Color.GOLD);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SoundController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                exitPressed = true;
            }
        });
        part2.add(exitButton).expandX();
        table.add(part2);
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
        float y_origin = (player_position.y + 20);
        canvas.draw(greyBar,Color.WHITE,x_origin,y_origin,greyBar.getRegionWidth(),greyBar.getRegionHeight());
        if(health >= 0){canvas.draw(RatioBar,c,x_origin,y_origin,RatioBar.getRegionWidth(),RatioBar.getRegionHeight());}
    }

    /** draws background water (for the sea) and moving currents (using shader)
     * Precondition & post-condition: the game canvas is open */
    private void drawWater() {
        if (USE_SHADER_FOR_WATER)
            canvas.useShader((System.currentTimeMillis() - startTime) / 1000.0f);
        float pixel = 1;
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
        mapBackground = directory.getEntry("map_background", Texture.class);
        gameBackground = directory.getEntry("background", Texture.class);
        pauseBackground = directory.getEntry("pause_background", Texture.class);
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
     * Returns whether to process the update loop.
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
        // NEW: Ask the level model to process current effects on objects
        levelModel.updateAllCurrentEffects();

        InputController input = InputController.getInstance();
        input.readInput();

        if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap()) {
            map = !map;
            SoundController.getInstance().playSFX("map_open");
        } // Toggle map
//        if (input.didTab()) { SoundController.getInstance().playSFX("level_complete");}

        // Now it is time to maybe switch screens.
        if (input.didExit() || exitPressed) {
            pause();
            exitPressed = false;
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        } else if (input.didNext() || nextPressed) {
            pause();
            nextPressed = false;
            listener.exitScreen(this, EXIT_NEXT);
            return false;
        }  else if (settingsPressed) {
            pause();
            settingsPressed = false;
            listener.exitScreen(this, EXIT_SETTINGS);
            return false;
        } else if (input.didPause() || pausePressed)  {
            pause();
            pausePressed = true;
            return false;
        } else if (input.didPrevious()) {
            pause();
            listener.exitScreen(this, EXIT_PREV);
            return false;
        } else if (input.didReset()) {
            reset();
        }
        // Level completed or failed
        if (complete || failed) { return false; }
        // Start countdown otherewise.
        if (countdown > 0) {
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
            Vector2 firePixel = ic.getFireDirection();
            Affine2 camera = calculateMovingCamera(100/3.0f);
            camera.inv().applyTo(firePixel);
            levelModel.createSpear(firePixel);
            player.addHealth(Spear.SPEAR_DAMAGE);
            SoundController.getInstance().playSFX("spear_throw");
        }

        // update forces for enemies, players, objects
        player.applyInputForce();
        resolveEnemies(dt);
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
//            if(h.willAttack()) {createBullet(h.getPosition(), levelModel.getPlayer());}
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
            if(obj.getType() == GameObject.ObjectType.SPEAR) levelModel.checkSpear((Spear) obj);
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

    // TODO: where should this field belong?
    private boolean wasInDanger = false;

    /** Update the level themed music according the game status */
    private void resolveMusic() {
        boolean nowInDanger = false;
        for(SharkController ai : controls){
            if(ai.isAlive() && ai.getState() == Shark.enemyState.ENRAGE){
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
        Gdx.input.setInputProcessor(stage);
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
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();
        try {
            GameObject bd1 = (GameObject) body1.getUserData();
            GameObject bd2 = (GameObject) body2.getUserData();
            // Check for bullet collision with object (terrain or enemy)
            if (bd1.getType() == GameObject.ObjectType.SPEAR) {
                ResolveSpearCollision((Spear) bd1, bd2);
            } else if (bd2.getType() == GameObject.ObjectType.SPEAR) {
                ResolveSpearCollision((Spear) bd2, bd1);
            }
            // Check for player collision with wood (health+)
            else if(bd1.getType() == GameObject.ObjectType.RAFT){
                ResolveRaftCollision((Raft) bd1, bd2);
            } else if(bd2.getType() == GameObject.ObjectType.RAFT){
                ResolveRaftCollision((Raft) bd2, bd1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Resolve collision between two objects of specific types
     * @param b bullet
     * @param g enemy */
    private void ResolveSpearCollision(Spear b, GameObject g) {
        if(g.getType() == GameObject.ObjectType.SHARK) {
            // stun shark
            g.setDestroyed(true);
        }
        if(g.getType() == GameObject.ObjectType.HYDRA) {
            // stun hydra
            ((Hydra) g).setHit(true);
        }
        // destroy bullet
        b.setDestroyed(true);
    }

    private void ResolveRaftCollision(Raft r, GameObject g){
        if(g.getType() == GameObject.ObjectType.WOOD){
            // update player health
            r.addHealth(((Wood) g).getWood());
            SoundController.getInstance().playSFX("wood_pickup");
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.SHARK){
            // update player health
            r.addHealth(Shark.ENEMY_DAMAGE);
            SoundController.getInstance().playSFX("raft_damage");
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.TREASURE){
            // add random wood and update player score
            r.addStar();
            levelModel.addRandomWood();
            playerScore++;
            SoundController.getInstance().playSFX("chest_collect");
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.GOAL){
            // Check player win
            if (!complete && !failed) setComplete(true);
        }
    }

    /** Unused Callback method for the end of a collision.*/
    @Override
    public void endContact(Contact contact) {
    }
    /** Unused ContactListener method.*/
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }
    /** Unused ContactListener method.*/
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
        System.out.println("Loaded level "+level_int);
        emptyLevel();
        levelModel.loadLevel(level_int, level_data);
        prepareEnemy();
        stage.clear();
        table.clear();
        plexer.clear();
        playerScore = 0;
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        transitionBuilt = false;
        pausePressed = false;
        pauseBuilt = false;
        settingsPressed = false;
        wasComplete = false;

        // Reset Soundcontroller
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
        SoundController.getInstance().haltMusic();
        setLevel(level_id);
    }

}
