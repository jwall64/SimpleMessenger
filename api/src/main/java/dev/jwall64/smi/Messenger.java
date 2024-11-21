package dev.jwall64.smi;

import dev.jwall64.mdcfg.serializer.SerializeContext;
import dev.jwall64.mdcfg.serializer.SerializeResult;
import dev.jwall64.mdcfg.serializer.Serializer;
import dev.jwall64.midnightlib.event.EventHandler;
import dev.jwall64.midnightlib.registry.Identifier;
import dev.jwall64.midnightlib.registry.Registry;

public interface Messenger {

    MessengerType<?> getType();

    void publish(Message msg);

    void publish(Message msg, long ttl);

    void subscribe(Identifier channel, Object listener, EventHandler<Message> handler);

    void close();

    static Serializer<Messenger> createSerializer(Registry<?, MessengerType<?>> types) {

        return new Serializer<Messenger>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> context, Messenger value) {
                return serializeGeneric(context, value, value.getType());
            }

            @Override
            public <O> SerializeResult<Messenger> deserialize(SerializeContext<O> context, O value) {
                O typeEnc = context.get("type", value);
                if(typeEnc == null) return SerializeResult.failure("Unable to deserialize messenger without a type!");

                SerializeResult<MessengerType<?>> typeRes = types.byIdSerializer().deserialize(context, typeEnc);
                if(!typeRes.isComplete()) {
                    return SerializeResult.failure("Unable to deserialize messenger with unknown type " + typeEnc);
                }

                MessengerType<?> type = typeRes.getOrNull();
                return deserializeGeneric(context, value, type).cast(Messenger.class);
            }

            private <O, T extends Messenger> SerializeResult<O> serializeGeneric(SerializeContext<O> context, Messenger messenger, MessengerType<T> type) {

                if(!type.messengerClass.isAssignableFrom(messenger.getClass())) {
                    return SerializeResult.failure("Wrong type of messenger " + messenger.getClass());
                }
                return type.serializer.serialize(context, type.messengerClass.cast(messenger));
            }

            private <O, T extends Messenger> SerializeResult<T> deserializeGeneric(SerializeContext<O> context, O value, MessengerType<T> type) {
                return type.serializer.deserialize(context, value);
            }
        };

    }

}
