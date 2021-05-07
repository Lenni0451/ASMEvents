package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.IEvent;
import net.lenni0451.asmevents.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;

public class EventManager {

    /**
     * Just for testing
     */
    public static void main(String[] args) throws Throwable {
        final String eventManagerPackage = EventManager.class.getName().replace(".", "/");
        final String pipelineName = IEventPipeline.class.getName().replace(".", "/");
        final String eventName = IEvent.class.getName().replace(".", "/");
        final String methodName = IEventPipeline.class.getDeclaredMethods()[0].getName();
        final String methodDescriptor = "(L" + eventName + ";)V";

        ClassNode node = new ClassNode();
        node.name = eventManagerPackage + "a";
        node.access = Opcodes.ACC_PUBLIC;
        node.superName = "java/lang/Object";
        node.interfaces = Collections.singletonList(pipelineName);
        node.version = Opcodes.V1_8;
        {
            MethodNode con = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            con.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            con.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
            con.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(con);
        }
        {
            MethodNode con = new MethodNode(Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null, null);
            con.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            con.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            con.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V"));
            con.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(con);
        }
        byte[] bc = ASMUtils.toBytes(node);

        Unsafe unsafe;
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        Class<?> clazz = unsafe.defineAnonymousClass(EventManager.class, bc, null);
        Object instance = clazz.getDeclaredConstructors()[0].newInstance();

        IEventPipeline pipeline = (IEventPipeline) instance;
        pipeline.call(new IEvent() {
            @Override
            public String toString() {
                return "GAY";
            }
        });
    }

}
