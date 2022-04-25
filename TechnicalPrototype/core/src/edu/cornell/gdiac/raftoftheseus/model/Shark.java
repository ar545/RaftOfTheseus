package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;


public class Shark extends GameObject {
    Random rand = new Random();

    private class PathfindingTile implements Comparable<PathfindingTile> {
        /** x,y of this tile **/
        public int[] position;
        /** the optimal first coordinate to get to this tile **/
        public int[] moveDirection;
        /** cost of getting here in ticks **/
        public float cost;
        /** minimum depth search to find this tile **/
        public int depth;

        public PathfindingTile(int[] position, int[] moveDirection, float cost, int depth){
            this.position = position;
            this.moveDirection = moveDirection;
            this.cost = cost;
            this.depth = depth;
        }

        @Override
        public int compareTo(PathfindingTile o) {
            return Float.compare(cost, o.cost);
        }
    }


//    private Random rand = new Random();

    public Shark(Vector2 position, Raft targetRaft, LevelModel level) {
        physicsObject = new WheelObstacle(1.45f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_ENEMY;

        this.targetRaft = targetRaft;
        this.level = level;
    }

    public ObjectType getType() {
        return ObjectType.SHARK;
    }

    public static void setConstants(JsonValue objParams){
        ENEMY_WANDER_SPEED = objParams.getFloat("wander speed");
        ENEMY_CHASE_SPEED = objParams.getFloat("chase speed");
        ENEMY_DAMAGE = objParams.getFloat("damage");
        ENEMY_ENRAGE_CHASE_SPEED = objParams.getFloat("enrage speed", 8f);
        PROTECT_RANGE = objParams.getInt("protect range", 5);
        MAX_DEPTH = objParams.getInt("search range", 30);
    }

    /**
     * How much damage an enemy deals to the player upon collision, per animation frame
     */
    public static final float DAMAGE_PER_FRAME = 0.5f;
    /**
     * How fast enemy wanders around w/o target
     **/
    public static float ENEMY_WANDER_SPEED = 2.5f;
    /**
     * How fast the enemy moves towards its target, in units per second
     */
    public static float ENEMY_CHASE_SPEED = 4.0f;
    /**
     * How fast the enemy moves towards its target while enraged , in units per second
     */
    public static float ENEMY_ENRAGE_CHASE_SPEED = 8.0f;
    /**
     * How much health will enemy take from player upon collision
     */
    public static float ENEMY_DAMAGE;

    /** how far will the enemy go from a nearby treasure in tiles **/
    private static int PROTECT_RANGE = 5;

    /** max depth for djikstra pathfind **/
    public static int MAX_DEPTH = 30;

    private Vector2 moveVector = new Vector2();

    public static enum enemyState {
        /**
         * The enemy just spawned
         */
        SPAWN,
        /**
         * The enemy is patrolling around without a target
         */
        WANDER,
        /**
         * The enemy has a target, but must get closer
         */
        CHASE,
        /**
         * Like chase, but increased range and moves at greater speed
         */
        ENRAGE,
    }

    /**
     * This is the player, if this enemy is targeting the player.
     */
    private Raft targetRaft;

    private Treasure closestTreasure;

    /** if the enemy is enraged **/
    private boolean enraged;

    /** LevelModel used to convert screen coords **/
    private LevelModel level;

    private HashSet<String> visited;


    public Shark() {
        super();
    }

    public boolean isEnraged(){
        return enraged;
    }

    public void setEnraged(boolean b){
        this.enraged = b;
    }

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

//    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        super.update(dt);
        if (moveVector != null && targetRaft != null) {
            physicsObject.getBody().applyLinearImpulse(moveVector, getPosition(), true);
        }
    }

    private Vector2 vectorToBoardCoordinates(int row, int col){
        Vector2 dest = new Vector2(level.boardToScreen(row), level.boardToScreen(col));
        return dest.sub(getPosition()).nor();
    }

    /** get directional vector at a grid location
     * precondition: the grid space is either current or empty **/
    private Vector2 getDirectionalVector(int x, int y){
        if (level.obstacles()[x][y].getType() == ObjectType.CURRENT){
            return ((Current) level.obstacles()[x][y]).getDirectionVector();
        }
        // if it's empty there's no vector
        return new Vector2();
    }

    /** how much does it cost to move off of this tile in a direction onto the other tile?
     * precond: both tiles are either current or empty
     * precond: controlsignal is wander, chase, or enrage
     **/
    private float solveCost(int x, int y,  int[] dir, enemyState controlSignal){
        float speed = 0;
        if (controlSignal == enemyState.WANDER){
            speed = ENEMY_WANDER_SPEED;
        }
        else if (controlSignal == enemyState.CHASE){
            speed = ENEMY_CHASE_SPEED;
        }
        else if (controlSignal == enemyState.ENRAGE){
            speed = ENEMY_ENRAGE_CHASE_SPEED;
        }
        Vector2 movementVector = new Vector2(dir[0], dir[1]).scl(speed);
        Vector2 movementVector2 = new Vector2(dir[0], dir[1]).scl(speed);
        // solve time to move across each half of the tile
        // not totally accurate but good enough estimate if someone is better at lin alg please fix
        // take projection of added vectors onto intended movement vector
        Vector2 tile1Movement = new Vector2(dir[0], dir[1]).scl(movementVector.add(getDirectionalVector(x, y)).dot(new Vector2(dir[0], dir[1]).nor()));
        Vector2 tile2Movement = new Vector2(dir[0], dir[1]).scl(movementVector2.add(getDirectionalVector(x + dir[0], y + dir[1])).dot(new Vector2(dir[0], dir[1]).nor()));
        return tile1Movement.len() / (level.getTileSize() / 2) + tile2Movement.len() / (level.getTileSize() / 2);
    }

    private Vector2 pathFind(Raft targetRaft){
        return pathFind(level.screenToBoard(targetRaft.getX()), level.screenToBoard(targetRaft.getY()), false);
    }

    private Vector2 pathFind(Treasure closestTreasure) {
        return pathFind(level.screenToBoard(closestTreasure.getX()), level.screenToBoard(closestTreasure.getY()), true);

    }


//    private Vector2 pathFind(int goalRow, int goalCol, enemyState controlSignal){
//        int enemyRow = level.screenToBoard(getX());
//        int enemyCol = level.screenToBoard(getY());
//        int[] goal = new int[]{goalRow, goalCol};
//        int[] origin = new int[]{enemyRow, enemyCol};
//        PriorityQueue<PathfindingTile> pq = new PriorityQueue<>();
////        Queue<PathfindingTile> q = new Queue<>();
//
//        // add original location to pq
//        // for every item in pq pop it and see if it's the soln if so return original move direction else
//        // if depth not too deep add all of the legal surrounding directions and return next pop
//        pq.add(new PathfindingTile(origin, origin, 0, 0));
////        addIfLegal(q, new PathfindingTile(origin, origin, 0, 0));
//        while (pq.size() > 0){
////            PathfindingTile t = pq.poll();
//            // if this is the goal return
//            if (Arrays.equals(t.position, goal)){
//                return vectorToBoardCoordinates(t.position[0], t.position[1]);
//            }
//            // check if it's the origin square - fill the legal starting directions
//            if (Arrays.equals(t.moveDirection, origin)){
//                // right
//                addIfLegal(pq, new int[]{t.position[0] + 1, t.position[1]}, new int[]{1,0},
//                        solveCost(t.position[0], t.position[1], new int[]{1,0}, controlSignal), t.depth + 1);
//                // left
//                addIfLegal(pq, new int[]{t.position[0] - 1, t.position[1]}, new int[]{-1,0},
//                        solveCost(t.position[0], t.position[1], new int[]{-1,0}, controlSignal), t.depth + 1);
//                // up
//                addIfLegal(pq, new int[]{t.position[0], t.position[1] + 1}, new int[]{0,1},
//                        solveCost(t.position[0], t.position[1], new int[]{0,1}, controlSignal), t.depth + 1);
//                // down
//                addIfLegal(pq, new int[]{t.position[0], t.position[1] - 1}, new int[]{0,-1},
//                        solveCost(t.position[0], t.position[1], new int[]{0,-1}, controlSignal), t.depth + 1);
//            }
//            // add all legal moves
//            else if (t.depth < MAX_DEPTH){
//                // right
//                addIfLegal(pq, new int[]{t.position[0] + 1, t.position[1]}, t.moveDirection,
//                        t.cost + solveCost(t.position[0], t.position[1], new int[]{1,0}, controlSignal), t.depth + 1);
//                // left
//                addIfLegal(pq, new int[]{t.position[0] - 1, t.position[1]}, t.moveDirection,
//                        t.cost + solveCost(t.position[0], t.position[1], new int[]{-1,0}, controlSignal), t.depth + 1);
//                // up
//                addIfLegal(pq, new int[]{t.position[0], t.position[1] + 1}, t.moveDirection,
//                        t.cost + solveCost(t.position[0], t.position[1], new int[]{0,1}, controlSignal), t.depth + 1);
//                // down
//                addIfLegal(pq, new int[]{t.position[0], t.position[1] - 1}, t.moveDirection,
//                        t.cost + solveCost(t.position[0], t.position[1], new int[]{0,-1}, controlSignal), t.depth + 1);
//            }
//        }
//        // nothing to return so just stay still
//        return vectorToBoardCoordinates(enemyRow, enemyCol);
//    }

    private Vector2 pathFind(int goalRow, int goalCol, boolean isTreasure){
        int enemyRow = level.screenToBoard(getX());
        int enemyCol = level.screenToBoard(getY());
        int[] goal = new int[]{goalRow, goalCol};
        int[] origin = new int[]{enemyRow, enemyCol};
//        PriorityQueue<PathfindingTile> pq = new PriorityQueue<>();
        Queue<PathfindingTile> q = new Queue<>();
        visited = new HashSet<>();
        // add original location to pq
        // for every item in pq pop it and see if it's the soln if so return original move direction else
        // if depth not too deep add all of the legal surrounding directions and return next pop
//        pq.add(new PathfindingTile(origin, origin, 0, 0));
        addIfLegal(q, origin, origin, 0);
        while (q.size > 0){
            PathfindingTile t = q.removeFirst();
            GameObject go =level.obstacles()[t.position[0]][t.position[1]];
            if (closestTreasure == null && go != null && go.getType() == ObjectType.TREASURE){
                closestTreasure = (Treasure) go;
            }
            // if this is the goal return
            if (Arrays.equals(t.position, goal)){
                // if it's the treasure we can't be totally on top of it
                if (isTreasure){
                    return new Vector2();
                }
                return new Vector2(t.moveDirection[0], t.moveDirection[1]);
            }
            // check if it's the origin square - fill the legal starting directions
            if (Arrays.equals(t.moveDirection, origin)){
                // right
                addIfLegal(q, new int[]{t.position[0] + 1, t.position[1]}, new int[]{1,0},
                        t.depth + 1);
                // left
                addIfLegal(q, new int[]{t.position[0] - 1, t.position[1]}, new int[]{-1,0},
                        t.depth + 1);
                // up
                addIfLegal(q, new int[]{t.position[0], t.position[1] + 1}, new int[]{0,1},
                        t.depth + 1);
                // down
                addIfLegal(q, new int[]{t.position[0], t.position[1] - 1}, new int[]{0,-1},
                        t.depth + 1);
            }
            // add all legal moves
            else if (t.depth < MAX_DEPTH){
                // right
                addIfLegal(q, new int[]{t.position[0] + 1, t.position[1]}, t.moveDirection, t.depth + 1);
                // left
                addIfLegal(q, new int[]{t.position[0] - 1, t.position[1]}, t.moveDirection,t.depth + 1);
                // up
                addIfLegal(q, new int[]{t.position[0], t.position[1] + 1}, t.moveDirection,t.depth + 1);
                // down
                addIfLegal(q, new int[]{t.position[0], t.position[1] - 1}, t.moveDirection,t.depth + 1);
            }
        }
        // nothing to return so just stay still
        return vectorToBoardCoordinates(enemyRow, enemyCol);
    }

//    private void addIfLegal(PriorityQueue<PathfindingTile> pq, int[] position, int[] moveDirection, float solveCost, int depth) {
//        if (level.obstacles()[position[0]][position[1]].getType() != ObjectType.OBSTACLE){
//            //only add if better
//            // to add hashmap for this
//            pq.add(new PathfindingTile(position, moveDirection, solveCost, depth));
//        }
//    }

private void addIfLegal(Queue<PathfindingTile> q, int[] position, int[] moveDirection, int depth) {
        if (level.inBounds(position[0], position[1]) && !visited.contains(Arrays.toString(position))) {
            GameObject go = level.obstacles()[position[0]][position[1]];
            if (go == null || go.getType() != ObjectType.OBSTACLE) {
                q.addLast(new PathfindingTile(position, moveDirection, 0, depth));
                visited.add(Arrays.toString(position));
            }
        }
}

    /**
     * call for AI controller
     */
    public void resolveAction(enemyState controlSignal, Raft player, long ticks) {
        if (isDestroyed())
            return;

        switch (controlSignal) {
            case SPAWN:
                // find nearest treasure
                break;
            case WANDER:
                // every once in a while pick a new random direction
//                if (ticks % 60 == 0) {
//                    int p = rand.nextInt(4);
//                    // move randomly in one of the four directions
//                    if (p == 0) {
//                        moveVector.set(0, 1);
//                    } else if (p == 1) {
//                        moveVector.set(0, -1);
//                    } else if (p == 2) {
//                        moveVector.set(-1, 0);
//                    } else {
//                        moveVector.set(1, 0);
//                    }
//                    calculateImpulse(ENEMY_WANDER_SPEED, 0.9f);
//                }
//                int x = rand.nextInt(PROTECT_RANGE * 2) - PROTECT_RANGE;
//                int y = rand.nextInt(PROTECT_RANGE * 2) - PROTECT_RANGE;
                if (closestTreasure == null) {
                    moveVector.set(pathFind(-1, -1, false));
                }
                else{
                    moveVector.set(pathFind(closestTreasure));
                }
                calculateImpulse(ENEMY_WANDER_SPEED, 0);
                break;
            case CHASE:
                // find a normal vector pointing to the target player
                moveVector.set(pathFind(targetRaft)).nor();
                // apply a linear impulse to accelerate towards the player, up to a max speed of ENEMY_CHASE_SPEED
                calculateImpulse(ENEMY_CHASE_SPEED, 0);
                break;
            case ENRAGE:
                // find a normal vector pointing to the target player
                moveVector.set(pathFind(targetRaft)).nor();
                // apply a linear impulse to accelerate towards the player, up to a max speed of ENEMY_ENRAGE_CHASE_SPEED
                calculateImpulse(ENEMY_ENRAGE_CHASE_SPEED, 0);
                break;
            default:
                // illegal state
                assert (false);
                break;
        }
    }

    /**
     * Sets moveVector so that applying it as a linear impulse brings this object's velocity closer to
     * moveVector*topSpeed.
     * Precondition: moveVector.len() == 1.
     * @param topSpeed Won't apply an impulse that takes us above this speed
     * @param smoothing Impulse is scaled by (1-smoothing). Higher smoothing means wider turns, slower responses.
     */
    private void calculateImpulse(float topSpeed, float smoothing) {
        float currentSpeed = physicsObject.getBody().getLinearVelocity().dot(moveVector); // current speed in that direction
        float impulseMagnitude = (topSpeed - currentSpeed)*physicsObject.getBody().getMass()*(1-smoothing);
        moveVector.scl(impulseMagnitude);
    }

    /* DISPLAY AND ANIMATION */

    // ANIMATION
    private static float HORIZONTAL_OFFSET = 0.0f;
    /** How much to enlarge the shark. */
    private static float TEXTURE_SCALE = 1.50f;
    /** The animation speed for the shark. */
    private static float IDLE_AS = 0.05f;
    private static float ATTACK_AS = 0.05f;
    /** The number of frames for this animation. */
    private static int IDLE_F = 9;
    private static int ATTACK_F = 7;
    /** Which frame to start on the filmstrip with this animation. */
    private static int IDLE_SF = 8;
    private static int ATTACK_SF = 0;

    // The value to increment once the animation time has passed. Used to calculate which frame should be used.
    private int frameCount = 0;
    // The amount of time elapsed, used for checking whether to increment frameCount.
    private float timeElapsed = 0;
    // Which frame should be set for drawing this game cycle.
    private int frame = IDLE_SF;
    // whether the shark was enraged in the last animation frame
    private boolean wasEnraged = false;

    /**
     * Realign the shark sprite so that the bottom of it is at the bottom of the physics object.
     */
    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(HORIZONTAL_OFFSET,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
    }

    /**
     * Set the filmstrip frame before call the super draw method.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas, Color color){
        ((FilmStrip) texture).setFrame(frame);
        super.draw(canvas, color);
    }

    /**
     * Method to set animation based on the time elapsed in the game.
     * @param dt the current time in the game.
     */
    public void setAnimationFrame(float dt) {
        timeElapsed += dt;
        if (isEnraged() != wasEnraged) {
            // animation changed, set frame to 0
            frameCount = 0;
            timeElapsed = 0;
            wasEnraged = isEnraged();
        }
        // update animation frame
        if(!isEnraged()) { // not attacking
            setFrame(IDLE_AS, IDLE_F, IDLE_SF, false);
        } else {
            setFrame(ATTACK_AS, ATTACK_F, ATTACK_SF, false);
        }
        // flip texture based on movement
        float flip = getLinearVelocity().x > 0 ? -1 : 1;
        textureScale.x = flip * Math.abs(textureScale.y);
    }

    /**
     * Sets the frame of the animation based on the FSM and time given.
     * @param animationSpeed how many seconds should pass between each frame.
     * @param frames the number of frames this animation has.
     * @param start which frame in the FilmStrip the animation starts on.
     * @param reverse whether the animation should be drawn backwards.
     * @return whether it has reached the last animation image.
     */
    private void setFrame(float animationSpeed, int frames, int start, boolean reverse){
        if (timeElapsed > animationSpeed){
            timeElapsed = 0;
            frameCount += 1;
            frame = start + (reverse ? (frames - 1) - frameCount % frames : frameCount % frames);
        }
    }

    /**
     * Checks whether the current frame is the starting or ending frame.
     * @param frames the amount of frames for the given animation.
     * @param start the starting index.
     * @param begin whether to check for the starting or ending index.
     * @return whether the current frame is the start or end frame.
     */
//    private boolean isFrame(int frames, int start, boolean begin){
//        return begin ? frame == start : frame == frames - 1 + start;
//    }

}
