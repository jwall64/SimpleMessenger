package dev.jwall64.smi;


import java.util.function.Consumer;

public interface Messenger {

    void publish(Message msg);

    void publish(Message msg, long ttl);

    void subscribe(String channel, Object listener, Consumer<Message> handler);

    void unsubscribe(String channel, Object listener);

    void unsubscribeAll(String channel);

    void close();

    boolean isClosed();


}
