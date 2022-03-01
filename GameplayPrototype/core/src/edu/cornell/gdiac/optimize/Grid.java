package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * This the grid class for the game.
 *
 * It will construct a data representation of the map with tiles that store information about texture and item placement.
 *
 * Size defined by map dimensions specified in JsonValue file.
 *
 */

public class Grid {

    /**
     * Inner class tile to store information about what to draw.
     */
    private class Tile{

    }

    // Json that stores information about the map.
    private final JsonValue data = null;
    // 2D array of arrays that stores all the tiles.
    private Array<Array<Tile>> grid;
    // Array to store all
    private Array<GameObject> objects;

    // Initializes the ocean tiles
    public Grid(int horz, int vert){

    }

    /**
     * Iterates through all the tiles in grid and draws them according to the position of the player camera.
     * @param canvas
     */
    public void drawAffine(GameCanvas canvas, Vector2 affine) {

    }
}
