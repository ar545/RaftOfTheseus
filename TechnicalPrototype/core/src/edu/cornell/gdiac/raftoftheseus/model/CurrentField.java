package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.ai.steer.behaviors.FollowFlowField.FlowField;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.raftoftheseus.steering.SteeringActor;

public class CurrentField implements FlowField<Vector2> {
    Vector2[][] field;
    int rows, columns;
    int resolution;

    /** Create current field in terrain current mode
     * @param height the number of grid rows (y) in the level world
     * @param width the number of grid column (x) in the level world
     * @param obstacles the center of height of the current terrain
     * @param resolution the size of grid in pixels */
    public CurrentField (float width, float height, int resolution, Array<SteeringActor> obstacles) {
        this(width, height, resolution);
        for (int i = 0; i < columns; i++) {
            ROWS:
            for (int j = 0; j < rows; j++) {
                for (int k = 0; k < obstacles.size; k++) {
                    SteeringActor obstacle = obstacles.get(k);
                    if (obstacle.getPosition().dst(resolution * (i + .5f), resolution * (j + .5f)) < obstacle.getBoundingRadius() + 40) {
                        field[i][j] = new Vector2(resolution * (i + .5f), resolution * (j + .5f)).sub(obstacle.getPosition()).nor();
                        continue ROWS;
                    }
                }
                field[i][j] = new Vector2(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            }
        }
    }

    /** Create current field grid-by-grid mode. Initialize an empty array and populate the other fields.
     * @param height the number of grid rows (y) in the level world
     * @param width the number of grid column (x) in the level world
     * @param resolution the size of grid in pixels */
    public CurrentField (float width, float height, int resolution) {
        this.resolution = resolution;
        this.columns = MathUtils.ceil(width / resolution);
        this.rows = MathUtils.ceil(height / resolution);
        this.field = new Vector2[columns][rows];
    }

    @Override
    public Vector2 lookup (Vector2 position) {
        int column = (int)MathUtils.clamp(position.x / resolution, 0, columns - 1);
        int row = (int)MathUtils.clamp(position.y / resolution, 0, rows - 1);
        return field[column][row];
    }

    Vector2 temp_cpy;
    Vector2 temp_sum = new Vector2(0, 0);

    /** Calculate and apply the linear displacement of gameObject o at its location due to current for time dt
     * @param dt time period
     * @param o the game object to act upon */
    public void updateCurrentEffects (GameObject o, float dt) {
        // Define the grid-wise location of the player
        calculateCurrentVelocity(o.getPosition());
        // displacement = velocity * time elapsed
        o.setPosition(o.getPosition().add(temp_sum.scl(dt)));
        temp_sum.setZero();
    }

    /** Calculate the linear velocity at position and store it in temp_sum
     * @param position the position to compute the linear velocity due to current */
    public void calculateCurrentVelocity(Vector2 position){
        int column = MathUtils.clamp((int)(position.x - 0.5f * resolution) / resolution, 0, columns - 1);
        int row = MathUtils.clamp((int)(position.y - 0.5f * resolution) / resolution, 0, rows - 1);
        float lx = (column + 0.5f) * resolution;
        float ly = (row + 0.5f) * resolution;
        float rx = (column + 1.5f) * resolution;
        float ry = (row + 1.5f) * resolution;

        // To the velocity vector, add the scaled lower-left corner contribution
        temp_cpy = field[column][row].cpy();
        temp_sum.add(temp_cpy.scl(rx - position.x).scl(ry - position.y));
        if(column + 1 < columns){ // add the scaled lower-right corner contribution
            temp_cpy = field[column + 1][row].cpy();
            temp_sum.add(temp_cpy.scl(position.x - lx).scl(ry - position.y));
        }
        if(row + 1 < rows){ // add the scaled upper-left corner contribution
            temp_cpy = field[column][row + 1].cpy();
            temp_sum.add(temp_cpy.scl(rx - position.x).scl(position.y - ly));
        }
        if((column + 1 < columns) && (row + 1 < rows)){ // add the scaled upper-right corner contribution
            temp_cpy = field[column + 1][row + 1].cpy();
            temp_sum.add(temp_cpy.scl(position.x - lx).scl(position.y - ly));
        }
        temp_sum.scl(0.12f);
    }

    /** @return the linear velocity at the position input
     * @param position the position to compute the linear velocity due to current */
    public Vector2 getCurrentVelocity(Vector2 position){
        calculateCurrentVelocity(position);
        Vector2 result = temp_sum.cpy();
        temp_sum.setZero();
        return result;
    }
}
