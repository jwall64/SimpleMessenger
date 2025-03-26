package dev.jwall64.smi;

import dev.jwall64.mdcfg.registry.Registry;
import dev.jwall64.mdcfg.serializer.SerializeContext;
import dev.jwall64.mdcfg.serializer.SerializeResult;
import dev.jwall64.mdcfg.serializer.Serializer;

public interface SerializableMessenger extends Messenger {

    MessengerType<?> getType();

    static Serializer<SerializableMessenger> createSerializer(Registry<?, MessengerType<?>> types) {

        return new Serializer<SerializableMessenger>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> context, SerializableMessenger value) {
                return serializeGeneric(context, value, value.getType());
            }

            @Override
            public <O> SerializeResult<SerializableMessenger> deserialize(SerializeContext<O> context, O value) {
                O typeEnc = context.get("type", value);
                if(typeEnc == null) return SerializeResult.failure("Unable to deserialize messenger without a type!");

                SerializeResult<MessengerType<?>> typeRes = types.byIdSerializer().deserialize(context, typeEnc);
                if(!typeRes.isComplete()) {
                    return SerializeResult.failure("Unable to deserialize messenger with unknown type " + typeEnc);
                }

                MessengerType<?> type = typeRes.getOrNull();
                return deserializeGeneric(context, value, type).cast(SerializableMessenger.class);
            }

            private <O, T extends Messenger> SerializeResult<O> serializeGeneric(SerializeContext<O> context, SerializableMessenger messenger, MessengerType<T> type) {

                if(!type.getMessengerClass().isAssignableFrom(messenger.getClass())) {
                    return SerializeResult.failure("Wrong type of messenger " + messenger.getClass());
                }
                return type.getSerializer().serialize(context, type.getMessengerClass().cast(messenger));
            }

            private <O, T extends Messenger> SerializeResult<T> deserializeGeneric(SerializeContext<O> context, O value, MessengerType<T> type) {
                return type.getSerializer().deserialize(context, value);
            }
        };

    }

}
