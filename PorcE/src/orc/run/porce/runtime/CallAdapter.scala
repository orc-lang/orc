package orc.run.porce.runtime

import orc.values.sites.OrcJavaCompatibility.{ Invocable, InvocableCtor, InvocableMethod }

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import org.objectweb.asm.{ ClassVisitor, ClassWriter }
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.Type
import org.objectweb.asm.commons.{ GeneratorAdapter, Method }

object CallAdapter {
  import scala.language.implicitConversions
  implicit def classToType(cls: Class[_]) = Type.getType(cls)

  private class CodeAdapter(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      exceptions: Array[String],
      classVisitor: ClassVisitor) extends GeneratorAdapter(
        ASM6,
        classVisitor.visitMethod(
          access,
          name,
          descriptor,
          signature,
          exceptions),
        access, name, descriptor) {

    private def typeIsReference(t: Type) = t.getSort == Type.OBJECT || t.getSort == Type.ARRAY

    override def cast(from: Type, to: Type): Unit = {
      if (!typeIsReference(from) && !typeIsReference(to)) {
        // Delegate to GeneratorAdapter
        super.cast(from, to)
      } else if (typeIsReference(from) && typeIsReference(to)) {
        checkCast(to)
      } else if (typeIsReference(from)) {
        unbox(to)
      } else if (typeIsReference(to)) {
        valueOf(from)
      } else {
        throw new AssertionError(s"$from, $to")
      }
    }

  }

  private object ClassLoader extends java.lang.ClassLoader {
    def defineClass(name: String, b: Array[Byte]): Class[_] =
      defineClass(null, b, 0, b.length)
  }

  private object InternalName {
    val CallAdapter = Type.getInternalName(classOf[CallAdapter])
  }

  private object Descriptor {
    val invokeMethod = Type.getType(classOf[CallAdapter].getMethod(
      "invoke", classOf[Object], classOf[Array[Object]]))
  }

  private var uniqueNumber = 0

  private def construct(invocable: Invocable) = synchronized {
    val name = {
      val methName = invocable match {
        case InvocableMethod(_) => invocable.name
        case InvocableCtor(_) => "init"
      }
      uniqueNumber += 1
      s"asm/orc/run/porce/runtime/$$CallAdapter$$${invocable.declaringClass.getSimpleName}_${methName}_$uniqueNumber"
    }

    val invokeExactParameters = if (invocable.isStatic)
      invocable.parameterTypes
    else
      invocable.declaringClass +: invocable.parameterTypes
    val invokeExactMethod = new Method("invokeExact", invocable.returnType,
      invokeExactParameters.map(Type.getType))

    val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)

    cw.visit(V1_8, ACC_SUPER | ACC_SYNTHETIC | ACC_PUBLIC, name, null,
      Type.getInternalName(classOf[CallAdapter]), Array())

    //    cw.visitInnerClass(
    //      "com/oracle/truffle/api/CompilerDirectives$TruffleBoundary",
    //      "com/oracle/truffle/api/CompilerDirectives",
    //      "TruffleBoundary",
    //      ACC_PUBLIC + ACC_STATIC + ACC_ANNOTATION + ACC_ABSTRACT +
    //        ACC_INTERFACE)

    {
      val mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        InternalName.CallAdapter,
        "<init>",
        "()V",
        false)
      mv.visitInsn(RETURN)
      mv.visitMaxs(0, 0)
      mv.visitEnd()
    }
    {
      val mv = new CodeAdapter(
        ACC_PUBLIC,
        "invoke", Descriptor.invokeMethod.getDescriptor,
        null, null, cw)
      mv.visitParameter("target", 0)
      mv.visitParameter("arguments", 0)

      mv.visitCode()

      if (!invocable.isStatic) {
        mv.loadArg(0)
        mv.checkCast(invocable.declaringClass)
      }

      if (invocable.parameterTypes.nonEmpty) {
        for ((paramCls, i) <- invocable.parameterTypes.view.zipWithIndex) {
          mv.loadArg(1)
          mv.push(i)
          mv.arrayLoad(classOf[Object])
          mv.cast(classOf[Object], paramCls)
        }
      }

      mv.invokeStatic(Type.getObjectType(name), invokeExactMethod)
      mv.cast(invocable.returnType, classOf[Object])
      mv.visitInsn(ARETURN)
      mv.visitMaxs(0, 0)
      mv.visitEnd()
    }

    {
      val mv = new CodeAdapter(
        ACC_PRIVATE + ACC_STATIC,
        invokeExactMethod.getName,
        invokeExactMethod.getDescriptor,
        null, null, cw)

      if (!invocable.isStatic) {
        mv.visitParameter("$CallAdapter$target", 0)
      }
      for (param <- invocable.executableMember.getParameters) {
        mv.visitParameter(param.getName, 0)
      }

      {
        val av0 = mv.visitAnnotation(Type.getType(classOf[TruffleBoundary]).getDescriptor, true)
        av0.visit("allowInlining", true)
        av0.visitEnd()
      }

      mv.visitCode()

      invocable match {
        case InvocableCtor(c) =>
          val targetCls = Type.getType(invocable.declaringClass)
          val targetMethod = Method.getMethod(c)
          mv.newInstance(targetCls)
          mv.dup()
          mv.loadArgs()
          mv.invokeConstructor(targetCls, targetMethod)
        case InvocableMethod(m) =>
          val targetCls = Type.getType(invocable.declaringClass)
          val targetMethod = Method.getMethod(m)
          mv.loadArgs()
          if (invocable.isStatic) {
            mv.invokeStatic(targetCls, targetMethod)
          } else if (m.getDeclaringClass.isInterface()) {
            mv.invokeInterface(targetCls, targetMethod)
          } else { // m is a normal method on a class
            mv.invokeVirtual(targetCls, targetMethod)
          }
      }

      mv.returnValue()
      mv.visitMaxs(0, 0)
      mv.visitEnd()
    }
    cw.visitEnd()
    (name, cw.toByteArray())
  }

  private val invocableCallAdapters = collection.mutable.HashMap[Invocable, Class[_ <: CallAdapter]]()

  def adapterClass(invocable: Invocable): Class[_ <: CallAdapter] = synchronized {
    invocableCallAdapters.getOrElseUpdate(invocable, {
      val (name, data) = construct(invocable)
      val cls = ClassLoader.defineClass(name, data).asInstanceOf[Class[_ <: CallAdapter]]
      try {
        // Create an instance to force linkage errors.
        cls.newInstance()
      } catch {
        case e: LinkageError => {
          import java.nio.file.{ Files, Paths }
          Files.write(Paths.get(s"${cls.getSimpleName}.class"), data)
          throw e
        }
      }
      cls
    })
  }

  def apply(invocable: Invocable): CallAdapter = {
    adapterClass(invocable).newInstance()
  }

  def main(args: Array[String]): Unit = {
    def make(i: Invocable): CallAdapter = {
      //import java.nio.file.{ Files, Paths }
      //val cls = adapterClass(i)
      //val (name, data) = CallAdapter.construct(i)
      //Files.write(Paths.get(s"${cls.getSimpleName}.class"), data)
      CallAdapter(i)
    }

    {
      val inst = make(Invocable(classOf[java.lang.Double].getMethod("doubleValue")))
      println(inst.invoke((0.2).asInstanceOf[AnyRef], Array()))
    }

    {
      val inst = make(Invocable(classOf[java.lang.Double].getMethod("valueOf", classOf[Double])))
      println(inst.invoke("test", Array((0.2).asInstanceOf[AnyRef])))
    }

    {
      val inst = make(Invocable(classOf[String].getConstructor(classOf[Array[Char]])))
      println(inst.invoke("test", Array(Array('a', 'b'))))
    }
  }
}

abstract class CallAdapter {
  def invoke(target: AnyRef, arguments: Array[AnyRef]): AnyRef
}
