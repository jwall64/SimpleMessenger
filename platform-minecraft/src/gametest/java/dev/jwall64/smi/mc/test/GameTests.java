package dev.jwall64.smi.mc.test;

import java.lang.reflect.Method;

import org.jspecify.annotations.NonNull;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;

public class GameTests implements CustomTestMethodInvoker {

    @Override
    public void invokeTestMethod(GameTestHelper helper, @NonNull Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }

}
