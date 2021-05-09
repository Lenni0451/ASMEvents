package net.lenni0451.asmevents.event.types;

import net.lenni0451.asmevents.event.IEvent;

public interface ICancellableEvent extends IEvent {

    boolean isCancelled();

    void setCancelled(final boolean cancelled);

}
