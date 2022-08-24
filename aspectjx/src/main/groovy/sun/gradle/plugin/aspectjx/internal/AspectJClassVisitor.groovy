package sun.gradle.plugin.aspectjx.internal

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-02-01
 */

class AspectJClassVisitor extends ClassVisitor {

    boolean isAspectClass = false

    AspectJClassVisitor(int opcodes, ClassWriter classWriter) {
        super(opcodes, classWriter)
    }

    @Override
    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        isAspectClass |= (desc == 'Lorg/aspectj/lang/annotation/Aspect;')

        return super.visitAnnotation(desc, visible)
    }
}