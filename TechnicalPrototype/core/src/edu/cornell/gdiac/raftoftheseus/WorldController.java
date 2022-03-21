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

    /** Scale for the health bar */
    private static final float HEALTH_BAR_SCALE = 0.6f;

    // FIELDS
    // CANVAS AND OBJECT LIST
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

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

        drawHealthBar(levelModel.getPlayer().getHealthRatio());
        drawStar(levelModel.getPlayer().getStar());

        if (map) {
            canvas.begin();
            float[] polygonVertices = levelModel.getWallVertices();
            Vector2 wallDrawscale = levelModel.getWallDrawscale();
            float x2 = polygonVertices[0], y2 = polygonVertices[1];
            canvas.draw(mapBackground, Color.GRAY, mapBackground.getWidth() / 2, mapBackground.getHeight() / 2,
                    (x2 / 4) * 0.5f * wallDrawscale.x + canvas.getWidth() / 4,
                    (y2 / 4) * 0.5f * wallDrawscale.y + canvas.getHeight() / 4 - y2, x2 * wallDrawscale.x * 0.5f, y2 * wallDrawscale.y * 0.5f - y2);
            for(GameObject obj : levelModel.getObjects()) {
                obj.drawMap(canvas);
            }
            canvas.end();
        }

        if (debug) {
            canvas.beginDebug();
            for(GameObject obj : levelModel.getObjects()) {
                obj.drawDebug(canvas);
            }
            canvas.endDebug();
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

        // force player remain with their current health
        float temporary_scalar = 0.5f; // player health circle scalar
        float r = levelModel.getPlayer().getHealth() * pixelsPerUnit * temporary_scalar;
        // TODO: this isn't the right expression, because acceleration != distance
        // TODO: However, this expression is meaningful in the way that it shows the thrust the player remain
        // TODO: The core decision we have to make here is that, do we want the player to cost health
        // TODO:  proportional to the distance of moving (as in gameplay) or proportional to the force they applied
        canvas.drawHealthCircle(r);
    }

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

    /** This function calculate the correct health bar color
     * @param median for red color the median should be 1/3 and 2/3 for green color
     * @param health the health percentage for the player
     * @return the rgb code representing the red or green color
     * old color function: Color c = new Color(Math.min(1, 2 - health * 2), Math.min(health * 2f, 1), 0, 1);*/
    private float makeColor(float median, float health){
        return Math.max(0, Math.min((1.5f - 3 * Math.abs(health - median)), 1));
    }

    /** Precondition: the game canvas has not begun; Post-condition: the game canvas will end after this function
     * @param health the health percentage for the player */
    private void drawHealthBar(float health) {
        Color c = new Color(makeColor((float)1/3, health), makeColor((float)2/3, health), 0, 1);
        TextureRegion RatioBar = new TextureRegion(colorBar, (int)(colorBar.getWidth() * health), colorBar.getHeight());
        float x_origin = (canvas.getWidth() - greyBar.getRegionWidth()*HEALTH_BAR_SCALE)  / (2f*HEALTH_BAR_SCALE);
        float y_origin = (canvas.getHeight() / (2f*HEALTH_BAR_SCALE));
        canvas.begin(HEALTH_BAR_SCALE, HEALTH_BAR_SCALE);
        canvas.draw(greyBar,Color.WHITE,x_origin, y_origin, greyBar.getRegionWidth(), greyBar.getRegionHeight());
        canvas.draw(RatioBar,c,x_origin, y_origin, RatioBar.getRegionWidth(), RatioBar.getRegionHeight());
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
        displayFont = directory.getEntry( "end" ,BitmapFont.class);
        mapBackground = directory.getEntry("map_background", Texture.class);
        bullet_texture = directory.getEntry( "bullet", Texture.class );
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

//      if (input.didDebug()) { debug = !debug; } // Toggle debug
        if (input.didMap()) { map = !map; } // Toggle map

        // Handle resets
        if (input.didReset()) {
            reset();
        }

        if (listener == null) {
            return true;
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
     * TODO: Bullets is now visible and fully functional. However, it has the remaining problem:
     *       - Bullets will be activated until the next update loop
     *       - Bullets are supposed to auto target an enemy, not go to the mouse position
     */
    private void createBullet(Enemy nearestEnemy) {
        // Compute position and velocity
        Vector2 facing = nearestEnemy.getPosition().sub(levelModel.getPlayer().getPosition()).nor();
        Bullet bullet = new Bullet(levelModel.getPlayer().getPosition().add(facing.scl(0.5f)));
        bullet.setTexture(bullet_texture);
//        bullet.setBullet(true); // this is unnecessary because our bullets travel fairly slowly
        bullet.setLinearVelocity(facing.scl(8));
        levelModel.addQueuedObject(bullet);
        levelModel.getPlayer().addHealth(Bullet.BULLET_HEALTH_COST);
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
        player.setMovement(ic.getMovement());
        player.setFire(ic.didFire());

        // Add a bullet if we fire
        if (player.isFire()) {
            // find nearest enemy to player
            Enemy nearestEnemy = null;
            float nearestD2 = -1;
            for (Enemy e : levelModel.getEnemies()) {
                if (!e.isDestroyed()) {
                    float d2 = e.getPosition().dst2(player.getPosition());
                    if (nearestD2 == -1 || d2 < nearestD2) {
                        nearestD2 = d2;
                        nearestEnemy = e;
                    }
                }
            }
            if (nearestEnemy != null) {
                createBullet(nearestEnemy);
            }
        }

        // update enemy, forces, music, player health
        resolveEnemies();
        player.applyForce();
        if(ic.Moved()){
            player.subtractHealth();
        }
        if(player.isDead() && !complete){
            setFailure(true);
        }
    }

    /** get enemies take actions according to their AI */
    private void resolveEnemies() {
        PooledList<Enemy> el = levelModel.getEnemies();
//        System.out.println("hi");

        for (int i = 0; i< el.size(); i++) {
            Enemy enemy = el.get(i);
//            System.out.println(i);
//            System.out.println(enemy);
//            System.out.println(Arrays.toString(controls));
//            System.out.println(controls.length);
//            System.out.println(controls[i]);
//            System.out.println(i);
            //TODO
            // this line is commented out because it was crashing. The list controls[] was a different size from the list getEnemies().
            enemy.resolveAction(controls[i].getAction(), levelModel.getPlayer(), controls[i].getTicks());
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
        for(AIController ai : controls){
            if(ai.getState() == Enemy.enemyState.CHASE){
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
            GameObject bd1 = (GameObject)body1.getUserData();
            GameObject bd2 = (GameObject)body2.getUserData();
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
        o.enterCurrent(c.getDirectionVector());
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

    /** Empty the level game objects */
    public void emptyLevel() {
        levelModel.reset();
        levelModel.world.setContactListener(this);
        setComplete(false);
        setFailure(false);
    }

    /** Prepare the AI for the enemy in the level */
    public void prepareEnemy(){
        PooledList<Enemy> enemies = levelModel.getEnemies();
        controls = new AIController[enemies.size()];
        for (int i = 0; i < enemies.size(); i++) {
            controls[i] = new AIController(i, enemies.get(i), levelModel.getPlayer());
        }
//        System.out.println(Arrays.toString(controls));
    }

    /**
     * Populate the level according to the new level selection.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void setLevel(int level_int){
        emptyLevel();
        levelModel.loadLevel(level_int);
        prepareEnemy();

        SoundController.getInstance().startLevelMusic();
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
       setLevel(LevelModel.LEVEL_RESTART_CODE);
    }

}
