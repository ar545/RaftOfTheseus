package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Class that provides the map screen for the state of the game.
 * */
public class MapController {

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Representation of game objects */
    private GameObject[] gameObjects;

    public MapController(GameCanvas canvas, GameObject[] gameObjects) {
        this.canvas = canvas;
        this.gameObjects = gameObjects;
    }

    public void draw(GameCanvas canvas) {
        for (GameObject obj: gameObjects) {
            //
        }
    }



}
