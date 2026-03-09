package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class WorldAlignedBlockStateModel implements BlockStateModel, FabricBlockStateModel {
    private static final int EDGE_MATERIAL_MASK = 0x3F;
    public static final int TEXTURALL_TERRAIN_QUAD_TAG = 0x5458544C;

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
        return tileSprite();
    }

    @Override
    public Sprite particleSprite(BlockRenderView blockView, BlockPos pos, BlockState state) {
        return tileSprite();
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
        if (!supportsTerrainShading(blockView)) {
            ((FabricBlockStateModel) delegate).emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        for (Direction face : Direction.values()) {
            if (cullTest.test(face)) {
                continue;
            }
            emitCanonicalFace(emitter, blockView, pos, face);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockRenderView blockView, BlockPos pos, BlockState state, Random random) {
        if (!supportsTerrainShading(blockView)) {
            return ((FabricBlockStateModel) delegate).createGeometryKey(blockView, pos, state, random);
        }

        int lightSignature = 1;
        int mergeSignature = 1;
        for (Direction face : Direction.values()) {
            lightSignature = 31 * lightSignature + sampleFaceLighting(blockView, pos, face).hashCode();
            lightSignature = 31 * lightSignature + sampleFaceOcclusion(blockView, pos, face).hashCode();
            mergeSignature = 31 * mergeSignature + sampleFaceMergeMaterials(blockView, pos, face).hashCode();
        }

        return new GeometryKey(
            delegate.getClass(),
            mat.materialIndex(),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            lightSignature,
            mergeSignature
        );
    }

    private void emitCanonicalFace(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, Direction face) {
        FaceLighting lighting = sampleFaceLighting(blockView, pos, face);
        FaceOcclusion occlusion = sampleFaceOcclusion(blockView, pos, face);
        FaceMergeMaterials mergeMaterials = sampleFaceMergeMaterials(blockView, pos, face);
        emitter.nominalFace(face);
        emitter.cullFace(face);
        // Let the shader own all custom shading so Indigo does not add an extra AO or diffuse pass.
        emitter.diffuseShade(false);
        emitter.ambientOcclusion(TriState.FALSE);
        emitter.shadeMode(ShadeMode.VANILLA);
        emitter.tag(TEXTURALL_TERRAIN_QUAD_TAG);
        writeFacePositions(emitter, face);
        int aoPayload = encodeNibblePayload(
            occlusion.bottomLeft(),
            occlusion.bottomRight(),
            occlusion.topLeft(),
            occlusion.topRight()
        );
        int edgePayload = encodeEdgePayload(mergeMaterials.uMin(), mergeMaterials.uMax(), mergeMaterials.vMin(), mergeMaterials.vMax());
        int payloadColor = encodePayloadColor(mat.materialIndex(), aoPayload, edgePayload);
        emitter.color(0, payloadColor);
        emitter.color(1, payloadColor);
        emitter.color(2, payloadColor);
        emitter.color(3, payloadColor);
        int[] vertexLightmaps = vertexLightmaps(face, lighting, edgePayload);
        emitter.lightmap(vertexLightmaps[0], vertexLightmaps[1], vertexLightmaps[2], vertexLightmaps[3]);
        remapUv(emitter, pos, face);
        emitter.spriteBake(normalSprite(), MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
    }

    private FaceLighting sampleFaceLighting(BlockRenderView blockView, BlockPos pos, Direction face) {
        CornerLight bottomLeft = sampleCornerLight(blockView, pos, face, false, false);
        CornerLight bottomRight = sampleCornerLight(blockView, pos, face, true, false);
        CornerLight topRight = sampleCornerLight(blockView, pos, face, true, true);
        CornerLight topLeft = sampleCornerLight(blockView, pos, face, false, true);
        return new FaceLighting(bottomLeft, bottomRight, topRight, topLeft);
    }

    private static int sampleLight(BlockRenderView blockView, BlockPos.Mutable mutable, LightType lightType, int x, int y, int z) {
        mutable.set(x, y, z);
        return blockView.getLightLevel(lightType, mutable);
    }

    private FaceOcclusion sampleFaceOcclusion(BlockRenderView blockView, BlockPos pos, Direction face) {
        float bottomLeft = sampleCornerOcclusion(blockView, pos, face, false, false);
        float bottomRight = sampleCornerOcclusion(blockView, pos, face, true, false);
        float topRight = sampleCornerOcclusion(blockView, pos, face, true, true);
        float topLeft = sampleCornerOcclusion(blockView, pos, face, false, true);
        return new FaceOcclusion(bottomLeft, bottomRight, topRight, topLeft);
    }

    private FaceMergeMaterials sampleFaceMergeMaterials(BlockRenderView blockView, BlockPos pos, Direction face) {
        TextureEdgeAxes axes = TextureEdgeAxes.forFace(face);
        return new FaceMergeMaterials(
            sampleMergeMaterial(blockView, pos, axes.uMin()),
            sampleMergeMaterial(blockView, pos, axes.uMax()),
            sampleMergeMaterial(blockView, pos, axes.vMin()),
            sampleMergeMaterial(blockView, pos, axes.vMax())
        );
    }

    private int sampleMergeMaterial(BlockRenderView blockView, BlockPos pos, Direction direction) {
        WorldAlignedTextureMaterial neighbor = TexturallTextureOverrides.materialFor(blockView.getBlockState(pos.offset(direction)).getBlock());
        if (neighbor == null || neighbor.materialIndex() == mat.materialIndex()) {
            return 0;
        }
        return neighbor.materialIndex();
    }

    private static int packCornerLight(CornerLight cornerLight) {
        return LightmapTextureManager.pack(cornerLight.block(), cornerLight.sky());
    }

    private static int encodeNibblePayload(float bottomLeft, float bottomRight, float topLeft, float topRight) {
        return quantizeOcclusionNibble(bottomLeft)
            | (quantizeOcclusionNibble(bottomRight) << 4)
            | (quantizeOcclusionNibble(topLeft) << 8)
            | (quantizeOcclusionNibble(topRight) << 12);
    }

    private static int encodePayloadColor(int materialIndex, int blockPayload, int edgePayload) {
        return ((materialIndex & 0xFF) << 24)
            | ((blockPayload & 0xFF) << 16)
            | (((blockPayload >> 8) & 0xFF) << 8)
            | (edgePayload & 0xFF);
    }

    private static int quantizeOcclusionNibble(float ambientOcclusion) {
        return Math.max(0, Math.min(15, Math.round(ambientOcclusion * 15.0F)));
    }

    private static int encodeEdgePayload(int uMin, int uMax, int vMin, int vMax) {
        return (uMin & EDGE_MATERIAL_MASK)
            | ((uMax & EDGE_MATERIAL_MASK) << 6)
            | ((vMin & EDGE_MATERIAL_MASK) << 12)
            | ((vMax & EDGE_MATERIAL_MASK) << 18);
    }

    private static int attachEdgePayload(int lightmap, int edgePayload) {
        return lightmap
            | (((edgePayload >> 8) & 0xFF) << 8)
            | (((edgePayload >> 16) & 0xFF) << 24);
    }

    private static CornerLight sampleCornerLight(BlockRenderView blockView, BlockPos pos, Direction face, boolean uHigh, boolean vHigh) {
        FaceAxes axes = FaceAxes.forFace(face);
        int baseX = pos.getX() + face.getOffsetX();
        int baseY = pos.getY() + face.getOffsetY();
        int baseZ = pos.getZ() + face.getOffsetZ();
        int cornerX = baseX + (uHigh ? axes.u().getOffsetX() : 0) + (vHigh ? axes.v().getOffsetX() : 0);
        int cornerY = baseY + (uHigh ? axes.u().getOffsetY() : 0) + (vHigh ? axes.v().getOffsetY() : 0);
        int cornerZ = baseZ + (uHigh ? axes.u().getOffsetZ() : 0) + (vHigh ? axes.v().getOffsetZ() : 0);
        int uStepX = axes.u().getOffsetX();
        int uStepY = axes.u().getOffsetY();
        int uStepZ = axes.u().getOffsetZ();
        int vStepX = axes.v().getOffsetX();
        int vStepY = axes.v().getOffsetY();
        int vStepZ = axes.v().getOffsetZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int block = averageLight(
            blockView,
            mutable,
            LightType.BLOCK,
            cornerX,
            cornerY,
            cornerZ,
            cornerX - uStepX,
            cornerY - uStepY,
            cornerZ - uStepZ,
            cornerX - vStepX,
            cornerY - vStepY,
            cornerZ - vStepZ,
            cornerX - uStepX - vStepX,
            cornerY - uStepY - vStepY,
            cornerZ - uStepZ - vStepZ
        );
        int sky = averageLight(
            blockView,
            mutable,
            LightType.SKY,
            cornerX,
            cornerY,
            cornerZ,
            cornerX - uStepX,
            cornerY - uStepY,
            cornerZ - uStepZ,
            cornerX - vStepX,
            cornerY - vStepY,
            cornerZ - vStepZ,
            cornerX - uStepX - vStepX,
            cornerY - uStepY - vStepY,
            cornerZ - uStepZ - vStepZ
        );
        return new CornerLight(block, sky);
    }

    private static float sampleCornerOcclusion(BlockRenderView blockView, BlockPos pos, Direction face, boolean uHigh, boolean vHigh) {
        FaceAxes axes = FaceAxes.forFace(face);
        int baseX = pos.getX() + face.getOffsetX();
        int baseY = pos.getY() + face.getOffsetY();
        int baseZ = pos.getZ() + face.getOffsetZ();
        int cornerX = baseX + (uHigh ? axes.u().getOffsetX() : 0) + (vHigh ? axes.v().getOffsetX() : 0);
        int cornerY = baseY + (uHigh ? axes.u().getOffsetY() : 0) + (vHigh ? axes.v().getOffsetY() : 0);
        int cornerZ = baseZ + (uHigh ? axes.u().getOffsetZ() : 0) + (vHigh ? axes.v().getOffsetZ() : 0);
        int uStepX = axes.u().getOffsetX();
        int uStepY = axes.u().getOffsetY();
        int uStepZ = axes.u().getOffsetZ();
        int vStepX = axes.v().getOffsetX();
        int vStepY = axes.v().getOffsetY();
        int vStepZ = axes.v().getOffsetZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        return averageOcclusion(
            blockView,
            mutable,
            cornerX,
            cornerY,
            cornerZ,
            cornerX - uStepX,
            cornerY - uStepY,
            cornerZ - uStepZ,
            cornerX - vStepX,
            cornerY - vStepY,
            cornerZ - vStepZ,
            cornerX - uStepX - vStepX,
            cornerY - uStepY - vStepY,
            cornerZ - uStepZ - vStepZ
        );
    }

    private static int[] vertexLightmaps(Direction face, FaceLighting lighting, int edgePayload) {
        return switch (face) {
            case DOWN, SOUTH, WEST -> new int[] {
                attachEdgePayload(packCornerLight(lighting.bottomLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.bottomRight()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topRight()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topLeft()), edgePayload)
            };
            case UP -> new int[] {
                attachEdgePayload(packCornerLight(lighting.bottomLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topRight()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.bottomRight()), edgePayload)
            };
            case NORTH -> new int[] {
                attachEdgePayload(packCornerLight(lighting.bottomRight()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.bottomLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topRight()), edgePayload)
            };
            case EAST -> new int[] {
                attachEdgePayload(packCornerLight(lighting.bottomRight()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.bottomLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topLeft()), edgePayload),
                attachEdgePayload(packCornerLight(lighting.topRight()), edgePayload)
            };
        };
    }

    private static int averageLight(
        BlockRenderView blockView,
        BlockPos.Mutable mutable,
        LightType lightType,
        int x0,
        int y0,
        int z0,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2,
        int x3,
        int y3,
        int z3
    ) {
        int total = sampleLight(blockView, mutable, lightType, x0, y0, z0)
            + sampleLight(blockView, mutable, lightType, x1, y1, z1)
            + sampleLight(blockView, mutable, lightType, x2, y2, z2)
            + sampleLight(blockView, mutable, lightType, x3, y3, z3);
        return Math.round(total * 0.25F);
    }

    private static float averageOcclusion(
        BlockRenderView blockView,
        BlockPos.Mutable mutable,
        int x0,
        int y0,
        int z0,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2,
        int x3,
        int y3,
        int z3
    ) {
        float total = sampleOcclusion(blockView, mutable, x0, y0, z0)
            + sampleOcclusion(blockView, mutable, x1, y1, z1)
            + sampleOcclusion(blockView, mutable, x2, y2, z2)
            + sampleOcclusion(blockView, mutable, x3, y3, z3);
        return total * 0.25F;
    }

    private static float sampleOcclusion(BlockRenderView blockView, BlockPos.Mutable mutable, int x, int y, int z) {
        mutable.set(x, y, z);
        return blockView.getBlockState(mutable).getAmbientOcclusionLightLevel(blockView, mutable);
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

    private Sprite tileSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(mat.tileSprite());
    }

    private Sprite normalSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(mat.normalSprite());
    }

    private static boolean supportsTerrainShading(BlockRenderView blockView) {
        return !(blockView instanceof EmptyBlockRenderView) && !(blockView instanceof MovingBlockRenderState);
    }

    private record GeometryKey(Class<?> delegateType, int materialIndex, int x, int y, int z, int lightSignature, int mergeSignature) {
    }

    private record CornerLight(int block, int sky) {
    }

    private record FaceLighting(CornerLight bottomLeft, CornerLight bottomRight, CornerLight topRight, CornerLight topLeft) {
    }

    private record FaceOcclusion(float bottomLeft, float bottomRight, float topRight, float topLeft) {
    }

    private record FaceMergeMaterials(int uMin, int uMax, int vMin, int vMax) {
    }

    private record FaceAxes(Direction u, Direction v) {
        private static FaceAxes forFace(Direction face) {
            return switch (face) {
                case UP, DOWN -> new FaceAxes(Direction.EAST, Direction.SOUTH);
                case NORTH, SOUTH -> new FaceAxes(Direction.EAST, Direction.UP);
                case WEST, EAST -> new FaceAxes(Direction.SOUTH, Direction.UP);
            };
        }
    }

    private record TextureEdgeAxes(Direction uMin, Direction uMax, Direction vMin, Direction vMax) {
        private static TextureEdgeAxes forFace(Direction face) {
            return switch (face) {
                case UP -> new TextureEdgeAxes(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH);
                case DOWN -> new TextureEdgeAxes(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH);
                case NORTH -> new TextureEdgeAxes(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
                case SOUTH -> new TextureEdgeAxes(Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
                case EAST -> new TextureEdgeAxes(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
                case WEST -> new TextureEdgeAxes(Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN);
            };
        }
    }
}
