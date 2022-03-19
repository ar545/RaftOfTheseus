package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
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
    /** Number of velocity iterations for the constraint solvers */
    public static final int WORLD_VELOCITY = 6;
    /** Number of position iterations for the constraint solvers */
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
    /** Whether this is an active controller */
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
    SoundController soundController;
    /** array of controls for each enemy**/
    private AIController[] controls;


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
        PooledList<Enemy> enemies = levelModel.getEnemies();
        controls = new AIController[enemies.size()];
        for (int i = 1; i < enemies.size(); i++) {
            controls[i] = new AIController(i, enemies.get(i), levelModel.getPlayer());
        }

        soundController = new SoundController(true);
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
        // return if no canvas pointer
        if(canvas == null){
            return;
        }

        float pixelsPerUnit = 100.0f/3.0f; // Tiles are 100 pixels wide

        // "Moving Camera" calculate offset = (ship pos) - (canvas size / 2)
        Vector2 offset2 = new Vector2((float)canvas.getWidth()/2, (float)canvas.getHeight()/2);
        offset2.sub(levelModel.getPlayer().getPosition().scl(pixelsPerUnit));
        Affine2 cameraTransform = new Affine2();
        cameraTransform.setToTranslation(offset2);

        canvas.clear();
        canvas.begin(cameraTransform);
        for(GameObject obj : levelModel.getObjects()) {
//            obj.drawAffine(canvas, offset2, pixelsPerUnit);
            obj.draw(canvas);
        }
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            for(GameObject obj : levelModel.getObjects()) {
                obj.drawDebug(canvas);
            }
            canvas.endDebug();
        }

        if (map) {

            canvas.begin();
            for(GameObject obj : levelModel.getObjects()) {
                obj.drawMap(canvas);
            }
            canvas.end();
        }

        // Final message
//        if (complete && !failed) {
//            displayFont.setColor(Color.YELLOW);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
//            canvas.end();
//        } else if (failed) {
//            displayFont.setColor(Color.RED);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
//            canvas.end();
//        }
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
//        earthTile = new TextureRegion(directory.getEntry( "shared:earth", Texture.class ));
//        goalTile  = new TextureRegion(directory.getEntry( "shared:goal", Texture.class ));
//        displayFont = directory.getEntry( "shared:retro" ,BitmapFont.class);
        levelModel.setDirectory(directory);
        levelModel.gatherAssets(directory);
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
        GameObject bullet = new Bullet(levelModel.getPlayer().getPosition());
        bullet.setLinearVelocity(levelModel.getPlayer().getFacing());
        levelModel.addQueuedObject(bullet);

        // TODO: set the texture for bullet? should be part of the populate texture function
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
        InputController ic = InputController.getInstance();
        Raft player = levelModel.getPlayer();
        player.setMovementX(ic.getMovement().x);
        player.setMovementY(ic.getMovement().y);
        player.setFire(ic.didFire());
//        System.out.println(player.getPosition().x + "/" + player.getPosition().y);

        // Add a bullet if we fire
        if (player.isFire()) {
            createBullet();
        }

        // update enemy
        resolveEnemies();
        player.applyForce();
        resolveMusic();
    }

    /** get enemies take actions according to their AI */
    private void resolveEnemies() {
        PooledList<Enemy> el = levelModel.getEnemies();
        for (int i = 0; i< el.size(); i++) {
            Enemy enemy = el.get(i);
            //TODO
            // this line is commented out because it was crashing. The list controls[] was a different size from the list getEnemies().
//            enemy.resolveAction(controls[i].getAction(), levelModel.getPlayer(), controls[i].getTicks());
        }
    }

    // TODO: When to switch music (not sound effects)?
    /** Update the level themed music according the game status */
    private void resolveMusic() {
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
        levelModel.world.step(WORLD_STEP, WORLD_VELOCITY,WORLD_POSIT);

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<GameObject>.Entry> iterator = levelModel.getObjects().entryIterator();
        while (iterator.hasNext()) {
            PooledList<GameObject>.Entry entry = iterator.next();
            GameObject obj = entry.getValue();
            if (obj.isDestroyed()) {
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

        try {
            GameObject bd1 = (GameObject)body1.getUserData();
            GameObject bd2 = (GameObject)body2.getUserData();
//            // Check for rock collision with any entity
//            if(bd1.getType().equals(GameObject.ObjectType.OBSTACLE)){
//                revertRecentMovement(bd2);
//            }else if(bd2.getType().equals(GameObject.ObjectType.OBSTACLE)){
//                revertRecentMovement(bd1);
//            }
            // Check for non-rock entity interaction with current
            if(bd1.getType().equals(GameObject.ObjectType.CURRENT)){
                pushEntity((Current) bd1, bd2);
            } else if(bd2.getType().equals(GameObject.ObjectType.CURRENT)){
                pushEntity((Current) bd2, bd1);
            }
            // Check for bullet collision with enemy (projectiles kill enemies)
            if (bd1.getType().equals(GameObject.ObjectType.BULLET) && bd2.getType().equals(GameObject.ObjectType.ENEMY)) {
                ResolveCollision((Bullet) bd1, (Enemy) bd2);
            }else if (bd2.getType().equals(GameObject.ObjectType.BULLET) && bd1.getType().equals(GameObject.ObjectType.ENEMY)) {
                ResolveCollision((Bullet) bd2, (Enemy) bd1);
            }
            // Check for player collision with wood (health+)
            else if(bd1.getType().equals(GameObject.ObjectType.RAFT) && bd2.getType().equals(GameObject.ObjectType.WOOD)){
                ResolveCollision((Raft)bd1, (Wood)bd2);
            } else if(bd1.getType().equals(GameObject.ObjectType.WOOD) && bd2.getType().equals(GameObject.ObjectType.RAFT)){
                ResolveCollision((Raft)bd2, (Wood)bd1);
            }
            // Check for player kill enemies (health-)
            else if(bd1.getType().equals(GameObject.ObjectType.RAFT) && bd2.getType().equals(GameObject.ObjectType.ENEMY)){
                ResolveCollision((Raft)bd1, (Enemy) bd2);
            }else if(bd1.getType().equals(GameObject.ObjectType.ENEMY) && bd2.getType().equals(GameObject.ObjectType.RAFT)){
                ResolveCollision((Raft)bd2, (Enemy) bd1);
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
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: Should we push current before we step the physics engine?
    /** Push o according to c
     * @param c the current
     * @param o the object to push
     * Precondition: object o is guaranteed not to be rock/obstacle */
    private void pushEntity(Current c, GameObject o) {
//        o.setPosition(c.getDirectionVector().add(o.getPosition()));
        o.getBody().applyLinearImpulse(c.getDirectionVector(), o.getPosition(), true);
    }

//    /** Place the object back to its position cache to revert its most recent movement
//     * @param o the game object to revert */
//    private void revertRecentMovement(GameObject o) { o.setPosition(o.getPositionCache()); }

    /** Resolve collision between two objects of specific types
     * @param b bullet
     * @param e enemy */
    private void ResolveCollision(Bullet b, Enemy e) {
        // destroy bullet
        removeBullet(b);
        // destroy enemy
        e.setDestroyed(true);
    }

    /** Resolve collision between two objects of specific types
     * @param r raft
     * @param e enemy */
    private void ResolveCollision(Raft r, Enemy e) {
        // update player health
        r.addHealth(Enemy.ENEMY_DAMAGE);
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
    }

    // TODO: When current not in touch with objects, do we need to stop acting force on it?
    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the character is NOT on the ground.  This is how we prevent
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
//            if (sensorFixtures.size == 0) {
//                levelModel.getPlayer().setGrounded(false);
//            }
//        }
    }

    /** Unused ContactListener method. May be used to play sound effects */
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        //TODO: below are imported from Lab 4. Question: What sound effects do we want?
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
        if (speed > bumpThresh) {
            GameObject.ObjectType s1 = ((GameObject)body1.getUserData()).getType();
            GameObject.ObjectType s2 = ((GameObject)body2.getUserData()).getType();
            if (s1 == GameObject.ObjectType.RAFT && s2 == GameObject.ObjectType.ENEMY) {
                playSoundEffect();
            }
            if (s1 == GameObject.ObjectType.ENEMY && s2 == GameObject.ObjectType.RAFT) {
                playSoundEffect();
            }
        }
    }

    // TODO: What sound effect are needed and when do we want them?
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

    public void setScreenListener(GDXRoot gdxRoot) {
    }
}
