package net.lenni0451.asmevents.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ReflectUtils {

    public static Method getMethodByArgs(final Class<?> clazz, final Class<?>... args) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Arrays.equals(method.getParameterTypes(), args)) return method;
        }
        throw new RuntimeException("Unable to find method in " + clazz.getName() + " with arguments " + Arrays.toString(args));
    }

    public static Field getEnumField(final Enum<?> value) {
        for (Field field : value.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.getType().equals(value.getClass())) continue;

            field.setAccessible(true);
            try {
                if (value.equals(field.get(null))) return field;
            } catch (Throwable ignored) {
            }
        }
        throw new RuntimeException("Unable to find enum field for " + value.getClass().getName() + " " + value);
    }

}
