package net.lenni0451.asmevents.internal;

import net.lenni0451.asmevents.IClassLoadProvider;
import net.lenni0451.asmevents.utils.PipelineLoader;

public class PipelineLoaderClassLoadProvider implements IClassLoadProvider {

    private final Class<?> parentClass;

    public PipelineLoaderClassLoadProvider(final Class<?> parentClass) {
        this.parentClass = parentClass;
    }

    @Override
    public <T> Class<T> loadClass(String name, byte[] data) {
        return new PipelineLoader(this.parentClass).loadPipeline(name, data);
    }

}
