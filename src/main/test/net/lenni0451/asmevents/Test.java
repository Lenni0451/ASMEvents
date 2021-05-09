package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
        EventManager.register(Test.class);
        EventManager.call(new TestEvent1());
        System.out.println();
        EventManager.call(new TestEvent2());
        System.out.println();
    }

    @EventTarget(noParamEvents = TestEvent2.class)
    public static void ab(final TestEvent1 event, final TestEvent1 event2) {
        System.out.println(event);
    }

}
