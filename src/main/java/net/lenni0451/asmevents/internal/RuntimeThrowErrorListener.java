package net.lenni0451.asmevents.internal;

public class RuntimeThrowErrorListener implements IErrorListener {

    @Override
    public void onException(Throwable t) {
        throw new RuntimeException("Unhandled exception in EventManager thrown", t);
    }

}
