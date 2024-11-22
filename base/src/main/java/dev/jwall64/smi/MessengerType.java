package dev.jwall64.smi;

import dev.jwall64.mdcfg.serializer.Serializer;

public interface MessengerType<T extends Messenger> {

    Class<T> getMessengerClass();
    Serializer<T> getSerializer();

}
