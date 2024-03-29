package oddymobstar.util.graphics.opengles.object;

import java.nio.ByteBuffer;

import oddymobstar.graphics.data.VertexArray;
import oddymobstar.graphics.model.ModelInterface;
import oddymobstar.graphics.programs.ShaderProgram;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glDrawElements;

/**
 * Created by timmytime on 17/11/15.
 */
public class SkyBox implements ModelInterface {

    private static final int POSITION_COMPONENT_COUNT = 3;
    private final VertexArray vertexArray;
    private final ByteBuffer indexArray;

    public SkyBox() {
        vertexArray = new VertexArray(new float[]{
                -1, 1, 1, //top left near
                1, 1, 1, //top right near
                -1, -1, 1, //bottom right near
                1, -1, 1, //bottom left near
                -1, 1, -1,//top left far
                1, 1, -1, //top right far
                -1, -1, -1, //bottom left far
                1, -1, -1 //bottom right far
        });

        indexArray = ByteBuffer.allocate(6 * 6).put(new byte[]{
                //front
                1, 3, 0,
                0, 3, 2,
                //back
                4, 6, 5,
                5, 6, 7,
                //left
                0, 2, 4,
                4, 2, 6,
                //right
                5, 7, 1,
                1, 7, 3,
                //top
                5, 1, 4,
                4, 1, 0,
                //bottom
                6, 2, 7,
                7, 2, 3
        });

        indexArray.position(0);

    }

    public void bindData(ShaderProgram shaderProgram) {
        vertexArray.setVertextAttribPointer(0, shaderProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_BYTE, indexArray);
    }
}
