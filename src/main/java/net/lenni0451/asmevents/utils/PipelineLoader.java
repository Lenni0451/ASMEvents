package net.lenni0451.asmevents.utils;

public class PipelineLoader extends ClassLoader {

    public PipelineLoader(final Class<?> callerClass) {
        super(callerClass.getClassLoader());
    }

    public <C> Class<C> loadPipeline(final String name, final byte[] bytes) {
        return (Class<C>) this.defineClass(name, bytes, 0, bytes.length);
    }

}
