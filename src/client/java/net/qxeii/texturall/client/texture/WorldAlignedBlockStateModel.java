package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
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
    private final WorldAlignedTextureMaterial material;

    public WorldAlignedBlockStateModel(BlockStateModel delegate, WorldAlignedTextureMaterial material) {
        this.delegate = delegate;
        this.material = material;
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
            Math.floorMod(pos.getX(), material.sheetSize()),
            Math.floorMod(pos.getY(), material.sheetSize()),
            Math.floorMod(pos.getZ(), material.sheetSize())
        );
    }

    private void emitCanonicalFace(QuadEmitter emitter, BlockPos pos, Direction face) {
        emitter.nominalFace(face);
        emitter.cullFace(face);
        writeFacePositions(emitter, face);
        emitter.color(0, WHITE);
        emitter.color(1, WHITE);
        emitter.color(2, WHITE);
        emitter.color(3, WHITE);
        remapUv(emitter, pos, face);
        emitter.spriteBake(sheetSprite(), MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
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

    private void remapUv(MutableQuadView quadView, BlockPos pos, Direction face) {
        int tileSpan = material.sheetSize();
        for (int vertex = 0; vertex < 4; vertex++) {
            float worldX = pos.getX() + quadView.x(vertex);
            float worldY = pos.getY() + quadView.y(vertex);
            float worldZ = pos.getZ() + quadView.z(vertex);
            float faceU = switch (face) {
                case UP, DOWN, NORTH -> worldX;
                case SOUTH -> -worldX;
                case EAST -> worldZ;
                case WEST -> -worldZ;
            };
            float faceV = switch (face) {
                case UP -> -worldZ;
                case DOWN -> worldZ;
                case NORTH, SOUTH, EAST, WEST -> -worldY;
            };
            float worldU = wrapPixels(faceU * 16.0F, tileSpan * 16.0F);
            float worldV = wrapPixels(faceV * 16.0F, tileSpan * 16.0F);
            quadView.uv(vertex, worldU / (tileSpan * 16.0F), worldV / (tileSpan * 16.0F));
        }
    }

    private static float wrapPixels(float value, float bound) {
        float wrapped = value % bound;
        return wrapped < 0.0F ? wrapped + bound : wrapped;
    }

    private Sprite sheetSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(material.spriteId());
    }

    private record GeometryKey(Class<?> delegateType, int x, int y, int z) {
    }
}
