package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class WorldAlignedBlockStateModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel delegate;
    private final WorldAlignedTextureMaterial mat;

    public WorldAlignedBlockStateModel(BlockStateModel delegate, WorldAlignedTextureMaterial mat) {
        this.delegate = delegate;
        this.mat = mat;
    }

    @Override
    public void addParts(Random random, List<BlockModelPart> parts) {
        delegate.addParts(random, parts);
    }

    @Override
    public Sprite particleSprite() {
        return sheetSprite();
    }

    @Override
    public Sprite particleSprite(BlockRenderView blockView, BlockPos pos, BlockState state) {
        return sheetSprite();
    }

    @Override
    public void emitQuads(
        QuadEmitter emitter,
        BlockRenderView blockView,
        BlockPos pos,
        BlockState state,
        Random random,
        Predicate<@Nullable Direction> cullTest
    ) {
        for (Direction face : Direction.values()) {
            if (cullTest.test(face)) {
                continue;
            }
            emitCanonicalFace(emitter, blockView, pos, face);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockRenderView blockView, BlockPos pos, BlockState state, Random random) {
        int lightSignature = 1;
        for (Direction face : Direction.values()) {
            lightSignature = 31 * lightSignature + encodeBlockLightDirection(blockView, pos, face);
            lightSignature = 31 * lightSignature + faceLightmap(blockView, pos, face);
        }

        return new GeometryKey(
            delegate.getClass(),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            lightSignature
        );
    }

    private void emitCanonicalFace(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, Direction face) {
        emitter.nominalFace(face);
        emitter.cullFace(face);
        // Disable Indigo's baked directional shading and AO so the shader sees one face-level
        // light sample plus the packed block-light direction payload.
        emitter.diffuseShade(false);
        emitter.ambientOcclusion(TriState.FALSE);
        emitter.shadeMode(ShadeMode.VANILLA);
        writeFacePositions(emitter, face);
        int materialMarker = encodeBlockLightDirection(blockView, pos, face);
        emitter.color(0, materialMarker);
        emitter.color(1, materialMarker);
        emitter.color(2, materialMarker);
        emitter.color(3, materialMarker);
        int faceLight = faceLightmap(blockView, pos, face);
        emitter.lightmap(faceLight, faceLight, faceLight, faceLight);
        remapUv(emitter, pos, face);
        emitter.spriteBake(sheetSprite(), MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
    }

    private int encodeBlockLightDirection(BlockRenderView blockView, BlockPos pos, Direction face) {
        int sampleX = pos.getX() + face.getOffsetX();
        int sampleY = pos.getY() + face.getOffsetY();
        int sampleZ = pos.getZ() + face.getOffsetZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        float x = sampleBlockLight(blockView, mutable, sampleX + 1, sampleY, sampleZ)
            - sampleBlockLight(blockView, mutable, sampleX - 1, sampleY, sampleZ);
        float y = sampleBlockLight(blockView, mutable, sampleX, sampleY + 1, sampleZ)
            - sampleBlockLight(blockView, mutable, sampleX, sampleY - 1, sampleZ);
        float z = sampleBlockLight(blockView, mutable, sampleX, sampleY, sampleZ + 1)
            - sampleBlockLight(blockView, mutable, sampleX, sampleY, sampleZ - 1);

        float normalBias = sampleBlockLight(blockView, mutable, sampleX, sampleY, sampleZ) * 0.35F;
        x += face.getOffsetX() * normalBias;
        y += face.getOffsetY() * normalBias;
        z += face.getOffsetZ() * normalBias;

        float lengthSquared = x * x + y * y + z * z;
        if (lengthSquared > 1.0e-6F) {
            float invLength = (float) (1.0 / Math.sqrt(lengthSquared));
            x *= invLength;
            y *= invLength;
            z *= invLength;
        } else {
            x = face.getOffsetX();
            y = face.getOffsetY();
            z = face.getOffsetZ();
        }

        return (mat.materialIndex() << 24)
            | (encodeUnitChannel(x) << 16)
            | (encodeUnitChannel(y) << 8)
            | encodeUnitChannel(z);
    }

    private static int sampleBlockLight(BlockRenderView blockView, BlockPos.Mutable mutable, int x, int y, int z) {
        return sampleLight(blockView, mutable, LightType.BLOCK, x, y, z);
    }

    private static int sampleLight(BlockRenderView blockView, BlockPos.Mutable mutable, LightType lightType, int x, int y, int z) {
        mutable.set(x, y, z);
        return blockView.getLightLevel(lightType, mutable);
    }

    private static int encodeUnitChannel(float value) {
        float normalized = value * 0.5F + 0.5F;
        if (normalized < 0.0F) {
            normalized = 0.0F;
        } else if (normalized > 1.0F) {
            normalized = 1.0F;
        }
        return Math.round(normalized * 255.0F);
    }

    private static int faceLightmap(BlockRenderView blockView, BlockPos pos, Direction face) {
        BlockPos samplePos = pos.offset(face);
        int blockLight = blockView.getLightLevel(LightType.BLOCK, samplePos);
        int skyLight = blockView.getLightLevel(LightType.SKY, samplePos);
        return LightmapTextureManager.pack(blockLight, skyLight);
    }

    private void remapUv(MutableQuadView quad, BlockPos pos, Direction face) {
        float sheetPixels = mat.sheetSize() * 16.0F;
        float minU = switch (face) {
            case UP, DOWN, NORTH -> pos.getX();
            case SOUTH -> -(pos.getX() + 1);
            case EAST -> pos.getZ();
            case WEST -> -(pos.getZ() + 1);
        };
        float minV = switch (face) {
            case DOWN -> pos.getZ();
            case UP -> -(pos.getZ() + 1);
            default -> -(pos.getY() + 1);
        };
        float baseU = wrapPixels(minU * 16.0F, sheetPixels);
        float baseV = wrapPixels(minV * 16.0F, sheetPixels);

        for (int v = 0; v < 4; v++) {
            float worldX = pos.getX() + quad.x(v);
            float worldY = pos.getY() + quad.y(v);
            float worldZ = pos.getZ() + quad.z(v);
            float faceU = switch (face) {
                case UP, DOWN, NORTH -> worldX;
                case SOUTH -> -worldX;
                case EAST -> worldZ;
                case WEST -> -worldZ;
            };
            float faceV = switch (face) {
                case UP -> -worldZ;
                case DOWN -> worldZ;
                default -> -worldY;
            };
            quad.uv(v, (baseU + (faceU - minU) * 16.0F) / sheetPixels,
                       (baseV + (faceV - minV) * 16.0F) / sheetPixels);
        }
    }

    private static void writeFacePositions(QuadEmitter emitter, Direction face) {
        switch (face) {
            case DOWN -> {
                emitter.pos(0, 0.0F, 0.0F, 0.0F);
                emitter.pos(1, 1.0F, 0.0F, 0.0F);
                emitter.pos(2, 1.0F, 0.0F, 1.0F);
                emitter.pos(3, 0.0F, 0.0F, 1.0F);
            }
            case UP -> {
                emitter.pos(0, 0.0F, 1.0F, 0.0F);
                emitter.pos(1, 0.0F, 1.0F, 1.0F);
                emitter.pos(2, 1.0F, 1.0F, 1.0F);
                emitter.pos(3, 1.0F, 1.0F, 0.0F);
            }
            case NORTH -> {
                emitter.pos(0, 1.0F, 0.0F, 0.0F);
                emitter.pos(1, 0.0F, 0.0F, 0.0F);
                emitter.pos(2, 0.0F, 1.0F, 0.0F);
                emitter.pos(3, 1.0F, 1.0F, 0.0F);
            }
            case SOUTH -> {
                emitter.pos(0, 0.0F, 0.0F, 1.0F);
                emitter.pos(1, 1.0F, 0.0F, 1.0F);
                emitter.pos(2, 1.0F, 1.0F, 1.0F);
                emitter.pos(3, 0.0F, 1.0F, 1.0F);
            }
            case WEST -> {
                emitter.pos(0, 0.0F, 0.0F, 0.0F);
                emitter.pos(1, 0.0F, 0.0F, 1.0F);
                emitter.pos(2, 0.0F, 1.0F, 1.0F);
                emitter.pos(3, 0.0F, 1.0F, 0.0F);
            }
            case EAST -> {
                emitter.pos(0, 1.0F, 0.0F, 1.0F);
                emitter.pos(1, 1.0F, 0.0F, 0.0F);
                emitter.pos(2, 1.0F, 1.0F, 0.0F);
                emitter.pos(3, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static float wrapPixels(float value, float bound) {
        float wrapped = value % bound;
        return wrapped < 0.0F ? wrapped + bound : wrapped;
    }

    private Sprite sheetSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(mat.spriteId());
    }

    private record GeometryKey(Class<?> delegateType, int x, int y, int z, int lightSignature) {
    }
}
