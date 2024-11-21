package dev.jwall64.smi;

import dev.jwall64.midnightlib.registry.Identifier;

import java.nio.ByteBuffer;

public class Message {

    public final Identifier channel;
    public final ByteBuffer payload;

    public Message(Identifier channel, ByteBuffer payload) {
        this.channel = channel;
        this.payload = payload;
    }
}
