package net.lenni0451.asmevents.event.wrapper;

import net.lenni0451.asmevents.event.types.ICancellableEvent;

public class CancellableEvent implements ICancellableEvent {

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
