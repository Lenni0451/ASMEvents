package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.IEvent;

import java.text.DecimalFormat;
import java.util.HashMap;

public class CallSpeedTest {

    public static void main(String[] args) throws Throwable {
        EventManager.setErrorListener(Throwable::printStackTrace);
        DecimalFormat df = new DecimalFormat();

        System.out.println("---------- Register Event Listener ----------");
        long registerTime;
        {
            registerTime = System.nanoTime();
            EventManager.register(new EventListener());
            System.out.println("EventManager: " + df.format(System.nanoTime() - registerTime));
        }
        System.out.println();

        long start;
        float middle;
        System.out.println("---------- Single call ----------");
        {
            start = System.nanoTime();
            EventManager.call(new ExampleEvent1());
            System.out.println("EventManager (ExampleEvent1): " + df.format(System.nanoTime() - start));

            start = System.nanoTime();
            EventManager.call(new ExampleEvent2());
            System.out.println("EventManager (ExampleEvent2): " + df.format(System.nanoTime() - start));
        }
        System.out.println();

        System.out.println("---------- 1000 call ----------");
        {
            middle = 0;
            for (int i = 0; i < 1000; i++) {
                start = System.nanoTime();
                EventManager.call(new ExampleEvent1());
                middle += System.nanoTime() - start;
            }
            middle /= 1000F;
            System.out.println("EventManager (ExampleEvent1): " + df.format(middle));

            middle = 0;
            for (int i = 0; i < 1000; i++) {
                start = System.nanoTime();
                EventManager.call(new ExampleEvent2());
                middle += System.nanoTime() - start;
            }
            middle /= 1000F;
            System.out.println("EventManager (ExampleEvent2): " + df.format(middle));
        }
        System.out.println();

        System.out.println("---------- 100000 call ----------");
        {
            middle = 0;
            for (int i = 0; i < 100000; i++) {
                start = System.nanoTime();
                EventManager.call(new ExampleEvent1());
                middle += System.nanoTime() - start;
            }
            middle /= 100000F;
            System.out.println("EventManager (ExampleEvent1): " + df.format(middle));

            middle = 0;
            for (int i = 0; i < 100000; i++) {
                start = System.nanoTime();
                EventManager.call(new ExampleEvent2());
                middle += System.nanoTime() - start;
            }
            middle /= 100000F;
            System.out.println("EventManager (ExampleEvent2): " + df.format(middle));
        }
    }

    public static class ExampleEvent1 implements IEvent {
    }

    public static class ExampleEvent2 implements IEvent {
    }

    public static class EventListener {

        @EventTarget
        public void onEvent(ExampleEvent1 event) {
            CallSpeedTest.testCodeHere(event);
        }

        @EventTarget
        public void onEvent(ExampleEvent2 event) {
            CallSpeedTest.testCodeHere(event);
        }

    }

    public static void testCodeHere(IEvent event) {
        if (Math.abs(-1) == 1) {
            new HashMap<>().put("dfg", "dfgfdg");
        }
    }

}
