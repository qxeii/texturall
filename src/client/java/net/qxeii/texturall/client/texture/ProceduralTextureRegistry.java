package net.qxeii.texturall.client.texture;

import net.minecraft.resource.Resource;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ProceduralTextureRegistry {
    private static final Map<Identifier, ProceduralTextureGenerator> GENERATORS = new ConcurrentHashMap<>();
    private static final Map<Identifier, byte[]> CACHE = new ConcurrentHashMap<>();

    private ProceduralTextureRegistry() {
    }

    public static void register(Identifier id, ProceduralTextureGenerator generator) {
        GENERATORS.put(id, generator);
        CACHE.remove(id);
    }

    public static boolean has(Identifier id) {
        return GENERATORS.containsKey(id);
    }

    public static byte[] getBytes(Identifier id) {
        ProceduralTextureGenerator generator = GENERATORS.get(id);
        if (generator == null) {
            return null;
        }

        return CACHE.computeIfAbsent(id, ignored -> generator.generatePng());
    }

    public static Optional<Resource> createResource(Identifier id) {
        byte[] bytes = getBytes(id);
        if (bytes == null) {
            return Optional.empty();
        }

        return Optional.of(new Resource(
            GeneratedTextureResourcePack.INSTANCE,
            () -> new ByteArrayInputStream(bytes),
            ResourceMetadata.NONE_SUPPLIER
        ));
    }

    public static Collection<Identifier> findResources(String namespace, String prefix) {
        List<Identifier> matches = new ArrayList<>();
        for (Identifier id : GENERATORS.keySet()) {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                matches.add(id);
            }
        }
        return matches;
    }

    public static Set<String> getNamespaces() {
        Set<String> namespaces = new HashSet<>();
        for (Identifier id : GENERATORS.keySet()) {
            namespaces.add(id.getNamespace());
        }
        return namespaces;
    }
}
