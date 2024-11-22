package dev.jwall64.smi;

import org.jetbrains.annotations.Nullable;

public interface MessengerManager {

    @Nullable
    default Messenger messenger() {
        return getMessenger(null);
    }

    @Nullable
    Messenger messenger(@Nullable String name);


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

    class Holder {
        public static MessengerManager gInstance;
    }

}
