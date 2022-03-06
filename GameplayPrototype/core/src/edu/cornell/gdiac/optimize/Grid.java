package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
        // int that indicates which texture to draw
        // 0 means ocean
        public int texture_id;
        // x position in terms of tiles
        public int x;
        // y position in terms of tiles
        public int y;
        /**
         * Constructor
         * @param id
         */
        public Tile(int id, int x, int y){
            texture_id = id;
            this.x = x;
            this.y = y;
        }
    }

    // Json that stores information about the map.
    private final JsonValue data = null;
    // 2D array of arrays that stores all the tiles.
    private Array<Array<Tile>> grid;
    // Array to store all
    private Array<GameObject> objects;
    // Texture to store background tiles to draw.
    private Texture ocean_tile;
    // Size in pixels of a tile // TODO load from json or another class
    private final float tile_size;


    /**
     * Initializes a blank ocean with horz * vert number of tiles.
     * @param horz
     * @param vert
     */
    public Grid(int horz, int vert, float tile_size){
        this.tile_size = tile_size;
        grid = new Array<>();
        for(int ii = 0; ii < horz; ii++){
            Array<Tile> temp = new Array<>();
            for(int jj = 0; jj < vert; jj++){
                temp.add(new Tile(0, ii, jj));
            }
            grid.add(temp);
        }
    }

    /**
     * TODO Constructor for more flexible use with appropriate Json schema.
     * @param data
     */
    public Grid(JsonValue data){
        tile_size = 0.0f;
    }

    /**
     * Method to set the ocean texture of the grid for drawing.
     * @param texture
     */
    public void setOceanTexture(Texture texture) {
        ocean_tile = texture;
    }

    /**
     * Iterates through all the tiles in grid and draws them according to the position of the player.
     * TODO Find more efficient way of drawing all tiles?
     * @param canvas
     */
    public void drawAffine(GameCanvas canvas, Vector2 affine) {
        for(Array<Tile> col : grid){
            for(Tile t : col){
                if( t.texture_id == 0 ){
                    float s = tile_size / ocean_tile.getHeight();
                    canvas.draw(ocean_tile, Color.WHITE, 0, 0,
                            t.x*tile_size + affine.x, t.y*tile_size + affine.y, 0.0f, s, s);
//                    canvas.drawBackgroundAffine(ocean_tile, new Vector2(affine.x + t.x * tile_size, affine.y + t.y * tile_size ));
                }
            }
        }
    }
}
