package edu.cornell.gdiac.raftoftheseus;

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

    public void updateCurrentEffects (GameObject o) {
        Vector2 position = o.getPosition();
        int column = (int)MathUtils.clamp((position.x - 0.5f*resolution) / resolution, 0, columns - 1);
        int row = (int)MathUtils.clamp((position.y - 0.5f*resolution) / resolution, 0, rows - 1);
        float lx = column * resolution;
        float ly = row * resolution;
        float rx = (column + 1) * resolution;
        float ry = (row + 1) * resolution;
        temp_cpy = field[column][row].cpy();
        temp_sum.add(temp_cpy.scl(rx - position.x).scl(ry - position.y));
        if(column + 1 < columns){
            temp_cpy = field[column + 1][row].cpy();
            temp_sum.add(temp_cpy.scl(position.x - lx).scl(ry - position.y));
        }
        if(row + 1 < rows){
            temp_cpy = field[column][row + 1].cpy();
            temp_sum.add(temp_cpy.scl(rx - position.x).scl(position.y - ly));
        }
        if((column + 1 < columns) && (row + 1 < rows)){
            temp_cpy = field[column + 1][row + 1].cpy();
            temp_sum.add(temp_cpy.scl(position.x - lx).scl(position.y - ly));
        }
        temp_sum.scl(rx - lx).scl(ry - ly);
        o.getPosition().add(temp_sum);
        temp_sum.setZero();
    }
}
