package net.lenni0451.asmevents.utils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class ASMUtils {

    public static byte[] toBytes(final ClassNode node) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static void addDefaultConstructor(final ClassNode node) {
        MethodVisitor visitor = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        visitor.visitInsn(Opcodes.RETURN);
        visitor.visitEnd();
    }

    public static void generateNullValue(final MethodVisitor visitor, final Class<?> type) {
        if (boolean.class.equals(type) || byte.class.equals(type) || short.class.equals(type) || char.class.equals(type) || int.class.equals(type)) {
            visitor.visitInsn(Opcodes.ICONST_0);
        } else if (long.class.equals(type)) {
            visitor.visitInsn(Opcodes.LCONST_0);
        } else if (float.class.equals(type)) {
            visitor.visitInsn(Opcodes.FCONST_0);
        } else if (double.class.equals(type)) {
            visitor.visitInsn(Opcodes.DCONST_0);
        } else {
            visitor.visitInsn(Opcodes.ACONST_NULL);
        }
    }

}
