package edu.cornell.gdiac.raftoftheseus.model;

/* TILED CONSTANTS */
public class Tiled {
    /** Index of the representation of default in tile set texture */
    protected static final int DEFAULT = 0;
    /** Index of the representation of start in tile set texture */
    protected static final int START = 1;
    /** Index of the representation of enemy in tile set texture */
    protected static final int ENEMY_SIREN = 2;
    /** Index of the representation of enemy in tile set texture */
    protected static final int ENEMY_SHARK = 3;
    /** Offset of north current in tile set index */
    protected static final int NORTH_EAST = 4;
    /** Offset of east current in tile set index */
    protected static final int EAST = 5;
    /** Offset of east current in tile set index */
    protected static final int EAST_SOUTH = 6;
    /** Offset of south current in tile set index */
    protected static final int SOUTH = 7;
    /** Index of the representation of rock in tile set texture */
    protected static final int ROCK_ALONE = 8;
    /** Index of the representation of rock in tile set texture */
    protected static final int ROCK_SHARP = 9;
    /** Index of the representation of goal in tile set texture */
    protected static final int GOAL = 10;
    /** Offset of south current in tile set index */
    protected static final int SOUTH_WEST = 11;
    /** Offset of west current in tile set index */
    protected static final int WEST = 12;
    /** Offset of west current in tile set index */
    protected static final int WEST_NORTH = 13;
    /** Offset of north current in tile set index */
    protected static final int NORTH = 14;
    /** Strong current offset */
    protected static final int STRONG_CURRENT = 14;
    /** Index of the representation of treasure in tile set texture */
    protected static final int TREASURE = 15;
    /** Index of the representation of wood with the lowest amount in tile set texture */
    protected static final int WOOD_LOW = 16;
    /** Index of the representation of wood with the second-lowest amount  in tile set texture */
    protected static final int WOOD_MIDDLE = 17;
    /** Index of the representation of wreck in tile set texture */
    protected static final int WRECK = 22;
    /** Index of the representation of wood with the default amount  in tile set texture */
    protected static final int WOOD_DEFAULT = 23;
    /** Index of the representation of wood with the highest amount  in tile set texture */
    protected static final int WOOD_HIGH = 24;
    /** Index of the representation of default current in tile set texture */
    protected static final int LAND_OFFSET = 28;
    /** Index of the representation of default current in tile set texture */
    protected static final int SEA = 42;
    /** the offset for sand textures */
    protected static final int SAND_OFFSET = 42;
    /** Index of the representation of plant in tile set texture */
    protected static final int HIGH_PLANT_START = 56;
    public static final int HIGH_PLANT_END = 62;
    public static final int PLANT_START = 63;
    /** Index of the representation of plant in tile set texture */
    protected static final int PLANT_END = 69;
    /** Index of the representation of plant in tile set texture */
    protected static final int HYDRA = 70;
    public static final int FIXED_PLANT_COUNT = 2;
    /** Total variation of terrains */
    protected static final int TERRAIN_TYPES = SEA - LAND_OFFSET - 1;
    protected static final int FULL_LAND = 7;
    /*=*=*=*=*=*=*=*=*=* TILED CURRENT DIRECTION CONSTANTS *=*=*=*=*=*=*=*=*=*/
    /** layer of environment and land */
    protected static final int LAYER_ENV = 0;
    /** layer of collectables and shark */
    protected static final int LAYER_COL = 1;
    /** layer of siren */
    protected static final int LAYER_SIREN = 2;

    /** @return the stationary type according to the tile int*/
    protected static Stationary.StationaryType computeRockType(int tile_int) {
        switch(tile_int){
            case Tiled.ROCK_ALONE: return Stationary.StationaryType.REGULAR_ROCK;
            case Tiled.ROCK_SHARP: return Stationary.StationaryType.SHARP_ROCK;
            default:
                if(tile_int > Tiled.SEA && tile_int <= Tiled.HIGH_PLANT_END) { return Stationary.StationaryType.CLIFF_TERRAIN; }
                else{ return Stationary.StationaryType.TERRAIN; }
        }
    }

    /** @return whether this current is a strong current*/
    protected static boolean isStrongCurrent(int tile_int){
        int div = tile_int % 7;
        if(div == 1 || div == 2 || div == 3) {return false;}
        return (tile_int <= Tiled.LAND_OFFSET && tile_int > Tiled.TREASURE);
    }

    /** @return the rock_int according to the tile_int: 0 for reg, 1-13 for land, -1 for sharp, -2 for plant */
    protected static int computeRockInt(int tile_int){
        if (tile_int == Tiled.ROCK_ALONE || tile_int == Tiled.ROCK_SHARP){ return Stationary.REGULAR; }
        if (tile_int > Tiled.LAND_OFFSET && tile_int < Tiled.SEA){ return tile_int - Tiled.LAND_OFFSET; }
        if (tile_int > Tiled.SAND_OFFSET && tile_int < Tiled.HIGH_PLANT_START){ return tile_int - Tiled.SAND_OFFSET; }
        if (tile_int == Tiled.HIGH_PLANT_START || tile_int == Tiled.PLANT_START){ return Stationary.plantA; }
        if (tile_int == Tiled.HIGH_PLANT_START + 1 || tile_int == Tiled.PLANT_START + 1){ return Stationary.plantB; }
        if (tile_int == Tiled.HIGH_PLANT_START + 2 || tile_int == Tiled.PLANT_START + 2){ return Stationary.plantC; }
        if (tile_int == Tiled.HIGH_PLANT_START + 3 || tile_int == Tiled.PLANT_START + 3){ return Stationary.plantD; }
        if (tile_int == Tiled.HIGH_PLANT_START + 4 || tile_int == Tiled.PLANT_START + 4){ return Stationary.plantB; }
        if (tile_int == Tiled.HIGH_PLANT_END - 1 || tile_int == Tiled.PLANT_END - 1){ return Stationary.plantC; }
        if (tile_int == Tiled.HIGH_PLANT_END || tile_int == Tiled.PLANT_END){ return Stationary.plantD; }
        return Stationary.NON_ROCK;
    }

    /** Compute the direction of the current base on the level json input
     * @param i level json input
     * @return the direction of the current */
    protected static Current.Direction compute_direction(int i) {
        switch (i){
            case Tiled.NORTH: return Current.Direction.NORTH;
            case Tiled.SOUTH: return Current.Direction.SOUTH;
            case Tiled.EAST: return Current.Direction.EAST;
            case Tiled.WEST: return Current.Direction.WEST;
            case Tiled.NORTH_EAST: return Current.Direction.NORTH_EAST;
            case Tiled.SOUTH_WEST: return Current.Direction.SOUTH_WEST;
            case Tiled.EAST_SOUTH: return Current.Direction.EAST_SOUTH;
            case Tiled.WEST_NORTH: return Current.Direction.WEST_NORTH;
            default: System.out.print("un-parse-able information: " + i); return Current.Direction.NONE;
        }
    }

    /** This function of magic number compute the best tile type to add to the top layer. */
    protected static int computeExtend(int rock_int){
        if(Stationary.isPlant(rock_int)) {return 7;}
        switch (rock_int){
            case Stationary.REGULAR:
                return Stationary.REGULAR;
            case 1: case 2:
                return rock_int + 7;
            case 3: case 4: case 7: case 13:
                return 7;
            case 5:
                return 13;
            case 6: case 9: case 10:
                return 6;
            case 8: case 11: case 12:
                return 12;
            default:
                return Stationary.NON_ROCK;
        }
    }
}
