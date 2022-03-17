package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.gdiac.util.PooledList;

import java.util.Iterator;

public class WorldController implements Screen, ContactListener {

    // CONSTANTS
    /** Exit code for quitting the game */
    public static final int EXIT_QUIT = 0;
    /** Exit code for advancing to next level */
    public static final int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 120;

    /** The amount of time for a physics engine step. */
    public static final float WORLD_STEP = 1/60.0f;
    /** Number of velocity iterations for the constrain solvers */
    public static final int WORLD_VELOC = 6;
    /** Number of position iterations for the constrain solvers */
    public static final int WORLD_POSIT = 2;

    // FIELDS
    // CANVAS AND OBJECT LIST
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    //TEXTURE
    /** The texture for walls and platforms */
    protected TextureRegion earthTile;
    /** The texture for the exit condition */
    protected TextureRegion goalTile;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;

    // WORLD
    protected LevelModel levelModel;

    // LEVEL STATUS
    /** Whether or not this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether the map mode is active */
    private boolean map;
    /** Whether the debug draw mode is active */
    private boolean debug;
    /** Countdown active for winning or losing */
    private int countdown;



    /**
     * Creates a new game world
     *
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2d coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected WorldController() {
        levelModel = new LevelModel();
        this.complete = false;
        this.failed = false;
        this.map  = false;
        this.debug = false;
        this.active = false;
        this.countdown = -1;
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
     * to be overriden if the world needs fancy backgrounds or the like.
     *
     * The method draws all objects in the order that they were added.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void draw(float dt) {
//        canvas.clear();

        canvas.begin();
        for(GameObject obj : levelModel.getObjects()) {
            obj.draw(canvas);
        }
        canvas.end();

        if (debug) {
//            canvas.beginDebug();
            for(GameObject obj : levelModel.getObjects()) {
//                obj.drawDebug(canvas);
            }
//            canvas.endDebug();
        }

        if (map) {
//            canvas.beginMap();
            for(GameObject obj : levelModel.getObjects()) {
//                obj.drawMap(canvas);
            }
//            canvas.endMap();
        }

        // Final message
        if (complete && !failed) {
            displayFont.setColor(Color.YELLOW);
            canvas.begin(); // DO NOT SCALE
            canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
            canvas.end();
        } else if (failed) {
            displayFont.setColor(Color.RED);
            canvas.begin(); // DO NOT SCALE
            canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
            canvas.end();
        }
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
        earthTile = new TextureRegion(directory.getEntry( "shared:earth", Texture.class ));
        goalTile  = new TextureRegion(directory.getEntry( "shared:goal", Texture.class ));
        displayFont = directory.getEntry( "shared:retro" ,BitmapFont.class);
        levelModel.setDirectory(directory);
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
        if (listener == null) {
            return true;
        }

//      if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap()) { map = !map; } // Toggle map

        // Handle resets
        if (input.didReset()) {
            reset();
        }

        // Now it is time to maybe switch screens.
        if (input.didExit()) {
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        } else if (input.didNext()) {
            pause();
            listener.exitScreen(this, EXIT_NEXT);
            return false;
        } else if (input.didPrevious()) {
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

    /**
     * Add a new bullet to the world and send it in the right direction.
     */
    private void createBullet() {
        // Compute position and velocity
        GameObject bullet = new Bullet(levelModel.getPlayer().position);
//        bullet.velocity = (levelModel.getPlayer().getFacing());
        levelModel.addQueuedObject(bullet);

//        bullet.setTexture(bulletTexture);
//        float radius = bulletTexture.getRegionWidth()/(2.0f*scale.x);
//        bullet.setBullet(true);
//        fireId = playSound( fireSound, fireId );
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
//        levelModel.getPlayer().setMovementX(InputController.getInstance().getMovement().x * levelModel.getPlayer().getForce());
//        levelModel.getPlayer().setMovementY(InputController.getInstance().getMovement().y * levelModel.getPlayer().getForce());
//        levelModel.getPlayer().setShooting(InputController.getInstance().didFire());

        // Add a bullet if we fire
//        if (levelModel.getPlayer().isShooting()) {
            createBullet();
//        }

//        levelModel.getPlayer().applyForce();
        resolveSounds();

        // update enemy
        resolveEnemies();
    }

    /***/
    private void resolveEnemies() {
        for (GameObject enemy:levelModel.getEnemies()) { // TODO: type should be enemy
//            enemy.resolveAction(levelModel.getPlayer());
        }
    }

    /***/
    private void resolveSounds() {
//        if(true){
//            soundController.updateMusic();
//        }
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
        levelModel.world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<GameObject>.Entry> iterator = levelModel.getObjects().entryIterator();
        while (iterator.hasNext()) {
            PooledList<GameObject>.Entry entry = iterator.next();
            GameObject obj = entry.getValue();
            if (obj.isRemoved()) {
                obj.deactivatePhysics(levelModel.world);
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);
            }
        }
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

    /*=*=*=*=*=*=*=*=*=* Physics *=*=*=*=*=*=*=*=*=*/
    /**
     * Remove a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(GameObject bullet) {
        bullet.markRemoved(true);
//        plopId = playSound( plopSound, plopId );
    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            GameObject bd1 = (GameObject)body1.getUserData();
            GameObject bd2 = (GameObject)body2.getUserData();

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != levelModel.getPlayer()) {
                removeBullet(bd1);
            }

            if (bd2.getName().equals("bullet") && bd1 != levelModel.getPlayer()) {
                removeBullet(bd2);
            }

//            // See if we have landed on the ground.
//            if ((levelModel.getPlayer().getSensorName().equals(fd2) && levelModel.getPlayer() != bd1) ||
//                    (levelModel.getPlayer().getSensorName().equals(fd1) && levelModel.getPlayer() != bd2)) {
//                levelModel.getPlayer().setGrounded(true);
//                sensorFixtures.add(levelModel.getPlayer() == bd1 ? fix2 : fix1); // Could have more than one ground
//            }

            // Check for win condition
            if ((bd1 == levelModel.getPlayer() && bd2 == levelModel.getGoal()) ||
                    (bd1 == levelModel.getGoal() && bd2 == levelModel.getPlayer())) {
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    @Override
    public void endContact(Contact contact) {
//        Fixture fix1 = contact.getFixtureA();
//        Fixture fix2 = contact.getFixtureB();
//
//        Body body1 = fix1.getBody();
//        Body body2 = fix2.getBody();
//
//        Object fd1 = fix1.getUserData();
//        Object fd2 = fix2.getUserData();
//
//        Object bd1 = body1.getUserData();
//        Object bd2 = body2.getUserData();
//
//        if ((levelModel.getPlayer().getSensorName().equals(fd2) && levelModel.getPlayer() != bd1) ||
//                (levelModel.getPlayer().getSensorName().equals(fd1) && levelModel.getPlayer() != bd2)) {
//            sensorFixtures.remove(levelModel.getPlayer() == bd1 ? fix2 : fix1);
////            if (sensorFixtures.size == 0) {
////                levelModel.getPlayer().setGrounded(false);
////            }
//        }
    }

    /** Unused ContactListener method */
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    /** Unused ContactListener method */
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

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
    public boolean isFailure( ) {
        return failed;
    }

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

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        levelModel.reset();
        levelModel.world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        levelModel.loadLevel(LevelModel.LEVEL_RESTART_CODE);
    }

    /**
     * Populate the level according to the new level selection.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void setLevel(int level_int){
        levelModel.reset();
        levelModel.world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        levelModel.loadLevel(level_int);

    }

}
