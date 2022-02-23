package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;
import edu.cornell.gdiac.optimize.RandomController;

/**
 * Model class for driftwood.
 */
public class Wood extends GameObject {
    // TODO: show the wood's log number by color in texture

    // Attributes
    /** How many logs is in this pile of wood. player health will add correspondingly */
    private final int wood;

    // Constants
    /** the maximum log generated for each pile of wood */
    private final static int MAXIMUM_WOOD_GENERATION = 2;
    /** the minimum log generated for each pile of wood */
    private final static int MINIMUM_WOOD_GENERATION = 1;

    /** Constructor, build a pile of wood with random number of logs */
    public Wood(boolean doubled){
        // Consider: wood = RandomController.rollInt(MINIMUM_WOOD_GENERATION, MAXIMUM_WOOD_GENERATION);
        if(doubled){
            wood = MAXIMUM_WOOD_GENERATION;
        }else{
            wood = MINIMUM_WOOD_GENERATION;
        }
    }

    /** get the type of wood objects
     * @return object type wood */
    @Override
    public ObjectType getType() {
        return ObjectType.WOOD;
    }

    /** return the number of logs in this pile of wood
     * @return int representing player health replenish */
    public int getWood() {
        return wood;
    }

    /** set this pile of wood to be destroyed
     * @param value whether to set the wood as destroyed */
    @Override
    public void setDestroyed(boolean value) {
        super.setDestroyed(value);
    }
}
