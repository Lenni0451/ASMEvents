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

    @EventTarget(noParamEvents = {TestEvent1.class, TestEvent2.class})
    public static void ab(final float coolFloat, boolean test, Double dfd) {
        System.out.println(coolFloat + " " + test + " " + dfd);
    }

}
