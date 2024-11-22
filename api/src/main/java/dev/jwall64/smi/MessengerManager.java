package dev.jwall64.smi;

import org.jetbrains.annotations.Nullable;

public interface MessengerManager {

    @Nullable
    default Messenger messenger() {
        return getMessenger(null);
    }

    @Nullable
    Messenger messenger(@Nullable String name);

    MessengerReference createReference(Messenger messenger);

    @Nullable
    static Messenger getMessenger() {
        if(Holder.gInstance == null) return null;
        return Holder.gInstance.messenger();
    }

    @Nullable
    static Messenger getMessenger(String name) {
        if(Holder.gInstance == null) return null;
        return Holder.gInstance.messenger(name);
    }

    @Nullable
    static MessengerReference getMessengerReference() {
        Messenger m = getMessenger();
        if(m == null) return null;
        return Holder.gInstance.createReference(m);
    }

    @Nullable
    static MessengerReference getMessengerReference(String name) {
        Messenger m = getMessenger(name);
        if(m == null) return null;
        return Holder.gInstance.createReference(m);
    }

    class Holder {
        public static MessengerManager gInstance;
    }

}
