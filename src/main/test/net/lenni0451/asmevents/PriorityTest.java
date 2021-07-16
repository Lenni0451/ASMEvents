package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.enums.EnumEventPriority;

public class PriorityTest {

    public static void main(String[] args) {
        EventManager.register(PriorityTest.class);
        EventManager.register(new PriorityTest());

        EventManager.call(new TestEvent1());
    }

    @EventTarget(priority = EnumEventPriority.LOWER)
    public static void lastCall(TestEvent1 event) {
        System.out.println("Static call (First register/last call) " + event);
    }

    @EventTarget
    public static void firstCall(TestEvent1 event) {
        System.out.println("Non static call (Last register/first call) " + event);
    }

}
