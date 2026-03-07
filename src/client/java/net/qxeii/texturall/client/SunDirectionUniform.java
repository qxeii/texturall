package net.qxeii.texturall.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryStack;

public final class SunDirectionUniform {
    private static final int SIZE = 16;

    private static GpuBuffer buffer;
    private static GpuBufferSlice slice;

    private SunDirectionUniform() {
    }

    public static void upload(float sunAngle) {
        ensureBuffer();

        float x = (float) -Math.sin(sunAngle);
        float y = (float) Math.cos(sunAngle);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = Std140Builder.onStack(stack, SIZE)
                .putVec3(x, y, 0.0F)
                .get();
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(buffer.slice(), data);
        }
    }

    public static GpuBufferSlice slice() {
        ensureBuffer();
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
