package edu.cornell.gdiac.raftoftheseus.model;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import com.badlogic.gdx.ai.msg.PriorityQueue;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Queue;

import static edu.cornell.gdiac.raftoftheseus.model.Shark.ENEMY_CHASE_SPEED;
import static edu.cornell.gdiac.raftoftheseus.model.Shark.ENEMY_ENRAGE_CHASE_SPEED;
import static edu.cornell.gdiac.raftoftheseus.model.Shark.enemyState.*;



public class SharkController {


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
        public String toString(){
            return position[0] + "---" + position[1];
        }

        @Override
        public int compareTo(PathfindingTile o) {
            return Float.compare(cost, o.cost);
        }

        @Override
        public int hashCode() {
            return position[0] * 33333 + position[1] * 5;
        }
    }


    Random rand = new Random();

    /**
     * How close a target must be for us to chase it
     */
    private static int CHASE_DIST = 12;

    /**
     * How close a target must be for us to chase it while enraged
     */
    private static final int ENRAGE_CHASE_DIST = 12;

    /**
     * How many ticks to enrage for
     */
    private static final int ENRAGE_DURATION = 5 * 30;

    private int id;

    private Shark shark;

    private Raft raft;

    private LevelModel level;

    private Shark.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;
    /**
     * The number of ticks when we last entered the enraged state
     */
    private long enrage_timestamp;

    private PriorityQueue<PathfindingTile> frontier;

    private HashSet<String> visited;

    // Set class constants
    public static void setConstants(JsonValue objParams){
        CHASE_DIST = objParams.getInt("chase distance", 12);
    }

    public SharkController(int id, Shark shark, Raft raft, LevelModel level) {
        this.id = id;
        this.shark = shark;
        this.raft = raft;
        this.level = level;
        state = SPAWN;
        ticks = 0;
        frontier = new PriorityQueue<>();
        visited = new HashSet<>();
    }

    public long getTicks(){
        return ticks;
    }

    public Shark.enemyState getState(){
        return state;
    }

    private float dist(){
        return shark.getPosition().dst(raft.getPosition());
    }

    private void enrage(){
        enrage_timestamp = ticks;
        shark.setEnraged(true);
        state = ENRAGE;
    }


    private void changeStateIfApplicable() {
//        int p = rand.nextInt(30000);
        int p = 0;
        // change state when shark gets hit by a spear (bad way to do this, but it's the only possible way without refactoring the architecture.)
        if (state == ENRAGE && !shark.isEnraged()) {
            state = WANDER;
            enrage_timestamp = ticks;
        }
        //
        switch (state) {
            case SPAWN:
                state = WANDER;
                break;
            case WANDER:
                if (p <= ticks && dist() <= ENRAGE_CHASE_DIST && ticks >= enrage_timestamp + ENRAGE_DURATION){
                    enrage();
                }
                else if (dist() <= CHASE_DIST)
                    state = CHASE;

                break;
            case CHASE:
                if (p <= ticks && dist() <= ENRAGE_CHASE_DIST && ticks >= enrage_timestamp + ENRAGE_DURATION){
                    enrage();
                }
                else if (dist() > CHASE_DIST)
                    state = WANDER;
                break;
            case ENRAGE:
                if (ticks >= enrage_timestamp + ENRAGE_DURATION || dist() > ENRAGE_CHASE_DIST){
                    state = WANDER;
                    shark.setEnraged(false);
                    enrage_timestamp = ticks;
                }
                break;
            default:
                // illegal state
                assert (false);
                state = WANDER;
                break;
        }
    }

    public boolean isAlive() {
        return (shark != null && !shark.isDestroyed());
    }

    private Vector2 vectorToBoardCoordinates(int row, int col){
        Vector2 dest = new Vector2(level.boardToScreen(row), level.boardToScreen(col));
        return dest.sub(shark.getPosition()).nor();
    }

    /** get directional vector at a grid location
     * precondition: the grid space is either current or empty **/
    private Vector2 getCurrentDirectionalVector(int x, int y){
        GameObject o = level.obstacles()[x][y];
        if (o != null && o.getType() == GameObject.ObjectType.CURRENT){
            return ((Current) level.obstacles()[x][y]).getDirectionVector();
        }
        // if it's empty there's no vector
        return new Vector2();
    }

    /** how much does it cost to move off of this tile in a direction onto the other tile?
     * precond: both tiles are either current or empty
     * precond: controlsignal is wander, chase, or enrage
     **/
    private float solveCost(int[] pos,  int[] dir){
        int x = pos[0];
        int y = pos[1];
        float moveDist = (level.getTileSize() / 2);
        // moving along a diagonal
        if (dir[0] != 0 && dir[1] != 0){
            moveDist *= Math.sqrt(2);
        }
        float speed = 0;
        if (state == Shark.enemyState.WANDER){
            speed = Shark.ENEMY_WANDER_SPEED;
        }
        else if (state == Shark.enemyState.CHASE){
            speed = ENEMY_CHASE_SPEED;
        }
        else if (state == Shark.enemyState.ENRAGE){
            speed = ENEMY_ENRAGE_CHASE_SPEED;
        }
        Vector2 movementVector = new Vector2(dir[0], dir[1]).scl(speed);
        Vector2 movementVector2 = new Vector2(dir[0], dir[1]).scl(speed);
        // solve time to move across each half of the tile
        // not totally accurate but good enough estimate if someone is better at lin alg please fix
        // take projection of added vectors onto intended movement vector
        Vector2 tile1Movement = new Vector2(dir[0], dir[1]).scl(movementVector.add(getCurrentDirectionalVector(x, y)).dot(new Vector2(dir[0], dir[1]).nor()));
        Vector2 tile2Movement = new Vector2(dir[0], dir[1]).scl(movementVector2.add(getCurrentDirectionalVector(x + dir[0], y + dir[1])).dot(new Vector2(dir[0], dir[1]).nor()));
        return tile1Movement.len() / moveDist + tile2Movement.len() / moveDist;
    }

    private boolean isSafe(int[] position){
        if (level.inBounds(position[0], position[1])) {
            GameObject go = level.obstacles()[position[0]][position[1]];
            return (go == null || go.getType() != GameObject.ObjectType.ROCK);
        }
        return false;
    }

    private void addToFrontierIfLegal(int[] position, int[] moveDirection, float cost, int depth){
//        int[] fin = {position[0] + moveDirection[0], position[1] + moveDirection[1]};
        PathfindingTile tile = new PathfindingTile(position, moveDirection, cost, depth);
        if (isSafe(position) && !visited.contains(tile.toString())) {
            frontier.add(tile);
            visited.add(tile.toString());
        }

    }

    private static int[] add(int[] a, int[] b){
        int[] c = {a[0] + b[0], a[1] + b[1]};
        return c;
    }

    private Vector2 djikstra(int goalCol, int goalRow){
        int[] origin = {level.screenToBoard(shark.getX()),level.screenToBoard(shark.getY())};
//        System.out.println(Arrays.toString(origin));
        frontier.clear();
        visited.clear();
        // add origin
        addToFrontierIfLegal(origin, null, 0, 0);
        while (frontier.size() > 0){

            PathfindingTile current = frontier.poll();

//            System.out.println(frontier.size());
//            System.out.println(Arrays.toString(current.moveDirection));
//            if (current.position[0] == 6 && current.position[1] == 5){
//                System.out.println(level.obstacles()[6][5]);
//            }
            if (current.position[0] == goalCol && current.position[1] == goalRow){
//                System.out.println("found");
                if (current.moveDirection == null){
                    return new Vector2(raft.getPosition().sub(shark.getPosition()).nor());
                }
                return new Vector2(current.moveDirection[0], current.moveDirection[1]);
            }
            int[] dirs = {-1, 0, 1};
            // for all 8 directions (3x3 = 9 but ignore 0, 0 case)
            for (int col : dirs){
                for (int row : dirs){
                    if (!(col == 0 && row == 0)){
                        int[] mod = {col, row};
                        int[] origDir = current.moveDirection;
                        if (Arrays.equals(current.position, origin)){
                            // init original directions to return
                            origDir = mod;
                        }
                        // add that direction
//                        System.out.println(Arrays.toString(current.position));
                        if (isSafe(current.position) && isSafe(add(current.position, mod))){
                            addToFrontierIfLegal(add(current.position, mod), origDir, current.cost + solveCost(current.position, mod), 1);
                        }
                    }
                }
            }

        }
        return new Vector2();
    }

    public Vector2 pathfind(Raft raft){
        return djikstra(level.screenToBoard(raft.getX()), level.screenToBoard(raft.getY()));
    }

    public void updateShark() {
        ticks++;
        if ((id + ticks) % 10 == 0) {
            // Process the FSM
            changeStateIfApplicable();
            // Pathfinding
//            markGoalTiles();
//            move = getMoveAlongPathToGoalTile();
        }
        if (shark.isDestroyed())
            return;

        switch (state) {
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
//                if (closestTreasure == null) {
//                    moveVector.set(pathFind(-1, -1, false));
//                }
//                else{
//                    moveVector.set(pathFind(closestTreasure));
//                }
//                 calculateImpulse(ENEMY_WANDER_SPEED, 0);
                break;
            case CHASE:
                // find a normal vector pointing to the target player
                shark.moveVector.set(pathfind(raft)).nor();
                // apply a linear impulse to accelerate towards the player, up to a max speed of ENEMY_CHASE_SPEED
                shark.calculateImpulse(ENEMY_CHASE_SPEED, 0);
                break;
            case ENRAGE:
                // find a normal vector pointing to the target player
                shark.moveVector.set(pathfind(raft)).nor();
                // apply a linear impulse to accelerate towards the player, up to a max speed of ENEMY_ENRAGE_CHASE_SPEED
                shark.calculateImpulse(ENEMY_ENRAGE_CHASE_SPEED, 0);
                break;
            default:
                // illegal state
                assert (false);
                break;
        }
    }



}