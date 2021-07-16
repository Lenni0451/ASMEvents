package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
        EventManager.register(Test.class);

        EventManager.call(new TestEvent1());
        EventManager.call(new TestEvent2());

        EventManager.unregister(Test.class);
        System.out.println("No calls");

        EventManager.call(new TestEvent1());
        EventManager.call(new TestEvent2());

        EventManager.register(Test.class);
        EventManager.unregister(TestEvent2.class, Test.class);
        System.out.println("Only one call");

        EventManager.call(new TestEvent1());
        EventManager.call(new TestEvent2());

        EventManager.unregister(Test.class);
        EventManager.register(TestEvent2.class, Test.class);
        System.out.println("Only one call");

        EventManager.call(new TestEvent1());
        EventManager.call(new TestEvent2());
    }

    @EventTarget
    public static void ab(final TestEvent1 event, String s) {
        System.out.println("Call 1 " + event + " " + s);
    }

    @EventTarget
    public static void abd(final TestEvent2 event, int i) {
        System.out.println("Call 2 " + event + " " + i);
    }

    @EventTarget
    public static void nothing() {
        System.out.println("Should not call");
    }

    @EventTarget(noParamEvents = {TestEvent2.class})
    public static void otherEventType(TestEvent1 event) {
        System.out.println("Should call twice " + event);
    }

}
