package net.lenni0451.asmevents.event.types;

import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.enums.EnumEventType;

public interface ITypedEvent extends IEvent {

    EnumEventType getType();

}
