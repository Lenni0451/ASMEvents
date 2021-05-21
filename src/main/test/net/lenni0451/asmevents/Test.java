package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
//        EventManager.setErrorListener(Throwable::printStackTrace);
        EventManager.setErrorListener((e) -> System.out.println("ERROR!"));

        EventManager.register(Test.class);
        EventManager.call(new TestEvent1());
    }

    @EventTarget
    public static void ab(final TestEvent1 event) {
        System.out.println("Call 1");
        throw new RuntimeException();
    }

    @EventTarget
    public static void abd(final TestEvent1 event) {
        System.out.println("Call 2");
        throw new RuntimeException();
    }

}
