package net.qxeii.texturall.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.state.WorldRenderState;
import net.qxeii.texturall.mixin.client.WorldRendererAccessor;
import org.lwjgl.system.MemoryStack;

public final class SunDirectionUniform {
    private static final int SIZE = 32;

    private static GpuBuffer buffer;
    private static GpuBufferSlice slice;

    private SunDirectionUniform() {
    }

    public static GpuBufferSlice upload() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        ensureBuffer();

        WorldRenderState worldRenderState = ((WorldRendererAccessor) client.worldRenderer).texturall$getWorldRenderState();
        float sunAngle = worldRenderState.skyRenderState.sunAngle;
        float moonAngle = worldRenderState.skyRenderState.moonAngle;

        float sunX = (float) -Math.sin(sunAngle);
        float sunY = (float) Math.cos(sunAngle);
        float moonX = (float) -Math.sin(moonAngle);
        float moonY = (float) Math.cos(moonAngle);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = Std140Builder.onStack(stack, SIZE)
                .putVec3(sunX, sunY, 0.0F)
                .putVec3(moonX, moonY, 0.0F)
                .get();
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(slice, data);
        }

        return slice;
    }

    private static void ensureBuffer() {
        if (buffer != null && !buffer.isClosed()) {
            return;
        }

        buffer = RenderSystem.getDevice().createBuffer(
            () -> "Texturall Sun Direction",
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
            SIZE
        );
        slice = buffer.slice();
    }
}
