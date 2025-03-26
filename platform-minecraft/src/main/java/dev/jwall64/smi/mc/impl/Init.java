package dev.jwall64.smi.mc.impl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jwall64.mdcfg.ConfigObject;
import dev.jwall64.mdcfg.ConfigSection;
import dev.jwall64.mdcfg.codec.FileWrapper;
import dev.jwall64.mdcfg.mc.api.ServerConfigFolders;
import dev.jwall64.mdcfg.serializer.ConfigContext;
import dev.jwall64.smi.AmqpMessenger;
import dev.jwall64.smi.MessengerManager;
import dev.jwall64.smi.MessengerManagerImpl;
import dev.jwall64.smi.mc.api.MessengerRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Init implements ModInitializer {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("messengers", new ConfigSection());
    private static final Logger log = LoggerFactory.getLogger("SimpleMessenger");

    private MessengerManagerImpl messengerManager;

    @Override
    public void onInitialize() {
        AmqpMessenger.register(MessengerRegistry.REGISTRY);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            messengerManager = new MessengerManagerImpl(MessengerRegistry.REGISTRY);
            MessengerManager.Holder.gInstance = messengerManager;

            Path configFolder = ServerConfigFolders.getConfigFolder(server).resolve("SimpleMessenger");
            try { Files.createDirectories(configFolder); } catch (IOException ex) { log.error("Unable to create config directory!", ex); }

            FileWrapper<ConfigObject> config = ServerConfigFolders.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
            config.load();

            try {
                messengerManager.loadAll(config.getRoot().asSection().getSection("messengers"));
            } catch (Exception ex) {
                log.error("Unable to load all messengers!", ex);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if(MessengerManager.Holder.gInstance == messengerManager) {
                messengerManager.clear();
                MessengerManager.Holder.gInstance = null;
            }
        });

    }
}
