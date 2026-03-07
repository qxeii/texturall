package net.qxeii.texturall.client;

import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class SunDirectionUniform {

    private static int blockShaderGlRef = -1;
    private static int uniformLocation  = -1;

    /**
     * Called for every newly created ShaderProgram. Captures this program if it
     * contains our custom uniform (i.e. it is our overridden block shader).
     */
    public static void tryCapture(ShaderProgram program) {
        int glRef = program.getGlRef();
        int loc   = GL20.glGetUniformLocation(glRef, "u_sunDirection");
        if (loc >= 0) {
            blockShaderGlRef = glRef;
            uniformLocation  = loc;
        }
    }

    /**
     * Upload the sun direction derived from {@code sunAngle} (radians, from
     * {@code WorldRenderState.skyRenderState.sunAngle}). Called once per frame.
     * Sun rises in the east (+X), is overhead at noon (+Y), sets in west (-X).
     */
    public static void upload(float sunAngle) {
        if (uniformLocation < 0) return;

        float x = (float) Math.cos(sunAngle);
        float y = (float) Math.sin(sunAngle);

        // Upload without requiring the block shader to be currently bound.
        int prev = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(blockShaderGlRef);
        GL20.glUniform3f(uniformLocation, x, y, 0.0f);
        GL20.glUseProgram(prev);
    }
}
