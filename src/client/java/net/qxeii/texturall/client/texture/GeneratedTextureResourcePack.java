package net.qxeii.texturall.client.texture;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

public final class GeneratedTextureResourcePack implements ResourcePack {
    public static final GeneratedTextureResourcePack INSTANCE = new GeneratedTextureResourcePack();

    private static final ResourcePackInfo INFO = new ResourcePackInfo(
        "texturall:generated",
        Text.literal("Texturall Generated Textures"),
        ResourcePackSource.BUILTIN,
        java.util.Optional.empty()
    );

    private GeneratedTextureResourcePack() {
    }

    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        return null;
    }

    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return null;
        }

        byte[] bytes = ProceduralTextureRegistry.getBytes(id);
        if (bytes == null) {
            return null;
        }

        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return;
        }

        for (Identifier id : ProceduralTextureRegistry.findResources(namespace, prefix)) {
            consumer.accept(id, open(type, id));
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return type == ResourceType.CLIENT_RESOURCES ? ProceduralTextureRegistry.getNamespaces() : Set.of();
    }

    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> metaReader) {
        return null;
    }

    @Override
    public ResourcePackInfo getInfo() {
        return INFO;
    }

    @Override
    public void close() {
    }
}
