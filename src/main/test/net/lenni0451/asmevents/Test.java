package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
        EventManager.register(new Test());
        EventManager.register(Test.class);
        EventManager.call(new TestEvent1());
        System.out.println();
        EventManager.call(new TestEvent2());
        System.out.println();
    }

    @EventTarget
    public void ab(final TestEvent1 event, final TestEvent2 event2, final TestEvent1 event3) {
        System.out.println(event);
        System.out.println(event2);
        System.out.println(event3);
    }

}
