package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.utils.Tuple;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {

    private static final Map<Class<? extends IEvent>, Map<Class<?>, List<Tuple<Object, Method>>>> EVENT_LISTENER = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IEvent>, IEventPipeline> EVENT_PIPELINES = new ConcurrentHashMap<>();

    /**
     * Register all events in the class<br>
     * If the listener is a class only static events are registered<br>
     *
     * @param listener The instance or class of the listener
     */
    public static void register(final Object listener) {
        register(null, listener);
    }

    /**
     * Only register a single event type<br>
     * If the eventClass is null all events are registered<br>
     * If the listener is a class only static events are registered<br>
     *
     * @param eventClass The event you want to register or null to register all events
     * @param listener   The instance or class of the listener
     */
    public static void register(final Class<? extends IEvent> eventClass, final Object listener) {
        Objects.requireNonNull(listener);
        final Class<?> listenerClass = (listener instanceof Class<?> ? (Class<?>) listener : listener.getClass());
        final Object listenerInstance = (listener instanceof Class<?> ? null : listener);

        for (Method method : listenerClass.getDeclaredMethods()) {
            EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
            if (eventTarget == null) continue;
            boolean onlyHasEvents = true;
            for (Class<?> type : method.getParameterTypes()) {
                if (!IEvent.class.isAssignableFrom(type)) {
                    onlyHasEvents = false;
                    break;
                }
            }
            if (!onlyHasEvents) return;
            for (Class<?> type : method.getParameterTypes()) {
                if (eventClass != null && !eventClass.equals(type)) continue;

                //Cast is not unchecked, believe me
                register((Class<? extends IEvent>) type, listenerClass, listenerInstance, method);
            }
        }
    }

    private static void register(final Class<? extends IEvent> eventClass, final Class<?> listenerClass, final Object instance, final Method method) {
        Objects.requireNonNull(eventClass);
        Objects.requireNonNull(listenerClass);
        Objects.requireNonNull(method);

        if (instance == null && !Modifier.isStatic(method.getModifiers())) return;
        if (instance != null && Modifier.isStatic(method.getModifiers())) return;

        final Map<Class<?>, List<Tuple<Object, Method>>> listenerClassToMethods = EVENT_LISTENER.computeIfAbsent(eventClass, c -> new HashMap<>());
        final List<Tuple<Object, Method>> methods = listenerClassToMethods.computeIfAbsent(listenerClass, c -> new CopyOnWriteArrayList<>());

        methods.add(new Tuple<>(instance, method));
    }


    private static void unregister(final Object listener) {
        unregister(null, listener);
    }

    private static void unregister(final Class<? extends IEvent> eventClass, final Object listener) {
        Objects.requireNonNull(listener);

    }

    private static void unregister(final Class<? extends IEvent> eventClass, final Class<?> listenerClass, final Object instance, final Method method) {
        Objects.requireNonNull(eventClass);
        Objects.requireNonNull(listenerClass);
        Objects.requireNonNull(method);

        if (instance == null && !Modifier.isStatic(method.getModifiers())) return;
        if (instance != null && Modifier.isStatic(method.getModifiers())) return;


    }

}
