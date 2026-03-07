package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class WorldAlignedBlockStateModel implements BlockStateModel, FabricBlockStateModel {
    private static final int WHITE = 0xFFFFFFFF;

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
            emitCanonicalFace(emitter, pos, face);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockRenderView blockView, BlockPos pos, BlockState state, Random random) {
        return new GeometryKey(
            delegate.getClass(),
            Math.floorMod(pos.getX(), mat.sheetSize()),
            Math.floorMod(pos.getY(), mat.sheetSize()),
            Math.floorMod(pos.getZ(), mat.sheetSize())
        );
    }

    private void emitCanonicalFace(QuadEmitter emitter, BlockPos pos, Direction face) {
        emitter.nominalFace(face);
        emitter.cullFace(face);
        // Disable Indigo's baked directional shading so vertex colors carry only
        // the lightmap value. block.fsh applies per-pixel directional shading instead.
        emitter.diffuseShade(false);
        emitter.shadeMode(ShadeMode.VANILLA);
        writeFacePositions(emitter, face);
        emitter.color(0, WHITE);
        emitter.color(1, WHITE);
        emitter.color(2, WHITE);
        emitter.color(3, WHITE);
        remapUv(emitter, pos, face);
        emitter.spriteBake(sheetSprite(), MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
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

    private record GeometryKey(Class<?> delegateType, int x, int y, int z) {
    }
}
