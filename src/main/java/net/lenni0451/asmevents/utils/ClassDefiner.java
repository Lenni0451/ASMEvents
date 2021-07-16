package net.lenni0451.asmevents.utils;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ClassDefiner {

    private static final Map<ClassLoader, ClassDefinerLoader> loaders = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> Class<T> define(final Class<?> parent, final String name, final byte[] data) {
        return define(parent.getClassLoader(), name, data);
    }

    public static <T> Class<T> define(final ClassLoader parentLoader, final String name, final byte[] data) {
        ClassDefinerLoader loader = loaders.computeIfAbsent(parentLoader, ClassDefinerLoader::new);
        synchronized (loader.getClassLoadingLock(name)) {
            if (loader.hasClass(name)) throw new IllegalStateException(name + " already defined");
            return (Class<T>) loader.define(name, data);
        }
    }

    private static class ClassDefinerLoader extends ClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }


        protected ClassDefinerLoader(final ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(final String name, final byte[] data) {
            synchronized (this.getClassLoadingLock(name)) {
                Class<?> c = this.defineClass(name, data, 0, data.length);
                this.resolveClass(c);
                return c;
            }
        }

        @Override
        public Object getClassLoadingLock(final String name) {
            return super.getClassLoadingLock(name);
        }

        public boolean hasClass(final String name) {
            synchronized (this.getClassLoadingLock(name)) {
                try {
                    Class.forName(name);
                    return true;
                } catch (ClassNotFoundException ignored) {
                }
                return false;
            }
        }

    }

}
 