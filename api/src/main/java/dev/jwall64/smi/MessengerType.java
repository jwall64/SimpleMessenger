package dev.jwall64.smi;

import dev.jwall64.mdcfg.serializer.Serializer;

public class MessengerType<T extends Messenger> {

    public final Class<T> messengerClass;
    public final Serializer<T> serializer;

    public MessengerType(Class<T> messengerClass, Serializer<T> serializer) {
        this.messengerClass = messengerClass;
        this.serializer = serializer;
    }
}
