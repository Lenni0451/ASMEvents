package net.lenni0451.asmevents.event.types;

import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.enums.EnumEventType;

/**
 * Typed events can be used if you need to call an event at the top and bottom of a method
 */
public interface ITypedEvent extends IEvent {

    EnumEventType getType();

}
