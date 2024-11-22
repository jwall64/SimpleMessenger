package dev.jwall64.smi;

import org.jetbrains.annotations.Nullable;

public interface MessengerReference {

    @Nullable
    Messenger get();

    boolean isValid();

}
