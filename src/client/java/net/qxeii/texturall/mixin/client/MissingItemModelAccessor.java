package net.qxeii.texturall.mixin.client;

import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(MissingItemModel.class)
public interface MissingItemModelAccessor {
    @Accessor("quads")
    List<BakedQuad> texturall$getQuads();

    @Accessor("settings")
    ModelSettings texturall$getSettings();
}
