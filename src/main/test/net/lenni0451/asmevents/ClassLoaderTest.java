package net.lenni0451.asmevents;

import net.lenni0451.asmevents.event.PipelineSafety;
import net.lenni0451.asmevents.event.wrapper.StoppableEvent;
import net.lenni0451.asmevents.internal.IWrappedCaller;
import net.lenni0451.asmevents.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Base64;

public class ClassLoaderTest {

    public static void main(String[] args) throws Throwable {
        Class<?> c = cool();
        Object ob = c.newInstance();
        IWrappedCaller obb = EventManager.wrap(ob, ob.getClass().getDeclaredMethod("listen", CoolEvent.class), CoolEvent.class);
        IWrappedCaller obb2 = EventManager.wrap(ob.getClass(), ob.getClass().getDeclaredMethod("listenStatic", CoolEvent.class), CoolEvent.class);
        obb.call(new CoolEvent());
        obb2.call(new CoolEvent());
        EventManager.register(ob);
        EventManager.register(ob.getClass());
        EventManager.call(new CoolEvent());
    }

    public static Class<?> cool() {
        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/ListenerTest", null, "java/lang/Object", null);
        {
            MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
            init.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(init);
        }
        {
            MethodNode listener = new MethodNode(Opcodes.ACC_PUBLIC, "listen", "(L" + CoolEvent.class.getName().replace(".", "/") + ";)V", null, null);
            listener.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            listener.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;"));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;"));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
            listener.instructions.add(new InsnNode(Opcodes.RETURN));
            listener.visibleAnnotations = new ArrayList<>();
            listener.visibleAnnotations.add(new AnnotationNode("Lnet/lenni0451/asmevents/event/EventTarget;"));
            node.methods.add(listener);
        }
        {
            MethodNode listener = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "listenStatic", "(L" + CoolEvent.class.getName().replace(".", "/") + ";)V", null, null);
            listener.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            listener.instructions.add(new InsnNode(Opcodes.DUP));
            listener.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;"));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;"));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
            listener.instructions.add(new LdcInsnNode("Static call"));
            listener.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
            listener.instructions.add(new InsnNode(Opcodes.RETURN));
            listener.visibleAnnotations = new ArrayList<>();
            listener.visibleAnnotations.add(new AnnotationNode("Lnet/lenni0451/asmevents/event/EventTarget;"));
            node.methods.add(listener);
        }
        byte[] data = ASMUtils.toBytes(node);
        CoolClassLoader ccl = new CoolClassLoader();
        return ccl.findClass(Base64.getEncoder().encodeToString(data));
    }


    static class CoolClassLoader extends ClassLoader {

        @Override
        public Class<?> findClass(String name) {
            byte[] bb = Base64.getDecoder().decode(name);
            return defineClass("test.ListenerTest", bb, 0, bb.length);
        }

    }

    @PipelineSafety
    public static class CoolEvent extends StoppableEvent {
    }

}
