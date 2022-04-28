package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.badlogic.gdx.utils.Timer;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.model.*;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Note;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Spear;
//import edu.cornell.gdiac.raftoftheseus.model.unused.Hydra;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.raftoftheseus.singleton.MusicController;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.gdiac.util.PooledList;

import java.util.Iterator;
import static edu.cornell.gdiac.raftoftheseus.GDXRoot.*;

public class WorldController implements Screen, ContactListener {


    // TODO -- add enums to switch between regular play mode, settings, pause, and transition

    /**
     * Method to call after loading to set all constants in World Controller,
     * @param objParams JsonValue of object_parameters instance.
     */
    public static void setConstants(JsonValue objParams){
//        EXIT_QUIT = objParams.getInt("exit quit", 0);
        Raft.setConstants(objParams.get("raft"));
        Spear.setConstants(objParams.get("spear"));
        Note.setConstants(objParams.get("note"));
        Shark.setConstants(objParams.get("shark"));
//        Hydra.setConstants(objParams.get("hydra"));
        Siren.setConstants(objParams.get("siren"));
        Rock.setConstants(objParams.get("rock"));
        JsonValue world = objParams.get("world");
        EXIT_COUNT = world.getInt("exit count", 1000);
        WORLD_STEP = 1/world.getFloat("world step", 60f);
        WORLD_VELOCITY = world.getInt("world velocity", 6);
        WORLD_POSIT = world.getInt("world posit", 2);
        shaderData = objParams.get("shader");
    }

    // CONSTANTS
    /** How many frames after winning/losing do we continue? */
    public static int EXIT_COUNT = 1000;
    /** Number of tutorial levels */
    public static int TUTORIAL_COUNT = 5;
    /** The amount of time for a physics engine step. */
    public static float WORLD_STEP;
    /** Number of velocity iterations for the constraint solvers */
    public static int WORLD_VELOCITY;
    /** Number of position iterations for the constraint solvers */
    public static int WORLD_POSIT;

    private static JsonValue shaderData;

    // FIELDS
    // CANVAS AND OBJECT LIST
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Reference to the game assets directory */
    private AssetDirectory directory;
    /** Game level save data */
    private static JsonValue saveData;

    // UI Elements
    private Stage stage;
    private Table table;
    private Skin skin;

    // TEXTURE
    /** The texture for the star */
    protected TextureRegion star;
    /** The hint image for movement and exit/reset controls */
    protected TextureRegion hintMovement;
    /** The hint image for firing controls */
    protected TextureRegion hintAttack;
    /** The hint image for map controls */
    protected TextureRegion hintMap;
    /** Texture for pause screen */
    protected Texture pauseBackground;
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
    /** Find whether an enemy can see the player. */
    private EnemyRayCast enemySight;
    /** Whether the settings button was pressed */
    private boolean settingsPressed;
    /** Whether the exit button was pressed */
    private boolean exitPressed;

    private final long startTime;

    // SHADER STUFF
    private float[] raftSamplePositionsXY = new float[16];
    private float[] raftSampleSpeeds = new float[8];
    private float raftSampleLastTime = 0.0f;

    /**
     * Creates a new game world
     *
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2d coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected WorldController(GameCanvas canvas) {
        this.canvas = canvas;
        levelModel = new LevelModel(canvas);
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
        enemySight = new EnemyRayCast();
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
        canvas.clear();

        // update animations
        levelModel.getPlayer().updateSpear(dt);
        levelModel.getPlayer().setAnimationFrame(dt);
        for(Siren s : levelModel.getSirens()){
            s.setAnimationFrame(dt);
        }
        for(Shark s : levelModel.getEnemies()){
            s.setAnimationFrame(dt);
        }

        // Update raft samples (for displaying the wake in the shader) before drawing water
        updateRaftWakeSamples();

        // Draw the level
        levelModel.updateCameraTransform();
        levelModel.draw((System.currentTimeMillis() - startTime) / 1000.0f);
        if(level_id > 5){ levelModel.renderLights(); } // New Added: Draw the light effects! if level is 4 up.

        // draw stars
        drawStar(playerScore);

        // draw control hints
        drawControlHints(dt);

        // draw interfaces
        if (debug) {
            levelModel.drawDebug();
        }
        if (map) {
            levelModel.drawMap();
        }
        if (pausePressed) {
            drawPause();
        }
        if (complete || failed) {
            if(!wasComplete && complete) {
                SfxController.getInstance().playSFX("level_complete");
                SfxController.getInstance().setLevelComplete();
                wasComplete = true;
                saveGame();
            }
            SfxController.getInstance().fadeMusic();
            drawTransition();
        }
    }

    private float hintTimer = 0f;
    private float fadeTimeStart = 5f;
    private float fadeTimeEnd = 6f;
    private float fadeTimeSpan = fadeTimeEnd - fadeTimeStart;
    private boolean blockHint = false;

    private void resetControlHints(){
        hintTimer = 0;
        blockHint = false;
    }

    private void drawControlHints(float dt) {
        if(blockHint) return;
        hintTimer += dt;
        TextureRegion hint = null;
        switch(level_id) {
            case(0):
                hint = hintMovement;
                break;
            case(2):
                hint = hintMap;
                break;
            case(4):
                hint = hintAttack;
                break;
            default:
                // draw nothing
        }
        if(hintTimer >= fadeTimeEnd) {
            blockHint = true;
            return;
        }
        if (hint != null) {
            canvas.begin();
            Color c;
            if(hintTimer >= fadeTimeStart){
                c = new Color(1f, 1f, 1f, (fadeTimeEnd - hintTimer)/fadeTimeSpan);
            } else {
                c = Color.WHITE;
            }
            canvas.draw(hint, c, hint.getRegionWidth()*0.5f, 0.0f, canvas.getWidth()*0.5f, canvas.getHeight()*0.5f + 120, hint.getRegionWidth(), hint.getRegionHeight());
            canvas.end();
        }

    }

    private void saveGame() {
        saveData.get("level_data").get(level_id).get("complete").set(true);
        // if the treasure available in the level is 0, the player gets 3 stars for completing
        int score = levelModel.getTreasureCount() > 0 ? playerScore : 3;
        saveData.get("level_data").get(level_id).get("score").set(score, Integer.toString(score));
        if (level_id + 1 < NUM_LEVELS) {
            saveData.get("level_data").get(level_id + 1).get("unlocked").set(true);
        }
    }

    private void updateRaftWakeSamples() {
        float time = (System.currentTimeMillis() - startTime)/1000.0f;
        float timeSince = time - raftSampleLastTime;
        float timeInterval = 0.15f;
        if(timeSince > timeInterval) {
            // add a new sample and discard the oldest one
            raftSampleLastTime = time;
            Vector2 pos = levelModel.getPlayer().getPosition().scl(1.0f/levelModel.getTileSize());
            pos.add(1, 1); // offset due to extra grid added in water map
            // TODO figure out why speed is inaccurate when on currents? or don't, it might look better this way
            float speed = levelModel.getPlayer().getLinearVelocity().len() * (1.0f/levelModel.getTileSize());
//            Vector2 lastPos = new Vector2(raftSamplePositionsXY[0], raftSamplePositionsXY[1]);
//            float speed = lastPos.sub(pos).len()/timeInterval; // approximate speed
            for(int i = raftSampleSpeeds.length-1; i >= 1; i--) {
                raftSampleSpeeds[i] = raftSampleSpeeds[i-1];
                raftSamplePositionsXY[2*i] = raftSamplePositionsXY[2*(i-1)];
                raftSamplePositionsXY[2*i+1] = raftSamplePositionsXY[2*(i-1)+1];
            }
            raftSampleSpeeds[0] = speed;
            raftSamplePositionsXY[0] = pos.x;
            raftSamplePositionsXY[1] = pos.y;

            canvas.setRaftSamples(raftSamplePositionsXY, raftSampleSpeeds);
            canvas.setRaftSampleTime(0.0f);
        } else {
            canvas.setRaftSampleTime(timeSince);
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

    /** Helper method to pass for button creation and concealing pause reseting. */
    private void resetPausePressed(){ pausePressed = false; }

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
            resumeButton.getLabel().setFontScale(0.5f);
            resumeButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    resumeButton.getLabel().setColor(Color.LIGHT_GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    resumeButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SfxController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    pausePressed = false;
                }
            });
            table.add(resumeButton).padTop(-20);
            table.row();

            TextButton restartButton = new TextButton("RESTART", skin);
            restartButton.getLabel().setFontScale(0.5f);
            restartButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    restartButton.getLabel().setColor(Color.LIGHT_GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    restartButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SfxController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    reset();
                }
            });
            table.add(restartButton);
            table.row();

            TextButton settingsButton = new TextButton("SETTINGS", skin);
            settingsButton.getLabel().setFontScale(0.5f);
            table.add(settingsButton);
            settingsButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    settingsButton.getLabel().setColor(Color.LIGHT_GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    settingsButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SfxController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    settingsPressed = true;

                }
            });
            table.row();

            TextButton exitButton = new TextButton("EXIT", skin);
            exitButton.getLabel().setFontScale(0.5f);
            exitButton.getLabel().setColor(Color.GOLD);
            exitButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    exitButton.getLabel().setColor(Color.LIGHT_GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    exitButton.getLabel().setColor(Color.GOLD);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SfxController.getInstance().playSFX("button_click");
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
                buildTransitionScreen(successBackgrounds[level_id < TUTORIAL_COUNT ? successBackgrounds.length - 1 : playerScore], false);
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
        mainButton.getLabel().setFontScale(0.6f);
        float buttonWidth =  mainButton.getWidth();
        mainButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                mainButton.getLabel().setColor(Color.LIGHT_GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                mainButton.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SfxController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                if (didFail) {
                    reset();
                } else {
                    nextPressed = true;
                }
            }
        });
        part1.add(mainButton).expandX().align(Align.center).padTop(10);
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.row().colspan(didFail ? 2 : 3);
        if (!didFail) {
            TextButton replayButton = new TextButton("REPLAY", skin);
            replayButton.getLabel().setFontScale(0.5f);
            replayButton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    super.enter(event, x, y, pointer, fromActor);
                    replayButton.getLabel().setColor(Color.LIGHT_GRAY);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    replayButton.getLabel().setColor(Color.WHITE);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    SfxController.getInstance().playSFX("button_click");
                    super.clicked(event, x, y);
                    reset();
                }
            });
            part2.add(replayButton).expandX().padRight(70);
        }

        TextButton settingsButton = new TextButton("SETTINGS", skin);
        settingsButton.getLabel().setFontScale(0.5f);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                settingsButton.getLabel().setColor(Color.LIGHT_GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                settingsButton.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SfxController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                settingsPressed = true;
            }
        });
        part2.add(settingsButton).expandX();

        TextButton exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(0.5f);
        exitButton.getLabel().setColor(Color.GOLD);
        exitButton.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
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
                SfxController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                exitPressed = true;
            }
        });
        part2.add(exitButton).expandX();
        table.add(part2);
        table.row();
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
        // UI things
        star = new TextureRegion(directory.getEntry( "star", Texture.class ));
        pauseBackground = directory.getEntry("pause_background", Texture.class);
        failedBackground = directory.getEntry("failed_background", Texture.class);
        successBackgrounds = new Texture[4];
        for (int i = 0; i < 4; i++) {
            successBackgrounds[i] = directory.getEntry("success_background_" + i, Texture.class);
        }
        hintAttack = new TextureRegion(directory.getEntry( "hint_attack", Texture.class ));
        hintMovement = new TextureRegion(directory.getEntry( "hint_movement", Texture.class ));
        hintMap = new TextureRegion(directory.getEntry( "hint_map", Texture.class ));

        // shader things
        Texture waterDiffuse = directory.getEntry("water_diffuse", Texture.class);
        waterDiffuse.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture waterNormal = directory.getEntry("water_normal", Texture.class);
        waterNormal.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture waterUVOffset = directory.getEntry("water_uv_offset", Texture.class);
        waterUVOffset.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        canvas.setWaterTextures(waterDiffuse, waterNormal, waterUVOffset);

        // level model assets
        levelModel.setDirectory(directory);
        levelModel.gatherAssets(directory);
        this.directory = directory;

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
        // NEW*: Ask the level model to process current effects on objects and light effects :*NEW
        levelModel.updateAllCurrentEffects(dt);
        levelModel.updateLights();

        // Read the player input
        InputController input = InputController.getInstance();
        input.readInput();
        if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap()) {
            map = !map;
            SfxController.getInstance().playSFX("map_open");
        } // Toggle map

        // Now it is time to maybe switch screens. First, check for input trigger screen switch
        if (input.didExit() || exitPressed) {
            pause();
            exitPressed = false;
            listener.exitScreen(this, QUIT);
            return false;
        } else if (input.didNext() || nextPressed) {
            pause();
            nextPressed = false;
            listener.exitScreen(this, NEXT_LEVEL);
            return false;
        }  else if (settingsPressed) {
            pause();
            settingsPressed = false;
            listener.exitScreen(this, WORLD_TO_SETTINGS);
            return false;
        } else if (input.didPause() || pausePressed)  {
            pause();
            pausePressed = true;
            return false;
        } else if (input.didPrevious()) {
            pause();
            listener.exitScreen(this, PREV_LEVEL);
            return false;
        } else if (input.didReset()) {
            reset();
        }
        // Then, handle resets trigger by completed or failed
        if (complete || failed) { return false; }
        // Start countdown otherwise.
        if (countdown > 0) {
            countdown--;
        } else if (countdown == 0) {
            if (failed) {
                reset();
            } else if (complete) {
                pause();
                listener.exitScreen(this, NEXT_LEVEL);
                return false;
            }
        }
        return true;
    }

    private boolean wasCharging = false;

    /** The core gameplay loop of this world. This method is called after input is read, but before collisions
     * are resolved. The very last thing that it should do is apply forces to the appropriate objects.
     * @param dt	Number of seconds since last animation frame */
    public void update(float dt){
        // Process actions in object model
        InputController ic = InputController.getInstance();
        Raft player = levelModel.getPlayer();
        player.setMovementInput(ic.getMovement());
        player.beginCharging(ic.didCharge());

        // Play sfx
        if(!wasCharging && player.isCharging()){
            wasCharging = true;
            SfxController.getInstance().playSFX("spear_charge");
        }

        // Create spear when possible
        if(player.canFire() && !player.hasSpear()){
            levelModel.createSpear();
        }

        // Move spear move after firing.
        if (player.canFire() && ic.didCharge() && player.hasSpear()) {
            // find the nearest enemy to player
            player.resetCanFire();
            Vector2 firePixel = ic.getFireDirection();
            levelModel.getCameraTransform().inv().applyTo(firePixel);
            levelModel.fireSpear(firePixel);
            SfxController.getInstance().playSFX("spear_throw");
            wasCharging = false;
        }

        // update forces for enemies, players, objects
        player.applyInputForce();
        player.applyProjectileForce();
        resolveEnemies(dt);

        // update light choice
        if(ic.didChange()){ levelModel.change(debug); }
    }

    /** get enemies take actions according to their AI */
    private void resolveEnemies(float dt) {
//        PooledList<Shark> el = levelModel.getEnemies();
        for (int i = 0; i< controls.length; i++) {
//            Shark shark = el.get(i);
//            shark.resolveAction(controls[i].getAction(), levelModel.getPlayer(), controls[i].getTicks());
            controls[i].updateShark();
        }

//        for (Hydra h : levelModel.getHydras()) {
//            levelModel.world.rayCast(hydraSight, h.getPosition(), levelModel.getPlayer().getPosition());
//            h.setSee(hydraSight.getCanSee());
////            if(h.willAttack()) {createBullet(h.getPosition(), levelModel.getPlayer());}
//            h.update(dt);
//        }

        for(Siren s : levelModel.getSirens()){
            s.update(dt);
            if(s.willAttack()){
                levelModel.createNote(s.getPosition().cpy(), s.getTargetDirection());
            }
        }
    }

    /** Processes physics
     * Once the update phase is over, but before we draw, we are ready to handle physics.
     * The primary method is the step() method in world. Also, update player health and garbage collection
     * @param dt	Number of seconds since last animation frame */
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
        if(player.isDead() && !complete && !failed && !debug){
            setFailure(true);
        }

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<GameObject>.Entry> iterator = levelModel.getObjects().entryIterator();
        while (iterator.hasNext()) {
            PooledList<GameObject>.Entry entry = iterator.next();
            GameObject obj = entry.getValue();
            if(obj.getType() == GameObject.ObjectType.SPEAR) {
                if(levelModel.checkProjectile((Spear) obj)) SfxController.getInstance().playSFX("spear_splash");
            } else if (obj.getType() == GameObject.ObjectType.NOTE) {
                levelModel.checkProjectile((Note) obj);
            }
            if (obj.isDestroyed()) {
                obj.deactivatePhysics(levelModel.world);
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);
            }
        }
        resolveMusic();
        resolveSFX(player);
    }

    private boolean useThread = false;
    private boolean sharkWas = false;
    private boolean sirenWas = false;

    /** Update the level themed music according the game status */
    private void resolveMusic() {
        // Get danger
        boolean sharkNow = false;
        boolean sirenNow = false;
        for(SharkController ai : controls){
            if(ai.isAlive() && ai.getState() == Shark.enemyState.ENRAGE){
                sharkNow = true;
                break;
            }
        }
        for(Siren s : levelModel.getSirens()){
            if(s.canHear()){
                sirenNow = true;
                break;
            }
        }

        // Update music
        if(useThread) {
            MusicController.getInstance().updateMusic(sharkNow, sirenNow);
        } else {
            boolean wasInDanger = sharkWas || sirenWas;
            boolean nowInDanger = sharkNow || sirenNow;
            if(!wasInDanger && nowInDanger){
                SfxController.getInstance().tradeMusic(false);
            }
            if(wasInDanger && !nowInDanger){
                SfxController.getInstance().tradeMusic(true);
            }
            SfxController.getInstance().updateMusic();
        }
        sirenWas = sirenNow;
        sharkWas = sharkNow;
    }

    private boolean wasMoving = false;

    /**
     * Start and stop sail sounds if necessary.
     * @param player
     */
    private void resolveSFX(Raft player){
        // Update raft sail sounds
        if(!wasMoving && player.isMoving()){
            SfxController.getInstance().playSFX("raft_sail_wind", true);
            wasMoving = true;
        } else if(wasMoving && !player.isMoving()){
            SfxController.getInstance().stopSFX("raft_sail_wind");
//            SfxController.getInstance().playSFX("raft_sail_down");
            wasMoving = false;
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

    /** Sets the save data */
    public void setSaveData(JsonValue saveData) { this.saveData = saveData; }

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
            // Check for player collision
            else if(bd1.getType() == GameObject.ObjectType.RAFT){
                ResolveRaftCollision((Raft) bd1, bd2);
            } else if(bd2.getType() == GameObject.ObjectType.RAFT){
                ResolveRaftCollision((Raft) bd2, bd1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Resolve collision between the spear and any other object.
     * @param s spear
     * @param g terrain or enemy */
    private void ResolveSpearCollision(Spear s, GameObject g) {
        if(g.getType() == GameObject.ObjectType.SHARK) {
            // stun shark
            SfxController.getInstance().playSFX("spear_enemy_hit");
            SfxController.getInstance().playSFX("shark_hit");
            s.setDestroyed(true);
            ((Shark)g).takeDamage();
        }
//      else if(g.getType() == GameObject.ObjectType.HYDRA) {
//            // stun hydra
//            SfxController.getInstance().playSFX("spear_enemy_hit");
//            SfxController.getInstance().playSFX("shark_hit");
//            ((Hydra) g).setHit(true);}
        else if (g.getType() == GameObject.ObjectType.SIREN){
            if(((Siren) g).setHit()) {
                s.setDestroyed(true);
                SfxController.getInstance().playSFX("spear_enemy_hit");
            }
        }
        else if (g.getType() == GameObject.ObjectType.ROCK || g.getType() == GameObject.ObjectType.OBSTACLE) {
            SfxController.getInstance().playSFX("spear_break");
            s.setDestroyed(true);
        }
        // destroy bullet

    }

    /**
     * Resolve collisions between the raft and any other object.
     * @param r the player
     * @param g wood, enemies, treasure, or projectiles
     */
    private void ResolveRaftCollision(Raft r, GameObject g){
        if(g.isDestroyed()) {return;}
        if(g.getType() == GameObject.ObjectType.WOOD){
            // update player health
            r.addHealth(((Wood) g).getWood());
            SfxController.getInstance().playSFX("wood_pickup");
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.SHARK || g.getType() == GameObject.ObjectType.SIREN ){
            // update player health
            r.addHealth(Shark.ENEMY_DAMAGE);
            SfxController.getInstance().playSFX("raft_damage");
            g.setDestroyed(true);
            r.setDamaged(true);
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                    r.setDamaged(false);
                }
            }, .1f, 1, 1);
        } else if(g.getType() == GameObject.ObjectType.TREASURE){
            // add random wood and update player score
            addScore();
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.GOAL){
            // Check player win
            if (!complete && !failed) setComplete(true);
        } else if(g.getType() == GameObject.ObjectType.ROCK){
            if(((Rock) g).isSharp()) r.addHealth(Rock.getDAMAGE());
        } else if(g.getType() == GameObject.ObjectType.NOTE){
            r.setProjectileForce(((Note) g).getForce());
            r.addHealth(Note.DAMAGE);
            g.setDestroyed(true);
        }
    }

    private void addScore(){
        levelModel.addRandomWood();
        playerScore++;
        if(playerScore > 3) { playerScore = 3; System.out.println("incorrect 4th treasure detected."); }
        SfxController.getInstance().playSFX("chest_collect");
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
            controls[i] = new SharkController(i, enemies.get(i), levelModel.getPlayer(), levelModel);
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
        // check if load the same level, if not, reset lerp vector
        boolean same_level = level_int == level_id;
        level_id = level_int;
        JsonValue level_data = directory.getEntry("level:" + level_int, JsonValue.class);
        System.out.println("Loaded level "+level_int);
        emptyLevel();
        levelModel.loadLevel(level_int, level_data);
        prepareEnemy();
        stage.clear();
        table.clear();
        playerScore = 0;
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        transitionBuilt = false;
        pausePressed = false;
        pauseBuilt = false;
        settingsPressed = false;
        wasComplete = false;
        wasMoving = false;
        wasCharging = false;

        // Reset Soundcontroller
        SfxController.getInstance().setMusicPreset(level_data.getInt("music_preset", 1));
        SfxController.getInstance().startLevelMusic();

        // Reset hint displays
        resetControlHints();

        // Reset player position history
        for(int i = 0; i < raftSampleSpeeds.length; i++) {
            // each added 1 for offset due to extra grid added in the water map
            raftSamplePositionsXY[2*i] = levelModel.getPlayer().getPosition().x * (1.0f/levelModel.getTileSize()) + 1;
            raftSamplePositionsXY[2*i+1] = levelModel.getPlayer().getPosition().y * (1.0f/levelModel.getTileSize()) + 1;
            raftSampleSpeeds[i] = 0.0f;
            raftSampleLastTime = (System.currentTimeMillis() - startTime)/1000.0f;

            canvas.setRaftSamples(raftSamplePositionsXY, raftSampleSpeeds);
            canvas.setRaftSampleTime(0.0f);
        }

        // update shader color palette
        String pref_palette = level_int < 6 ? "colors_light" :
                              level_int < 10 ? "colors_natural" :
                              "colors_purple";
        String[] shaderColorStrings = shaderData.get(pref_palette).asStringArray();
        float[] shaderColors = new float[3*shaderColorStrings.length];
        for (int i = 0; i < shaderColorStrings.length; i ++) {
            Color c = Color.valueOf(shaderColorStrings[i]+"00");
            shaderColors[3*i+0] = c.r;
            shaderColors[3*i+1] = c.g;
            shaderColors[3*i+2] = c.b;
        }
        canvas.setShaderColors(shaderColors);

        // reset lerp if level changed
        if(!same_level) { levelModel.resetLerp(); }
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        SfxController.getInstance().haltMusic();
        setLevel(level_id);
    }

}
