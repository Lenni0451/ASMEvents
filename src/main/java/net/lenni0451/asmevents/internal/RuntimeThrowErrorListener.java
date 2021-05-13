package net.lenni0451.asmevents.internal;

import net.lenni0451.asmevents.IErrorListener;

public class RuntimeThrowErrorListener implements IErrorListener {

    @Override
    public void onException(Throwable t) {
        throw new RuntimeException("Unhandled exception in EventManager thrown", t);
    }

}
