package dev.jwall64.smi;

import java.nio.ByteBuffer;

public class Message {

    public final String channel;
    public final ByteBuffer payload;

    public Message(String channel, ByteBuffer payload) {
        this.channel = channel;
        this.payload = payload;
    }
}
