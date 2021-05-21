package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;

public class Test {

    public static void main(String[] args) {
//        EventManager.setErrorListener(Throwable::printStackTrace);
        EventManager.setErrorListener((e) -> System.out.println("ERROR!"));

        new Thread(() -> {
            EventManager.register(TestEvent1.class, Test.class);
            for (int i = 0; i < 100; i++) {
                EventManager.call(new TestEvent1());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            EventManager.register(TestEvent2.class, Test.class);
            for (int i = 0; i < 200; i++) {
                EventManager.call(new TestEvent2());
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @EventTarget
    public static void ab(final TestEvent1 event) {
        System.out.println("Call 1");
    }

    @EventTarget
    public static void abd(final TestEvent2 event) {
        System.out.println("Call 2");
    }

}
