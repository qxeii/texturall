package net.qxeii.texturall.mixin.client;

import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.qxeii.texturall.client.texture.ProceduralTextureRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(NamespaceResourceManager.class)
public abstract class NamespaceResourceManagerMixin {
    @Shadow @Final private ResourceType type;
    @Shadow @Final private String namespace;

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void texturall$overrideResource(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (!shouldHandle(id)) {
            return;
        }

        Optional<Resource> generated = ProceduralTextureRegistry.createResource(id);
        if (generated.isPresent()) {
            cir.setReturnValue(generated);
        }
    }

    @Inject(method = "getAllResources", at = @At("RETURN"), cancellable = true)
    private void texturall$appendGeneratedResource(Identifier id, CallbackInfoReturnable<List<Resource>> cir) {
        if (!shouldHandle(id)) {
            return;
        }

        Optional<Resource> generated = ProceduralTextureRegistry.createResource(id);
        if (generated.isEmpty()) {
            return;
        }

        List<Resource> resources = new ArrayList<>(cir.getReturnValue());
        resources.add(generated.get());
        cir.setReturnValue(resources);
    }

    @Inject(method = "findResources", at = @At("RETURN"), cancellable = true)
    private void texturall$addGeneratedResources(
        String startingPath,
        Predicate<Identifier> allowedPathPredicate,
        CallbackInfoReturnable<Map<Identifier, Resource>> cir
    ) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return;
        }

        Map<Identifier, Resource> merged = new LinkedHashMap<>(cir.getReturnValue());
        boolean changed = false;
        for (Identifier id : ProceduralTextureRegistry.findResources(namespace, startingPath)) {
            if (!allowedPathPredicate.test(id)) {
                continue;
            }

            Optional<Resource> generated = ProceduralTextureRegistry.createResource(id);
            if (generated.isPresent()) {
                merged.put(id, generated.get());
                changed = true;
            }
        }

        if (changed) {
            cir.setReturnValue(merged);
        }
    }

    private boolean shouldHandle(Identifier id) {
        return type == ResourceType.CLIENT_RESOURCES
            && namespace.equals(id.getNamespace())
            && ProceduralTextureRegistry.has(id);
    }
}
