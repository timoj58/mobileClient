package oddymobstar.graphics.programs;

import android.content.Context;

import oddymobstar.util.graphics.opengles.RawResourceLoader;
import oddymobstar.util.graphics.opengles.ShaderHelper;

import static android.opengl.GLES20.glUseProgram;

/**
 * Created by timmytime on 10/11/15.
 */
public abstract class ShaderProgram implements ProgramInterface {


    protected static final String U_COLOR = "u_Color";
    protected static final String A_POSITION = "a_Position";
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TIME = "u_Time";
    protected static final String U_VECTOR_TO_LIGHT = "u_VectorToLight";
    protected static final String U_MV_MATRIX = "u_MVMatrix";
    protected static final String U_IT_MV_MATRIX = "u_IT_MVMatrix";
    protected static final String U_MVP_MATRIX = "u_MVPMatrix";
    protected static final String U_POINT_LIGHT_POSITIONS = "u_PointLightPositions";
    protected static final String U_POINT_LIGHT_COLORS = "u_PointLightColors";
    protected static final String A_NORMAL = "a_Normal";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    protected static final String A_DIRECTION_VECTOR = "a_DirectionVector";
    protected static final String A_PARTICLE_START_TIME = "a_ParticleStartTime";


    protected int aPositionLocation, uColorLocation, aTextureCoordinatesLocation, uTextureUnitLocation, uMatrixLocation,
            uVectorToLightLocation, uMVMatrixLocation, uIT_MVMatrixLocation, uMVPMatrixLocation, uPointLightPositionsLocation,
            uPointLightColorsLocation, aNormalLocation;


    protected int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {

        program = ShaderHelper.buildProgram(RawResourceLoader.readRawResource(context, vertexShaderResourceId),
                RawResourceLoader.readRawResource(context, fragmentShaderResourceId));
    }

    public void useProgram() {
        glUseProgram(program);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getaTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }

    public int getNormalAttributeLocation() {
        return aNormalLocation;
    }

}
