package net.qxeii.texturall.client;

import net.fabricmc.api.ClientModInitializer;
import net.qxeii.texturall.client.texture.TexturallModelLoadingPlugin;
import net.qxeii.texturall.client.texture.TexturallTextureOverrides;

public class TexturallClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TexturallTextureOverrides.bootstrap();
        TexturallModelLoadingPlugin.register();
    }
}
