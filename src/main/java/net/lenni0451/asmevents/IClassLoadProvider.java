package net.lenni0451.asmevents;

public interface IClassLoadProvider {

    <T> Class<T> loadClass(final String name, final byte[] data);

}
