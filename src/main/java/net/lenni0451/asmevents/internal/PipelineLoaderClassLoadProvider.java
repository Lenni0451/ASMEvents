package net.lenni0451.asmevents.internal;

import net.lenni0451.asmevents.EventManager;
import net.lenni0451.asmevents.IClassLoadProvider;
import net.lenni0451.asmevents.utils.PipelineLoader;

public class PipelineLoaderClassLoadProvider implements IClassLoadProvider {

    @Override
    public <T> Class<T> loadClass(String name, byte[] data) {
        return new PipelineLoader(EventManager.class).loadPipeline(name, data);
    }

}
