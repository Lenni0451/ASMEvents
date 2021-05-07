package net.lenni0451.asmevents.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;

public class ASMUtils {

    public static byte[] toBytes(final ClassNode node) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static ClassNode fromBytes(final byte[] classBytes) {
        final ClassReader classReader = new ClassReader(classBytes);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    public static Class<?> defineClass(final ClassLoader classLoader, final ClassNode classNode) throws Throwable {
        final Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        final byte[] classBytes = toBytes(classNode);
        return (Class<?>) defineClass.invoke(classLoader, classNode.name.replace("/", "."), classBytes, 0, classBytes.length);
    }

}
