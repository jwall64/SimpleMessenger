package dev.jwall64.smi;

import org.jetbrains.annotations.Nullable;
import dev.jwall64.mdcfg.ConfigSection;
import dev.jwall64.mdcfg.serializer.ConfigContext;
import dev.jwall64.mdcfg.serializer.Serializer;
import dev.jwall64.midnightlib.registry.Identifier;
import dev.jwall64.midnightlib.registry.Registry;

import java.util.Collections;
import java.util.Map;

public class MessengerManagerImpl implements MessengerManager {

    private final Serializer<SerializableMessenger> serializer;

    private Map<String, SerializableMessenger> messengers;

    public MessengerManagerImpl(Registry<Identifier, MessengerType<?>> registry) {
        this.serializer = SerializableMessenger.createSerializer(registry.freeze());
        this.messengers = Collections.emptyMap();
    }

    public void loadAll(ConfigSection config) {
        messengers.values().forEach(Messenger::close);
        messengers = serializer.mapOf().deserialize(ConfigContext.INSTANCE, config).getOrThrow();
    }

    public ConfigSection saveAll() {
        return serializer.mapOf().serialize(ConfigContext.INSTANCE, messengers).getOrThrow().asSection();
    }

    public void clear() {
        messengers.values().forEach(Messenger::close);
        messengers = Collections.emptyMap();
    }

    @Override
    public @Nullable Messenger messenger(@Nullable String name) {
        if(name == null) {
            name = "default";
        }
        return messengers.get(name);
    }

    @Override
    public MessengerReference createReference(Messenger messenger) {
        return null;
    }

    public static void register(MessengerManagerImpl impl) {
        Holder.gInstance = impl;
    }
}
