package dev.jwall64.smi;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class MessengerReferenceImpl implements MessengerReference {

    private final WeakReference<Messenger> ref;
    public MessengerReferenceImpl(WeakReference<Messenger> ref) {
        this.ref = ref;
    }

    @Override
    public @Nullable Messenger get() {
        return ref.get();
    }

    @Override
    public boolean isValid() {
        return get() != null;
    }
}
