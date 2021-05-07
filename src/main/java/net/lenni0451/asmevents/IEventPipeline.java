package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.IEvent;

public interface IEventPipeline {

    void call(IEvent event);

}
