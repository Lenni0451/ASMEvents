package net.lenni0451.asmevents.event.wrapper;

import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.enums.EnumEventType;
import net.lenni0451.asmevents.event.types.ITypedEvent;

public class TypedEvent implements IEvent, ITypedEvent {

    private final EnumEventType type;

    public TypedEvent(final EnumEventType type) {
        this.type = type;
    }

    @Override
    public EnumEventType getType() {
        return this.type;
    }

}
