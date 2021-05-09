package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EnumEventPriority;
import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
        EventManager.register(Test.class);
        EventManager.register(new Test());
        System.out.println(EventManager.EVENT_LISTENER);
        System.out.println(EventManager.EVENT_PIPELINES);
    }

    @EventTarget(priority = EnumEventPriority.HIGH)
    public void ad(final TestEvent2 event, final TestEvent1 event1) {

    }

    @EventTarget
    public static void ab(final TestEvent1 event, final TestEvent2 event2, final TestEvent1 event3) {

    }

}
