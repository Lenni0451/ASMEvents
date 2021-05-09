package net.lenni0451.asmevents;

import jdk.internal.org.objectweb.asm.Type;
import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.utils.ASMUtils;
import net.lenni0451.asmevents.utils.PipelineLoader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {

    public static final Map<Class<? extends IEvent>, Map<Object, List<Method>>> EVENT_LISTENER = new ConcurrentHashMap<>();
    public static final Map<Class<? extends IEvent>, IEventPipeline> EVENT_PIPELINES = new ConcurrentHashMap<>();

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
        final List<Class<? extends IEvent>> updatedEvents = new ArrayList<>();

        for (Method method : listenerClass.getDeclaredMethods()) {
            EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
            if (eventTarget == null) continue;
            if (method.getParameterCount() <= 0) continue;
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
                updatedEvents.add((Class<? extends IEvent>) type);
                register((Class<? extends IEvent>) type, listener, method);
            }
        }
        updatePipelines(updatedEvents);
    }

    /**
     * Internal method to register a listener
     *
     * @param eventClass The class of the event to register
     * @param listener   The class for static or the instance for non static listener
     * @param method     The method to execute
     */
    private static void register(final Class<? extends IEvent> eventClass, final Object listener, final Method method) {
        Objects.requireNonNull(eventClass);
        Objects.requireNonNull(listener);
        Objects.requireNonNull(method);

        if (listener instanceof Class<?> && !Modifier.isStatic(method.getModifiers())) return;
        if (!(listener instanceof Class<?>) && Modifier.isStatic(method.getModifiers())) return;

        final Map<Object, List<Method>> listenerClassToMethods = EVENT_LISTENER.computeIfAbsent(eventClass, c -> new HashMap<>());
        final List<Method> methods = listenerClassToMethods.computeIfAbsent(listener, c -> new CopyOnWriteArrayList<>());

        methods.add(method);
    }


    /**
     * Unregister all events from a class or listener instance<br>
     * If the listener is a class all static events get unregistered<br>
     * If the listener is an instance all non static events get unregistered
     *
     * @param listener The class or instance of the listener
     */
    public static void unregister(final Object listener) {
        Objects.requireNonNull(listener);

        for (Map.Entry<Class<? extends IEvent>, Map<Object, List<Method>>> entry : EVENT_LISTENER.entrySet()) {
            if (entry.getValue().containsKey(listener)) unregister(entry.getKey(), listener);
        }
    }

    public static void unregister(final Class<? extends IEvent> eventClass, final Object listener) {
        Objects.requireNonNull(listener);

        EVENT_LISTENER.get(eventClass).remove(listener);
        if (EVENT_LISTENER.get(eventClass).isEmpty()) {
            EVENT_LISTENER.remove(eventClass);
            EVENT_PIPELINES.remove(eventClass);
        } else {
            updatePipeline(eventClass);
        }
    }


    private static void updatePipelines(final List<Class<? extends IEvent>> eventTypes) {
        for (Class<? extends IEvent> eventType : eventTypes) updatePipeline(eventType);
    }

    private static void updatePipeline(final Class<? extends IEvent> eventType) {
        final Map<Object, List<Method>> listenerMethods = EVENT_LISTENER.get(eventType);
        if (listenerMethods == null) return;

        final List<Method> allMethods = new ArrayList<>();
        final Map<Method, Object> methodToInstance = new HashMap<>();
        final List<Object> allListener;

        { //Prepare list of all methods and map to map them back to the instance
            for (Map.Entry<Object, List<Method>> entry : listenerMethods.entrySet()) {
                for (Method method : entry.getValue()) {
                    allMethods.add(method);
                    methodToInstance.put(method, entry.getKey());
                }
            }
            allMethods.sort((o1, o2) -> { //Sort all methods by priority
                EventTarget o1target = o1.getDeclaredAnnotation(EventTarget.class);
                EventTarget o2target = o2.getDeclaredAnnotation(EventTarget.class);
                return o1target.priority().compareTo(o2target.priority());
            });
            allListener = new ArrayList<>(listenerMethods.keySet());
            allListener.removeIf(o -> o instanceof Class<?>);
        }

        ClassNode pipelineNode = new ClassNode();
        pipelineNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "net/lenni0451/asmevents/" + eventType.getSimpleName() + "Pipeline", null, "java/lang/Object", new String[]{IEventPipeline.class.getName().replace(".", "/")});
        pipelineNode.sourceFile = eventType.getName() + " Pipeline"; //This shows when an exception is printed. Some nice to have debug details
        pipelineNode.sourceDebug = "ASMEvents by Lenni0451"; //Some credits for me :)
        ASMUtils.addDefaultConstructor(pipelineNode);

        for (int i = 0; i < allListener.size(); i++) {
            final Object listener = allListener.get(i);

            pipelineNode.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "listener" + i, Type.getDescriptor(listener.getClass()), null, null);
        }
        { //Insert call method and all listener calls
            MethodVisitor visitor = pipelineNode.visitMethod(Opcodes.ACC_PUBLIC, IEventPipeline.class.getDeclaredMethods()[0].getName(), "(" + Type.getDescriptor(IEvent.class) + ")V", null, new String[]{"java/lang/Throwable"});
            { //Cast and IEvent interface to the actual event class and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, eventType.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 2);
            }
            for (Method method : allMethods) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    final Object listener = methodToInstance.get(method);
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitFieldInsn(Opcodes.GETFIELD, pipelineNode.name, "listener" + allListener.indexOf(listener), Type.getDescriptor(listener.getClass()));
                }
                for (Class<?> param : method.getParameterTypes()) {
                    if (param.equals(eventType)) visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    else visitor.visitInsn(Opcodes.ACONST_NULL);
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    visitor.visitMethodInsn(Opcodes.INVOKESTATIC, method.getDeclaringClass().getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), false);
                } else {
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, method.getDeclaringClass().getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), false);
                }
            }
            visitor.visitInsn(Opcodes.RETURN);
            visitor.visitEnd();
        }

        byte[] data = ASMUtils.toBytes(pipelineNode);
        PipelineLoader pipelineLoader = new PipelineLoader(EventManager.class);
        Class<? extends IEventPipeline> pipelineClass = pipelineLoader.loadPipeline(pipelineNode.name.replace("/", "."), data);
    }

}
