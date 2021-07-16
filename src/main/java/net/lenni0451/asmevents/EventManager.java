package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.EnumPipelineSafety;
import net.lenni0451.asmevents.event.EventTarget;
import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.event.PipelineSafety;
import net.lenni0451.asmevents.event.enums.EnumEventType;
import net.lenni0451.asmevents.event.types.ICancellableEvent;
import net.lenni0451.asmevents.event.types.IStoppableEvent;
import net.lenni0451.asmevents.event.types.ITypedEvent;
import net.lenni0451.asmevents.internal.IEventPipeline;
import net.lenni0451.asmevents.internal.IWrappedCaller;
import net.lenni0451.asmevents.internal.RuntimeThrowErrorListener;
import net.lenni0451.asmevents.utils.ASMUtils;
import net.lenni0451.asmevents.utils.ClassDefiner;
import net.lenni0451.asmevents.utils.ReflectUtils;
import net.lenni0451.asmevents.utils.Tuple;
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

public class EventManager {

    private static final Map<Class<? extends IEvent>, Map<Object, Map<Method, IWrappedCaller>>> EVENT_LISTENER = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IEvent>, IEventPipeline> EVENT_PIPELINES = new ConcurrentHashMap<>();
    private static IErrorListener ERROR_LISTENER = new RuntimeThrowErrorListener();

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

        final Map<Object, Map<Method, IWrappedCaller>> listenerClassToMethods = EVENT_LISTENER.computeIfAbsent(eventClass, c -> new HashMap<>());
        final Map<Method, IWrappedCaller> methods = listenerClassToMethods.computeIfAbsent(listener, c -> new ConcurrentHashMap<>());

        if (!methods.containsKey(method)) methods.put(method, wrap(listener, method, eventClass));
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

        for (Map.Entry<Class<? extends IEvent>, Map<Object, Map<Method, IWrappedCaller>>> entry : EVENT_LISTENER.entrySet()) {
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
        final Map<Object, Map<Method, IWrappedCaller>> listenerMethods = EVENT_LISTENER.get(eventType);
        if (listenerMethods == null) return;

        final PipelineSafety pipelineSafety = eventType.getDeclaredAnnotation(PipelineSafety.class);
        final List<Tuple<Method, IWrappedCaller>> allMethods = new ArrayList<>();

        { //Prepare list of all methods and map to map them back to the instance
            for (Map.Entry<Object, Map<Method, IWrappedCaller>> entry : listenerMethods.entrySet()) {
                for (Map.Entry<Method, IWrappedCaller> methods : entry.getValue().entrySet()) {
                    allMethods.add(new Tuple<>(methods.getKey(), methods.getValue()));
                }
            }
            allMethods.sort((o1, o2) -> { //Sort all methods by priority
                EventTarget o1target = o1.getA().getDeclaredAnnotation(EventTarget.class);
                EventTarget o2target = o2.getA().getDeclaredAnnotation(EventTarget.class);
                return o2target.priority().compareTo(o1target.priority());
            });
        }

        ClassNode pipelineNode = new ClassNode();
        pipelineNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "net/lenni0451/asmevents/" + eventType.getSimpleName() + "Pipeline" + System.nanoTime(), null, "java/lang/Object", new String[]{IEventPipeline.class.getName().replace(".", "/")});
        pipelineNode.sourceFile = eventType.getName() + " Pipeline"; //This shows when an exception is printed. Some nice to have debug details
        pipelineNode.sourceDebug = "ASMEvents by Lenni0451"; //Some credits for me :)
        ASMUtils.addDefaultConstructor(pipelineNode);

        if (pipelineSafety != null && pipelineSafety.value().equals(EnumPipelineSafety.ERROR_LISTENER)) { //Add the errorListener field if needed
            pipelineNode.visitField(Opcodes.ACC_PUBLIC, "errorListener", Type.getDescriptor(IErrorListener.class), null, null);
        }
        for (int i = 0; i < allMethods.size(); i++) {
            pipelineNode.visitField(Opcodes.ACC_PUBLIC, "listener" + i, Type.getDescriptor(IWrappedCaller.class), null, null);
        }
        { //Insert call method and all listener calls
            MethodVisitor visitor = pipelineNode.visitMethod(Opcodes.ACC_PUBLIC, ReflectUtils.getMethodByArgs(IEventPipeline.class, IEvent.class).getName(), "(" + Type.getDescriptor(IEvent.class) + ")V", null, new String[]{"java/lang/Throwable"});
            if (ICancellableEvent.class.isAssignableFrom(eventType)) { //Cast an IEvent implementation to a ICancellableEvent if it can be cancelled and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, ICancellableEvent.class.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 2);
            }
            if (ITypedEvent.class.isAssignableFrom(eventType)) { //Cast an IEvent implementation to a ITypedEvent if it is typed and store it
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitTypeInsn(Opcodes.CHECKCAST, ITypedEvent.class.getName().replace(".", "/"));
                visitor.visitVarInsn(Opcodes.ASTORE, 3);
            }
            int wrapperIndex = 0;
            for (Tuple<Method, IWrappedCaller> method : allMethods) {
                final EventTarget eventTarget = method.getA().getDeclaredAnnotation(EventTarget.class);
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

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ICancellableEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ICancellableEvent.class).getName(), "()Z", true);
                    visitor.visitJumpInsn(Opcodes.IFEQ, skipReturn);
                    visitor.visitInsn(Opcodes.RETURN);
                    visitor.visitLabel(skipReturn);
                } else if (ICancellableEvent.class.isAssignableFrom(eventType) && eventTarget.skipCancelled()) { //Check if a cancellable event is cancelled and we do not want to listen for it
                    if (jumpAfter == null) jumpAfter = new Label();

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ICancellableEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ICancellableEvent.class).getName(), "()Z", true);
                    visitor.visitJumpInsn(Opcodes.IFNE, jumpAfter);
                }
                if (ITypedEvent.class.isAssignableFrom(eventType) && !eventTarget.type().equals(EnumEventType.ALL)) { //Check if the type of a typed event is the wanted type
                    if (jumpAfter == null) jumpAfter = new Label();

                    visitor.visitVarInsn(Opcodes.ALOAD, 3);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITypedEvent.class.getName().replace(".", "/"), ReflectUtils.getMethodByArgs(ITypedEvent.class).getName(), "()" + Type.getDescriptor(EnumEventType.class), true);
                    visitor.visitFieldInsn(Opcodes.GETSTATIC, EnumEventType.class.getName().replace(".", "/"), ReflectUtils.getEnumField(eventTarget.type()).getName(), Type.getDescriptor(EnumEventType.class));
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "equals", "(Ljava/lang/Object;)Z", false);
                    visitor.visitJumpInsn(Opcodes.IFEQ, jumpAfter);
                }
                {
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitFieldInsn(Opcodes.GETFIELD, pipelineNode.name, "listener" + wrapperIndex++, Type.getDescriptor(IWrappedCaller.class));
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    //And finally actually call the listener method
                    Method m = ReflectUtils.getMethodByArgs(IWrappedCaller.class, IEvent.class);
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, IWrappedCaller.class.getName().replace(".", "/"), m.getName(), Type.getMethodDescriptor(m), true);
                }
                if (pipelineSafety != null) {
                    if (jumpAfter == null) jumpAfter = new Label();
                    visitor.visitLabel(endBlock);
                    visitor.visitJumpInsn(Opcodes.GOTO, jumpAfter);
                    visitor.visitLabel(catchBlock);
                    switch (pipelineSafety.value()) {
                        case PRINT: //Print the exception
                            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
                            break;
                        case ERROR_LISTENER: //Call the error listener
                            visitor.visitVarInsn(Opcodes.ALOAD, 0);
                            visitor.visitFieldInsn(Opcodes.GETFIELD, pipelineNode.name, "errorListener", Type.getDescriptor(IErrorListener.class));
                            visitor.visitInsn(Opcodes.SWAP);
                            final Method onExceptionMethod = ReflectUtils.getMethodByArgs(IErrorListener.class, Throwable.class);
                            visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, IErrorListener.class.getName().replace(".", "/"), onExceptionMethod.getName(), Type.getMethodDescriptor(onExceptionMethod), true);
                            break;
                        case IGNORE: //Pop the exception of the stack
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
            Class<? extends IEventPipeline> pipelineClass = ClassDefiner.define(EventManager.class, pipelineNode.name.replace("/", "."), ASMUtils.toBytes(pipelineNode));

            //Create an instance of the loaded pipeline class
            IEventPipeline pipeline = (IEventPipeline) pipelineClass.getDeclaredConstructors()[0].newInstance();
            { //Use reflection to set all listener instance fields and error listener if needed
                Field[] fields = pipeline.getClass().getDeclaredFields();
                if (fields.length > 0 && fields[0].getType().equals(IErrorListener.class)) {
                    Field field = fields[0];
                    field.setAccessible(true);
                    field.set(pipeline, ERROR_LISTENER);
                    fields = Arrays.copyOfRange(fields, 1, fields.length); //Cut the error listener field
                }
                for (int i = 0; i < allMethods.size(); i++) {
                    Field field = fields[i];
                    field.setAccessible(true);
                    field.set(pipeline, allMethods.get(i).getB());
                }
            }
            EVENT_PIPELINES.put(eventType, pipeline);
        } catch (Throwable t) {
            ERROR_LISTENER.onException(t);
        }
    }

    /**
     * Generate a call wrapper using the class loader of the listener<br>
     * This fixes ClassLoader problems because the event listener is always executed from a class loaded with the same loader
     *
     * @param listener The listener instance or class if static
     */
    private static IWrappedCaller wrap(final Object listener, final Method method, final Class<? extends IEvent> eventType) {
        final boolean isStatic = listener instanceof Class;
        final Class<?> listenerClass = isStatic ? (Class<?>) listener : listener.getClass();

        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "net/lenni0451/asmevents/Wrapper" + System.nanoTime(), null, "java/lang/Object", new String[]{IWrappedCaller.class.getName().replace(".", "/")});
        if (!isStatic) {
            node.visitField(Opcodes.ACC_PUBLIC, "listener", Type.getDescriptor(listenerClass), null, null);
        }
        { //Default constructor
            MethodVisitor mv = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
        }
        {
            MethodVisitor mv = node.visitMethod(Opcodes.ACC_PUBLIC, ReflectUtils.getMethodByArgs(IWrappedCaller.class, IEvent.class).getName(), "(" + Type.getDescriptor(IEvent.class) + ")V", null, null);
            if (!isStatic) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, node.name, "listener", Type.getDescriptor(listenerClass));
            }
            for (Class<?> param : method.getParameterTypes()) { //Load all method parameter or load null if it is not the current event
                if (param.equals(eventType)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, eventType.getName().replace(".", "/"));
                } else if (param.equals(IEvent.class)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                } else {
                    ASMUtils.generateNullValue(mv, param);
                }
            }
            if (isStatic) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, listenerClass.getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), false);
            } else {
                mv.visitMethodInsn(method.getDeclaringClass().isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, listenerClass.getName().replace(".", "/"), method.getName(), Type.getMethodDescriptor(method), method.getDeclaringClass().isInterface());
            }
            mv.visitInsn(Opcodes.RETURN);
        }
        try {
            Object ob = ClassDefiner.define(listenerClass, node.name.replace("/", "."), ASMUtils.toBytes(node)).newInstance();
            if (!isStatic) ob.getClass().getDeclaredFields()[0].set(ob, listener);
            return (IWrappedCaller) ob;
        } catch (Throwable t) {
            ERROR_LISTENER.onException(t);
        }
        return null;
    }


    /**
     * Set the handler of unhandled exceptions<br>
     * By default all exceptions are thrown as RuntimeExceptions<br>
     * You may want to just print them to prevent the program from crashing<br>
     * <br>
     * If an event has the {@link PipelineSafety} annotation and uses {@link EnumPipelineSafety#ERROR_LISTENER} the pipeline of the event has to be rebuilt to update the error listener
     *
     * @param errorListener The listener
     */
    public static void setErrorListener(final IErrorListener errorListener) {
        Objects.requireNonNull(errorListener);

        ERROR_LISTENER = errorListener;
    }

}
