package dev.jwall64.smi.mc.api;

import dev.jwall64.mdcfg.registry.Identifier;
import dev.jwall64.mdcfg.registry.Registry;
import dev.jwall64.smi.MessengerType;

public interface MessengerRegistry {

    Registry<Identifier, MessengerType<?>> REGISTRY = Registry.create("smi");

}
