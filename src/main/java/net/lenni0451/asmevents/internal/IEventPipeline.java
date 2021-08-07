package net.lenni0451.asmevents.internal;

import net.lenni0451.asmevents.IErrorListener;
import net.lenni0451.asmevents.event.IEvent;

import java.util.List;

public interface IEventPipeline {

    void call(IEvent event);
    void setFields(IErrorListener errorListener, List callers);

}
