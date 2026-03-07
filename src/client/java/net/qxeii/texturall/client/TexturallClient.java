package net.qxeii.texturall.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.qxeii.texturall.client.texture.TexturallModelLoadingPlugin;
import net.qxeii.texturall.client.texture.TexturallTextureOverrides;

public class TexturallClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TexturallTextureOverrides.bootstrap();
        TexturallModelLoadingPlugin.register();

        WorldRenderEvents.START_MAIN.register(context ->
            SunDirectionUniform.upload(context.worldState().skyRenderState.sunAngle)
        );
    }
}
