package dev.jwall64.smi.mc.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import dev.jwall64.smi.Messenger;
import dev.jwall64.smi.MessengerManager;

public class Init implements ModInitializer {

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

            Messenger messenger = MessengerManager.getMessenger();

            if(messenger == null) {
                throw new IllegalStateException("Messenger is null");
            }
        });

    }
}
