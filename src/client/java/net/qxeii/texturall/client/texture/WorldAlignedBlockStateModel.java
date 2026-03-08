package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
        List<BlockModelPart> originalParts = new ArrayList<>();
        delegate.addParts(random, originalParts);
        Sprite sprite = tileSprite();
        for (BlockModelPart part : originalParts) {
            parts.add(retexturePart(part, sprite));
        }
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
        int blendSignature = 1;
        for (Direction face : Direction.values()) {
            lightSignature = 31 * lightSignature + sampleFaceLighting(blockView, pos, face).hashCode();
            blendSignature = 31 * blendSignature + sampleBlendNeighborPayload(blockView, pos, face);
        }

        return new GeometryKey(
            delegate.getClass(),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            lightSignature,
            blendSignature
        );
    }

    private void emitCanonicalFace(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, Direction face) {
        FaceLighting lighting = sampleFaceLighting(blockView, pos, face);
        emitter.nominalFace(face);
        emitter.cullFace(face);
        // Let the shader own all custom shading so Indigo does not add an extra AO or diffuse pass.
        emitter.diffuseShade(false);
        emitter.ambientOcclusion(TriState.FALSE);
        emitter.shadeMode(ShadeMode.VANILLA);
        writeFacePositions(emitter, face);
        int neighborPayload = sampleBlendNeighborPayload(blockView, pos, face);
        int blockPayload = encodeNibblePayload(
            lighting.bottomLeft().block(),
            lighting.bottomRight().block(),
            lighting.topLeft().block(),
            lighting.topRight().block()
        );
        int payloadColor = encodePayloadColor(mat.materialIndex(), neighborPayload);
        emitter.color(0, payloadColor);
        emitter.color(1, payloadColor);
        emitter.color(2, payloadColor);
        emitter.color(3, payloadColor);
        int[] vertexLightmaps = vertexLightmaps(face, lighting);
        emitter.lightmap(
            packCustomLightmap(vertexLightmaps[0], blockPayload),
            packCustomLightmap(vertexLightmaps[1], blockPayload),
            packCustomLightmap(vertexLightmaps[2], blockPayload),
            packCustomLightmap(vertexLightmaps[3], blockPayload)
        );
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

    private static int sampleBlockLight(BlockRenderView blockView, BlockPos.Mutable mutable, int x, int y, int z) {
        return sampleLight(blockView, mutable, LightType.BLOCK, x, y, z);
    }

    private static int sampleLight(BlockRenderView blockView, BlockPos.Mutable mutable, LightType lightType, int x, int y, int z) {
        mutable.set(x, y, z);
        return blockView.getLightLevel(lightType, mutable);
    }

    private static int encodeNibblePayload(int bottomLeft, int bottomRight, int topLeft, int topRight) {
        return (clampLightNibble(bottomLeft))
            | (clampLightNibble(bottomRight) << 4)
            | (clampLightNibble(topLeft) << 8)
            | (clampLightNibble(topRight) << 12);
    }

    private static int encodePayloadColor(int materialIndex, int neighborPayload) {
        return ((materialIndex & 0xFF) << 24)
            | ((neighborPayload & 0xFF) << 16)
            | (((neighborPayload >> 8) & 0xFF) << 8)
            | ((neighborPayload >> 16) & 0xFF);
    }

    private static int packCornerLight(CornerLight cornerLight) {
        return LightmapTextureManager.pack(cornerLight.block(), cornerLight.sky());
    }

    private static int clampLightNibble(int lightLevel) {
        return Math.max(0, Math.min(15, lightLevel));
    }

    private static int packCustomLightmap(int packedLightmap, int blockPayload) {
        int lightX = packedLightmap & 0xFFFF;
        int lightY = (packedLightmap >>> 16) & 0xFFFF;
        int payloadLow = blockPayload & 0xFF;
        int payloadHigh = (blockPayload >>> 8) & 0xFF;
        return (lightX | (payloadLow << 8)) | ((lightY | (payloadHigh << 8)) << 16);
    }

    private int sampleBlendNeighborPayload(BlockRenderView blockView, BlockPos pos, Direction face) {
        FaceAxes axes = FaceAxes.forFace(face);
        int uLow = sampleBlendNeighborMaterial(blockView, pos, face, axes.u().getOpposite());
        int uHigh = sampleBlendNeighborMaterial(blockView, pos, face, axes.u());
        int vLow = sampleBlendNeighborMaterial(blockView, pos, face, axes.v().getOpposite());
        int vHigh = sampleBlendNeighborMaterial(blockView, pos, face, axes.v());
        return encodeNeighborPayload(uLow, uHigh, vLow, vHigh);
    }

    private int sampleBlendNeighborMaterial(BlockRenderView blockView, BlockPos pos, Direction face, Direction edgeDirection) {
        BlockPos neighborPos = pos.offset(edgeDirection);
        BlockState neighborState = blockView.getBlockState(neighborPos);
        WorldAlignedTextureMaterial neighborMaterial = TexturallTextureOverrides.materialFor(neighborState.getBlock());
        if (neighborMaterial == null || neighborMaterial.materialIndex() == mat.materialIndex()) {
            return 0;
        }

        BlockState faceNeighborState = blockView.getBlockState(neighborPos.offset(face));
        if (!Block.shouldDrawSide(neighborState, faceNeighborState, face)) {
            return 0;
        }

        return clampNeighborMaterial(neighborMaterial.materialIndex());
    }

    private static int encodeNeighborPayload(int uLow, int uHigh, int vLow, int vHigh) {
        return clampNeighborMaterial(uLow)
            | (clampNeighborMaterial(uHigh) << 6)
            | (clampNeighborMaterial(vLow) << 12)
            | (clampNeighborMaterial(vHigh) << 18);
    }

    private static int clampNeighborMaterial(int materialIndex) {
        return Math.max(0, Math.min(63, materialIndex));
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

    private static int[] vertexLightmaps(Direction face, FaceLighting lighting) {
        return switch (face) {
            case DOWN, SOUTH, WEST -> new int[] {
                packCornerLight(lighting.bottomLeft()),
                packCornerLight(lighting.bottomRight()),
                packCornerLight(lighting.topRight()),
                packCornerLight(lighting.topLeft())
            };
            case UP -> new int[] {
                packCornerLight(lighting.bottomLeft()),
                packCornerLight(lighting.topLeft()),
                packCornerLight(lighting.topRight()),
                packCornerLight(lighting.bottomRight())
            };
            case NORTH -> new int[] {
                packCornerLight(lighting.bottomRight()),
                packCornerLight(lighting.bottomLeft()),
                packCornerLight(lighting.topLeft()),
                packCornerLight(lighting.topRight())
            };
            case EAST -> new int[] {
                packCornerLight(lighting.bottomRight()),
                packCornerLight(lighting.bottomLeft()),
                packCornerLight(lighting.topLeft()),
                packCornerLight(lighting.topRight())
            };
        };
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

    private static BlockModelPart retexturePart(BlockModelPart part, Sprite sprite) {
        return new RetexturedBlockModelPart(
            copyQuads(part.getQuads(Direction.DOWN), sprite),
            copyQuads(part.getQuads(Direction.UP), sprite),
            copyQuads(part.getQuads(Direction.NORTH), sprite),
            copyQuads(part.getQuads(Direction.SOUTH), sprite),
            copyQuads(part.getQuads(Direction.WEST), sprite),
            copyQuads(part.getQuads(Direction.EAST), sprite),
            copyQuads(part.getQuads(null), sprite),
            part.useAmbientOcclusion(),
            sprite
        );
    }

    private static List<BakedQuad> copyQuads(List<BakedQuad> quads, Sprite sprite) {
        List<BakedQuad> copied = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            copied.add(new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.tintIndex(),
                quad.face(),
                sprite,
                false,
                quad.lightEmission()
            ));
        }
        return copied;
    }

    private Sprite tileSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(mat.tileSprite());
    }

    private Sprite normalSprite() {
        return MinecraftClient.getInstance().getAtlasManager().getSprite(mat.normalSprite());
    }

    private record GeometryKey(Class<?> delegateType, int x, int y, int z, int lightSignature, int blendSignature) {
    }

    private record CornerLight(int block, int sky) {
    }

    private record FaceLighting(CornerLight bottomLeft, CornerLight bottomRight, CornerLight topRight, CornerLight topLeft) {
    }

    private record RetexturedBlockModelPart(
        List<BakedQuad> down,
        List<BakedQuad> up,
        List<BakedQuad> north,
        List<BakedQuad> south,
        List<BakedQuad> west,
        List<BakedQuad> east,
        List<BakedQuad> unculled,
        boolean useAmbientOcclusion,
        Sprite particleSprite
    ) implements BlockModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction face) {
            if (face == null) {
                return unculled;
            }
            return switch (face) {
                case DOWN -> down;
                case UP -> up;
                case NORTH -> north;
                case SOUTH -> south;
                case WEST -> west;
                case EAST -> east;
            };
        }

        @Override
        public boolean useAmbientOcclusion() {
            return useAmbientOcclusion;
        }

        @Override
        public Sprite particleSprite() {
            return particleSprite;
        }
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
}
