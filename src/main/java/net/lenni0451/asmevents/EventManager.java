package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.PipelineSafety;
import net.lenni0451.asmevents.event.enums.EnumEventType;
import net.lenni0451.asmevents.event.types.ICancellableEvent;
import net.lenni0451.asmevents.event.types.IStoppableEvent;
import net.lenni0451.asmevents.event.types.ITypedEvent;
import net.lenni0451.asmevents.internal.IEventPipeline;
import net.lenni0451.asmevents.internal.PipelineLoaderClassLoadProvider;
import net.lenni0451.asmevents.internal.RuntimeThrowErrorListener;
import net.lenni0451.asmevents.utils.ASMUtils;
import net.lenni0451.asmevents.utils.ReflectUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {

    private static final Map<Class<? extends IEvent>, Map<Object, List<Method>>> EVENT_LISTENER = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IEvent>, IEventPipeline> EVENT_PIPELINES = new ConcurrentHashMap<>();
    private static IErrorListener ERROR_LISTENER = new RuntimeThrowErrorListener();
    private static IClassLoadProvider CLASS_LOAD_PROVIDER = new PipelineLoaderClassLoadProvider();

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
        final Set<Class<? extends IEvent>> updatedEvents = new HashSet<>();

        for (Method method : listenerClass.getDeclaredMethods()) {
            EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
            if (eventTarget == null) continue;
            for (Class<?> type : method.getParameterTypes()) {
                if (eventClass != null && !eventClass.equals(type)) continue;
                if (!IEvent.class.isAssignableFrom(type)) continue;

                //Cast is not unchecked, believe me
                updatedEvents.add((Class<? extends IEvent>) type);
                register((Class<? extends IEvent>) type, listener, method);
            }
            for (Class<? extends IEvent> type : eventTarget.noParamEvents()) {
                if (eventClass != null && !eventClass.equals(type)) continue;

                updatedEvents.add(type);
                register(type, listener, method);
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

        if (!methods.contains(method)) methods.add(method);
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

    /**
     * Unregister a specific event from a class or listener instance<br>
     * If the listener is a class all static events get unregistered<br>
     * If the listener is an instance all non static events get unregistered
     *
     * @param eventClass The class of the event to unregister
     * @param listener   The class or instance of the listener
     */
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


    /**
     * Call an event and pass it into the pipeline
     *
     * @param event The event to call
     * @return The same event you already passed
     */
    public static <T extends IEvent> T call(final T event) {
        Objects.requireNonNull(event);

        try {
            IEventPipeline pipeline = EVENT_PIPELINES.get(event.getClass());
            if (pipeline != null) pipeline.call(event);
        } catch (Throwable t) {
            ERROR_LISTENER.onException(t);
        }
        try {
            IEventPipeline pipeline = EVENT_PIPELINES.get(IEvent.class);
            if (pipeline != null) pipeline.call(event);
        } catch (Throwable t) {
            ERROR_LISTENER.onException(t);
        }

        return event;
    }


    /**
     * Internal method to recalculate a list of event pipelines
     *
     * @param eventTypes The list of events to recalculate
     */
    private static void updatePipelines(final Collection<Class<? extends IEvent>> eventTypes) {
        for (Class<? extends IEvent> eventType : eventTypes) updatePipeline(eventType);
    }

    /**
     * Internal method to recalculate a event pipeline
     *
     * @param eventType The event to recalculate
     */
    private static void updatePipeline(final Class<? extends IEvent> eventType) {
        final Map<Object, List<Method>> listenerMethods = EVENT_LISTENER.get(eventType);
        if (listenerMethods == null) return;

        final PipelineSafety pipelineSafety = eventType.getDeclaredAnnotation(PipelineSafety.class);
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
                return o2target.priority().compareTo(o1target.priority());
            });
            allListener = new ArrayList<>(listenerMethods.keySet());
            allListener.removeIf(o -> o instanceof Class<?>); //Remove all static listener classes as we do not need an instance of them
        }

        ClassNode pipelineNode = new ClassNode();
        pipelineNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "net/lenni0451/asmevents/" + eventType.getSimpleName() + "Pipeline", null, "java/lang/Object", new String[]{IEventPipeline.class.getName().replace(".", "/")});
        pipelineNode.sourceFile = eventType.getName() + " Pipeline"; //This shows when an exception is printed. Some nice to have debug details
        pipelineNode.sourceDebug = "ASMEvents by Lenni0451"; //Some credits for me :)
        ASMUtils.addDefaultConstructor(pipelineNode);

        for (int i = 0; i < allListener.size(); i++) { //Add a field for all listener instances we need
            final Object listener = allListener.get(i);

            pipelineNode.visitField(Opcodes.ACC_PUBLIC, "listener" + i, Type.getDescriptor(listener.getClass()), null, null);
        }
        { //Insert call method and all listener calls
            MethodVisitor visitor = pipelineNode.visitMethod(Opcodes.ACC_PUBLIC, ReflectUtils.getMethodByArgs(IEventPipeline.class, IEvent.class).getName(), "(" + Type.getDescriptor(IEvent.class) + ")V", null, new String[]{"java/lang/Throwable"});
            { //Cast an IEvent implementation to the actual event class and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, eventType.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 2);
            }
            if (ICancellableEvent.class.isAssignableFrom(eventType)) { //Cast an IEvent implementation to a ICancellableEvent if it can be cancelled and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, ICancellableEvent.class.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 3);
            }
            if (ITypedEvent.class.isAssignableFrom(eventType)) { //Cast an IEvent implementation to a ITypedEvent if it is typed and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, ITypedEvent.class.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 4);
            }
            for (Method method : allMethods) {
                final EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
                Label jumpAfter = null;
                Label endBlock = null;
                Label catchBlock = null;
                if (pipelineSafety != null) {
                    final Label tryBlock = new Label();
                    endBlock = new Label();
                    catchBlock = new Label();

                    visitor.visitTryCatchBlock(tryBlock, endBlock, catchBlock, "java/lang/Throwable");
                    visitor.visitLabel(tryBlock);
                }

                if (IStoppableEvent.class.isAssignableFrom(eventType)) { //Check if the stoppable event is stopped and return if so
                    final Label skipReturn = new Label();

                    visitor.visitVarInsn(Opcodes.ALOAD, 3);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ICancellableEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ICancellableEvent.class).getName(), "()Z", true);
                    visitor.visitJumpInsn(Opcodes.IFEQ, skipReturn);
                    visitor.visitInsn(Opcodes.RETURN);
                    visitor.visitLabel(skipReturn);
                } else if (ICancellableEvent.class.isAssignableFrom(eventType) && eventTarget.skipCancelled()) { //Check if a cancellable event is cancelled and we do not want to listen for it
                    if (jumpAfter == null) jumpAfter = new Label();

                    visitor.visitVarInsn(Opcodes.ALOAD, 3);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ICancellableEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ICancellableEvent.class).getName(), "()Z", true);
                    visitor.visitJumpInsn(Opcodes.IFNE, jumpAfter);
                }
                if (ITypedEvent.class.isAssignableFrom(eventType) && !eventTarget.type().equals(EnumEventType.ALL)) { //Check if the type of a typed event is the wanted type
                    if (jumpAfter == null) jumpAfter = new Label();

                    visitor.visitVarInsn(Opcodes.ALOAD, 4);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITypedEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ITypedEvent.class).getName(), "()" + Type.getDescriptor(EnumEventType.class), true);
                    visitor.visitFieldInsn(Opcodes.GETSTATIC, EnumEventType.class.getName().replace(".", "/"), ReflectUtils.getEnumField(eventTarget.type()).getName(), Type.getDescriptor(EnumEventType.class));
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "equals", "(Ljava/lang/Object;)Z", false);
                    visitor.visitJumpInsn(Opcodes.IFEQ, jumpAfter);
                }
                {
                    if (!Modifier.isStatic(method.getModifiers())) { //Load the listener instance if the listener method is not static
                        final Object listener = methodToInstance.get(method);
                        visitor.visitVarInsn(Opcodes.ALOAD, 0);
                        visitor.visitFieldInsn(Opcodes.GETFIELD, pipelineNode.name, "listener" + allListener.indexOf(listener), Type.getDescriptor(listener.getClass()));
                    }
                    for (Class<?> param : method.getParameterTypes()) { //Load all method paramenter or load null if it is not the current event
                        if (param.equals(eventType)) visitor.visitVarInsn(Opcodes.ALOAD, 2);
                        else ASMUtils.generateNullValue(visitor, param);
                    }
                    //And finally actually call the listener method
                    if (Modifier.isStatic(method.getModifiers())) {
                        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, method.getDeclaringClass().getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), false);
                    } else {
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, method.getDeclaringClass().getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), false);
                    }
                }
                if (pipelineSafety != null) {
                    if (jumpAfter == null) jumpAfter = new Label();
                    visitor.visitLabel(endBlock);
                    visitor.visitJumpInsn(Opcodes.GOTO, jumpAfter);
                    visitor.visitLabel(catchBlock);
                    if (pipelineSafety.print()) {
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
                    } else {
                        visitor.visitInsn(Opcodes.POP);
                    }
                }
                if (jumpAfter != null) visitor.visitLabel(jumpAfter);
            }
            visitor.visitInsn(Opcodes.RETURN);
            visitor.visitEnd();
        }

        try {
            //Load the pipeline class
            Class<? extends IEventPipeline> pipelineClass = CLASS_LOAD_PROVIDER.loadClass(pipelineNode.name.replace("/", "."), ASMUtils.toBytes(pipelineNode));

            //Create an instance of the loaded pipeline class
            IEventPipeline pipeline = (IEventPipeline) pipelineClass.getDeclaredConstructors()[0].newInstance();
            { //Use reflection to set all listener instance fields
                Field[] fields = pipeline.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    field.setAccessible(true);
                    field.set(pipeline, allListener.get(i));
                }
            }
            EVENT_PIPELINES.put(eventType, pipeline);
        } catch (Throwable t) {
            ERROR_LISTENER.onException(t);
        }
    }


    /**
     * Set the handler of unhandled exceptions<br>
     * By default all exceptions are thrown as RuntimeExceptions<br>
     * You may want to just print them to prevent the program from crashing
     *
     * @param errorListener The listener
     */
    public static void setErrorListener(final IErrorListener errorListener) {
        Objects.requireNonNull(errorListener);

        ERROR_LISTENER = errorListener;
    }

    /**
     * The the provider for loading the internal pipeline classes<br>
     * By default all pipeline classes are loaded using a custom class loader
     *
     * @param classLoadProvider The provider
     */
    public static void setClassLoadProvider(final IClassLoadProvider classLoadProvider) {
        Objects.requireNonNull(classLoadProvider);

        CLASS_LOAD_PROVIDER = classLoadProvider;
    }

}
