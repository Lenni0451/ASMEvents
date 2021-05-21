package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EnumPipelineSafety;
import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.PipelineSafety;

@PipelineSafety(EnumPipelineSafety.ERROR_LISTENER)
public class TestEvent1 implements IEvent {
}
