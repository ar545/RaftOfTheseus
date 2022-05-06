package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.model.*;
import edu.cornell.gdiac.raftoftheseus.model.enemy.*;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Note;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Spear;
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
        Raft.setConstants(objParams.get("raft"));
        Spear.setConstants(objParams.get("spear"));
        Note.setConstants(objParams.get("note"));
        Shark.setConstants(objParams.get("shark"));
        Hydra.setConstants(objParams.get("hydra"));
        Siren.setConstants(objParams.get("siren"));
        Stationary.setConstants(objParams.get("stationary"));
        Shipwreck.setConstants(objParams.get("shipwreck"));
        Current.setConstants(objParams.get("current"));
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
    /** Control settings information for tutorial levels */
    private static JsonValue controlSettings;

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
    /** The texture for the empty star */
    protected TextureRegion emptyStar;
    /** The texture for the filled star */
    protected TextureRegion filledStar;
    /** The hint image for wasd movement */
    protected TextureRegion wasdIcon;
    /** The hint image for arrows movement */
    protected TextureRegion arrowsIcon;
    /** The hint image for firing controls */
    protected TextureRegion hintAttack;
    /** The hint background for controls */
    protected TextureRegion hintKey;
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

    /** Sets the control settings information  */
    public static void setKeyParams(JsonValue keyParams){
        controlSettings = keyParams;
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
        levelModel.getPlayer().updateSpear(dt, firePixel);
        levelModel.getPlayer().setAnimationFrame(dt);
        for(Spear s : levelModel.getSpears()){
            s.setAnimationFrame(dt);
        }
        for(Siren s : levelModel.getSirens()){
            s.setAnimationFrame(dt);
        }
        for(Shark s : levelModel.getSharks()){
            s.setAnimationFrame(dt);
        }
        for(Treasure s: levelModel.getTreasure()){
            s.setAnimationFrame(dt);
        }

        // Update raft samples (for displaying the wake in the shader) before drawing water
        updateRaftWakeSamples();

        // Draw the level
        levelModel.draw((System.currentTimeMillis() - startTime) / 1000.0f, level_id < 5);

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
//            SfxController.getInstance().fadeMusic();
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
        if(hintTimer >= fadeTimeEnd) {
            blockHint = true;
            return;
        }
        Color c;
        if(hintTimer >= fadeTimeStart){
            c = new Color(1f, 1f, 1f, (fadeTimeEnd - hintTimer)/fadeTimeSpan);
        } else {
            c = Color.WHITE;
        }
        BitmapFont font = skin.getFont("default-font");
        font.setColor(c);
        canvas.begin();
        switch(level_id) {
            case(0):
                // TOP SECTION
                canvas.draw(
                        wasdIcon, c,
                        wasdIcon.getRegionWidth()*0.5f, 0.0f,
                        canvas.getWidth()*0.5f - wasdIcon.getRegionWidth() + 50, canvas.getHeight()*0.5f + 120,
                        wasdIcon.getRegionWidth(), wasdIcon.getRegionHeight()
                );
                font.getData().setScale(0.3f);
                canvas.drawText(
                        "OR", font,
                        canvas.getWidth()*0.5f - 20, canvas.getHeight()*0.5f + 120 + wasdIcon.getRegionHeight() / 2
                );
                canvas.draw(
                        arrowsIcon, c,
                        arrowsIcon.getRegionWidth()*0.5f, 0.0f,
                        canvas.getWidth()*0.5f + arrowsIcon.getRegionWidth() - 50, canvas.getHeight()*0.5f + 120,
                        arrowsIcon.getRegionWidth(), arrowsIcon.getRegionHeight()
                );
                font.getData().setScale(0.5f);
                // BOTTOM SECTION LEFT
                canvas.drawText(
                        "RESTART", font,
                        canvas.getWidth()*0.5f - wasdIcon.getRegionWidth() - 70 , canvas.getHeight()*0.5f - 70 + wasdIcon.getRegionHeight()
                );
                canvas.draw(
                        hintKey, c,
                        hintKey.getRegionWidth()*0.5f, hintKey.getRegionWidth() *0.5f,
                        canvas.getHeight()*0.5f + wasdIcon.getRegionHeight() + 150, canvas.getHeight()*0.5f - 90 + wasdIcon.getRegionHeight(),
                        hintKey.getRegionWidth() * 0.7f, hintKey.getRegionHeight() * 0.85f
                );
                canvas.drawText(
                        controlSettings.get("mouse keyboard").get("reset").asString(), font,
                        canvas.getHeight()*0.5f + wasdIcon.getRegionHeight() + 125, canvas.getHeight()*0.5f - 70 + wasdIcon.getRegionHeight()
                );
//                // BOTTOM SECTION RIGHT
                canvas.drawText(
                        "PAUSE", font,
                        canvas.getWidth()*0.5f - wasdIcon.getRegionWidth() + 280 , canvas.getHeight()*0.5f - 70 + wasdIcon.getRegionHeight()
                );
                canvas.draw(
                        hintKey, c,
                        hintKey.getRegionWidth()*0.5f, hintKey.getRegionWidth() *0.5f,
                        canvas.getWidth()*0.5f - wasdIcon.getRegionWidth() + 500, canvas.getHeight()*0.5f - 90 + wasdIcon.getRegionHeight(),
                        hintKey.getRegionWidth() * 0.7f, hintKey.getRegionHeight() * 0.85f
                );
                canvas.drawText(
                        controlSettings.get("mouse keyboard").get("pause").asString(), font,
                        canvas.getWidth()*0.5f - wasdIcon.getRegionWidth() + 470, canvas.getHeight()*0.5f - 70 + wasdIcon.getRegionHeight()
                );
                break;
            case(2):
                font.getData().setScale(0.5f);
                canvas.drawText(
                        "MAP", font,
                        canvas.getWidth()*0.5f - 140, canvas.getHeight()*0.5f + 100 + hintKey.getRegionHeight()
                );
                canvas.draw(
                        hintKey, c,
                        hintKey.getRegionWidth()*0.5f, 0.0f,
                        canvas.getWidth()*0.5f + 50, canvas.getHeight()*0.5f + 120,
                        hintKey.getRegionWidth(), hintKey.getRegionHeight()
                );
                canvas.drawText(
                        controlSettings.get("mouse keyboard").get("map").asString(), font,
                        canvas.getWidth()*0.5f + 35, canvas.getHeight()*0.5f + 100 + hintKey.getRegionHeight()
                );
                break;
            case(4):
                canvas.draw(hintAttack, c, hintAttack.getRegionWidth()*0.5f, 0.0f, canvas.getWidth()*0.5f, canvas.getHeight()*0.5f + 120, hintAttack.getRegionWidth(), hintAttack.getRegionHeight());
                break;
            default:
                // draw nothing
        }
        canvas.end();
    }

    private void saveGame() {
        saveData.get("level_data").get(level_id).get("complete").set(true);
        // if the treasure available in the level is 0, the player gets 3 stars for completing
        int score = levelModel.getTreasureCount() > 0 ? playerScore : 3;
        saveData.get("level_data").get(level_id).get("score").set(score, Integer.toString(score));
        if (level_id + 1 < NUM_LEVELS) {
            saveData.get("level_data").get(level_id + 1).get("unlocked").set(true);
        }
        FileHandle file = Gdx.files.local("save_data.json");
        String jsonString = saveData.prettyPrint(JsonWriter.OutputType.json, 1);
        file.writeString(jsonString, false);
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

    private void drawPause() {
        if (!pauseBuilt) {
            pauseBuilt = true;
            drawTransparentOverlay();
            table.setFillParent(true);
            table.align(Align.center);
            stage.addActor(table);
            skin.add("pause_background", pauseBackground);
            table.setBackground(skin.getDrawable("pause_background"));

            TextButton resumeButton =  UICreator.createTextButton("RESUME", skin, 0.35f);
            resumeButton.addListener(UICreator.createListener(resumeButton, Color.LIGHT_GRAY, Color.WHITE, this::resetPausePressed));
            table.add(resumeButton).padTop(-20);
            table.row();

            TextButton restartButton = UICreator.createTextButton("RESTART", skin, 0.35f);
            restartButton.addListener(UICreator.createListener(restartButton, Color.LIGHT_GRAY, Color.WHITE, this::reset));
            table.add(restartButton);
            table.row();

            TextButton settingsButton = UICreator.createTextButton("SETTINGS", skin, 0.35f);
            table.add(settingsButton);
            settingsButton.addListener(UICreator.createListener(settingsButton, Color.LIGHT_GRAY, Color.WHITE, this::setSettingsPressed));
            table.row();

            TextButton exitButton = new TextButton("EXIT", skin);
            exitButton.getLabel().setFontScale(0.35f);
            exitButton.getLabel().setColor(Color.GOLD);
            exitButton.addListener(UICreator.createListener(exitButton, Color.LIGHT_GRAY, Color.GOLD, this::setExitPressed));
            table.add(exitButton);
        }
        stage.act();
        stage.draw();
    }

    /** Whether to build or draw the transition screen. */
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

    /** Code to construct the transition screen. */
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
        TextButton mainButton = UICreator.createTextButton(didFail ? "RESTART" : "NEXT", skin, 0.4f);
        mainButton.addListener(UICreator.createListener(mainButton, this::goNext, didFail));
        part1.add(mainButton).expandX().align(Align.center).padTop(10);
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.row().colspan(didFail ? 2 : 3);
        if (!didFail) {
            TextButton replayButton = UICreator.createTextButton("REPLAY", skin, 0.4f);
            replayButton.addListener(UICreator.createListener(replayButton, Color.LIGHT_GRAY, Color.WHITE, this::reset));
            part2.add(replayButton).expandX().padRight(70);
        }

        TextButton settingsButton = UICreator.createTextButton("SETTINGS", skin, 0.4f, Color.GOLD);
        settingsButton.addListener(UICreator.createListener(settingsButton, Color.LIGHT_GRAY, Color.WHITE, this::setSettingsPressed));
        part2.add(settingsButton).expandX();

        TextButton exitButton = UICreator.createTextButton("EXIT", skin, 0.4f, Color.GOLD);
        exitButton.addListener(UICreator.createListener(exitButton, Color.GRAY, Color.GOLD, this::setExitPressed));
        part2.add(exitButton).expandX();
        table.add(part2);
        table.row();
    }

    /** @param didFail whether the player has failed ofr not. */
    private void goNext(boolean didFail) {
        if (didFail) {
            reset();
        } else {
            nextPressed = true;
        }
    }

    /** Set settingsPressed to be true for transitioning. */
    private void setSettingsPressed(){ settingsPressed = true; }
    /** Set settingsPressed to be true for transitioning. */
    private void setPausePressed(){ pausePressed = true; }
    /** Set settingsPressed to be true for transitioning. */
    private void setExitPressed(){ exitPressed = true; }
    /** Helper method to pass for button creation and concealing pause resetting. */
    private void resetPausePressed(){ pausePressed = false; }

    /** Draw star at the up left corner
     * Precondition: the game canvas has not begun; Post-condition: the game canvas will end after this function */
    private void drawStar(int star) {
        canvas.begin();
        int size = 70;
        int padding = 10;
        canvas.draw(star > 0 ? filledStar : emptyStar, padding, canvas.getHeight() - size);
        canvas.draw(star > 1 ? filledStar : emptyStar, size + padding, canvas.getHeight() - size);
        canvas.draw(star > 2 ? filledStar : emptyStar, 2 * size + padding, canvas.getHeight() - size);
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
        emptyStar = new TextureRegion(directory.getEntry("empty_star", Texture.class));
        filledStar = new TextureRegion(directory.getEntry("filled_star", Texture.class));
        pauseBackground = directory.getEntry("pause_background", Texture.class);
        failedBackground = directory.getEntry("failed_background", Texture.class);
        successBackgrounds = new Texture[4];
        for (int i = 0; i < 4; i++) {
            successBackgrounds[i] = directory.getEntry("success_background_" + i, Texture.class);
        }
        hintAttack = new TextureRegion(directory.getEntry( "hint_attack", Texture.class ));
        wasdIcon = new TextureRegion(directory.getEntry( "hint_wasd", Texture.class ));
        arrowsIcon = new TextureRegion(directory.getEntry("hint_arrows", Texture.class));
        hintKey = new TextureRegion(directory.getEntry( "black_key_background", Texture.class ));

        // shader things
        Texture waterDiffuse = directory.getEntry("water_diffuse", Texture.class);
        waterDiffuse.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture waterNormal = directory.getEntry("water_normal", Texture.class);
        waterNormal.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture waterUVOffset = directory.getEntry("water_uv_offset", Texture.class);
        waterUVOffset.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture floatingItemMask = directory.getEntry("floating_item_mask", Texture.class);
        floatingItemMask.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        canvas.setWaterTextures(waterDiffuse, waterNormal, waterUVOffset, floatingItemMask);

        // level model assets
        levelModel.setDirectory(directory);
        levelModel.gatherAssets();
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
        // Update camera to prevent null pointer exceptions
        levelModel.updateCameraTransform();
        levelModel.updateAllCurrentEffects(dt);
        levelModel.updateLights();

        // Read the player input
        InputController input = InputController.getInstance();
        input.readInput();
        if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap() && !complete && !failed) {
            // Toggle map
            map = !map;
            SfxController.getInstance().playSFX("map_open");
        }

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
        } else if ((input.didPause() || pausePressed) && (!complete && !failed))  {
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

    // Boolean to keep track of charging for sound effects
    private boolean wasCharging = false;
    // Vector2 to store mouse position in pixel coordinates.
    private Vector2 firePixel;

    /** The core gameplay loop of this world. This method is called after input is read, but before collisions
     * are resolved. The very last thing that it should do is apply forces to the appropriate objects.
     * @param dt	Number of seconds since last animation frame */
    public void update(float dt){
        // Process actions in object model
        InputController ic = InputController.getInstance();
        firePixel = ic.getMouseLocation();
        levelModel.getCameraTransform().inv().applyTo(firePixel);
        Raft player = levelModel.getPlayer();
        player.setMovementInput(ic.getMovement());
        player.beginCharging(ic.didCharge());

        // Play sfx
        if(!wasCharging && player.isCharging()){
            wasCharging = true;
            SfxController.getInstance().playSFX("spear_charge");
        }

        if(!player.canFire() && ic.didRelease()) { player.reverseFire(); wasCharging = false; } // cancel fire if player release before time

        // Create spear when possible
        if(player.canFire() && !player.hasSpear()){ levelModel.createSpear(); }

        // Move spear move after firing.
        if (player.canFire() && ic.didRelease() && player.hasSpear()) {
            // find the nearest enemy to player
            player.resetCanFire();
            levelModel.fireSpear(firePixel);
            SfxController.getInstance().playSFX("spear_throw");
            wasCharging = false;
        }

        // update forces for enemies, players, objects
        player.applyInputForce(levelModel.playerOnCurrent(), levelModel.getPlayerCurrentVelocity());
        player.applyProjectileForce();
        resolveEnemies(dt);

        // update light choice
        if(ic.didChange()){ levelModel.change(debug); }
    }

    /** get enemies take actions according to their AI */
    private void resolveEnemies(float dt) {

//        for (Hydra h : levelModel.getHydras()) {
//            levelModel.world.rayCast(hydraSight, h.getPosition(), levelModel.getPlayer().getPosition());
//            h.setSee(hydraSight.getCanSee());
////            if(h.willAttack()) {createBullet(h.getPosition(), levelModel.getPlayer());}
//            h.update(dt);
//        }

        for(Shark s : levelModel.getSharks()){
            updateLineOfSight(s);
            s.updateAI(dt);
        }
        for(Siren s : levelModel.getSirens()){
            s.updateAI(dt);
            if(s.willAttack()){
                levelModel.createNote(s.getPosition().cpy(), s.getTargetDirection(levelModel.getPlayerCurrentVelocity()));
            }
        }
    }

    private void updateLineOfSight(Shark s) {
        enemySight.reset();
        levelModel.world.rayCast(enemySight, s.getPosition(), levelModel.getPlayer().getPosition());
        s.canSee = enemySight.getCanSee();
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
        for(Shark s : levelModel.getSharks()){
            if(s.canHear()){
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
                resolveSpearCollision((Spear) bd1, bd2);
            } else if (bd2.getType() == GameObject.ObjectType.SPEAR) {
                resolveSpearCollision((Spear) bd2, bd1);
            }
            // Check for player collision
            else if(bd1.getType() == GameObject.ObjectType.RAFT){
                resolveRaftCollision((Raft) bd1, bd2);
            } else if(bd2.getType() == GameObject.ObjectType.RAFT){
                resolveRaftCollision((Raft) bd2, bd1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Resolve collision between the spear and any other object.
     * @param s spear
     * @param g terrain or enemy */
    private void resolveSpearCollision(Spear s, GameObject g) {
        if(g.getType() == GameObject.ObjectType.SHARK) {
            // stun shark
            if (((Shark)g).setHit()) {
                SfxController.getInstance().playSFX("spear_enemy_hit");
                SfxController.getInstance().playSFX("shark_hit");
                ((Shark) g).takeDamage();
            }
            s.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.HYDRA) {
            // stun hydra
            SfxController.getInstance().playSFX("spear_enemy_hit");
            SfxController.getInstance().playSFX("shark_hit");
            ((Hydra) g).setHit();
        } else if (g.getType() == GameObject.ObjectType.SIREN){
            if(((Siren) g).setHit()) {
                SfxController.getInstance().playSFX("spear_enemy_hit");
            }
            s.setDestroyed(true);
        } else if (g.getType() == GameObject.ObjectType.STATIONARY) {
            SfxController.getInstance().playSFX("spear_break");
            s.setDestroyed(true);
        } else if (g.getType() == GameObject.ObjectType.SHIPWRECK){
            s.setDestroyed(true);
            Shipwreck sw = ((Shipwreck) g);
            sw.takeDamage();
            if(sw.noHealth()){
                sw.setDestroyed(true);
                levelModel.addWood(sw.getPosition(), Shipwreck.getDrops());
            }
        }
        // destroy bullet

    }

    /**
     * Resolve collisions between the raft and any other object.
     * @param r the player
     * @param g wood, enemies, treasure, or projectiles
     */
    private void resolveRaftCollision(Raft r, GameObject g){
        if(g.isDestroyed()) {return;}
        if(g.getType() == GameObject.ObjectType.WOOD){
            // update player health
            r.addHealth(((Wood) g).getWood());
            SfxController.getInstance().playSFX("wood_pickup");
            g.setDestroyed(true);
        } else if(g.getType() == GameObject.ObjectType.SHARK || g.getType() == GameObject.ObjectType.SIREN ){
            if (g.getType() == GameObject.ObjectType.SHARK) {
                if (!((Shark)g).canHurtPlayer())
                    return; // ignore collisions with underwater shark
            }
            // update player health
            if(!r.isDamaged()) {
                r.addHealth(Shark.CONTACT_DAMAGE);
                SfxController.getInstance().playSFX("raft_damage");
//            g.setDestroyed(true);
                r.setDamaged(true);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        r.setDamaged(false);
                    }
                }, 2f, 1, 1);
            }
        } else if(g.getType() == GameObject.ObjectType.TREASURE){
            // add random wood and update player score
            addScore();
            r.halfLife();
            ((Treasure) g).setCollected(true);
        } else if(g.getType() == GameObject.ObjectType.GOAL){
            // Check player win
            if (!complete && !failed) setComplete(true);
        } else if(g.getType() == GameObject.ObjectType.STATIONARY){
            if(((Stationary) g).isSharp()) {
                if (!r.isDamaged()) {
                    r.addHealth(Stationary.getSharpRockDamage());
                    r.setDamaged(true);
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            r.setDamaged(false);
                        }
                    }, 2, 1, 1);
                }
            }
        } else if(g.getType() == GameObject.ObjectType.NOTE){
            r.setProjectileForce(((Note) g).getForce());
            r.addHealth(Note.DAMAGE);
            g.setDestroyed(true);
        }
    }

    private void addScore(){
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
        int diff = levelModel.getDifficulty();
        String pref_palette = diff == 0 ? "colors_light" :
                              diff == 1 ? "colors_natural" :
                              diff == 2 ? "colors_purple" :
                              "colors_natural";
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
