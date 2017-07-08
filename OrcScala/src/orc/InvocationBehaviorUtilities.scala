package orc

import orc.error.runtime.UncallableValueException
import orc.error.runtime.HaltException

/*
trait TypeDirectedInvoker extends Invoker {
  val targetCls: Class[_]
  val argumentClss: Array[Class[_]]
  
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // TODO:PERFORMANCE: This zip/forall combo might be a performance problem.
    targetCls.isInstance(target) && arguments.length == argumentClss.length && (argumentClss zip arguments).forall(p => p._1.isInstance(p._2))
  }
}

trait ValueDirectedTargetTypeDirectedArgumentsInvoker extends Invoker {
  val targetCls: Class[_]
  val argumentClss: Array[Class[_]]
  
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // TODO:PERFORMANCE: This zip/forall combo might be a performance problem.
    targetCls == target && arguments.length == argumentClss.length && (argumentClss zip arguments).forall(p => p._1.isInstance(p._2))
  }
}

abstract class TypeDirectedAccessor(val targetCls: Class[_]) extends Accessor {
  def canGet(target: AnyRef): Boolean = {
    targetCls.isInstance(target)
  }
}
*/

object InvocationBehaviorUtilities {
  /** True iff arguments are all of the same type as the matching class in argumentClss.
    *
    * A null value in argumentClss matches only exactly null in arguments.
    */
  def valuesHaveType(arguments: Array[AnyRef], argumentClss: Array[Class[_]]): Boolean = {
    arguments.length == argumentClss.length && {
      // Conceptually: (argumentClss zip arguments).forall(... Predicate ...)
      // But this is hot path, so.... Sorry.
      var i = 0
      var res = true
      while (i < arguments.length && res) {
        // Predicate here:
        res &&= (argumentClss(i) == null && arguments(i) == null) || argumentClss(i).isInstance(arguments(i))
        i += 1
      }
      res
    }
  }

  /** Get the class of v, or null is v == null.
    */
  def valueType(v: AnyRef): Class[_] = {
    if (v == null)
      null
    else
      v.getClass()
  }
}

abstract class OnlyDirectInvoker extends DirectInvoker {
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    try {
      h.publish(invokeDirect(target, arguments))
    } catch {
      case _: HaltException =>
        h.halt()
    }
  }
}