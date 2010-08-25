package orc.lib.builtin

import orc.ast.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

object IfT extends PartialSite with UntypedSite {
  override def name = "IfT"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(b : java.lang.Boolean) => 
        if (b.booleanValue) { Some(Signal) } else { None }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object IfF extends PartialSite with UntypedSite {
  override def name = "IfF"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(b : java.lang.Boolean) => 
        if (b.booleanValue) { None } else { Some(Signal) }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object Eq extends TotalSite with UntypedSite {
  override def name = "Eq"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(null, b) => new java.lang.Boolean(b == null)
      case List(a,b) => new java.lang.Boolean(a equals b)
      case _ => throw new ArityMismatchException(2, args.size)
  }
}

object Let extends TotalSite with UntypedSite {
  override def name = "let"
  def evaluate(args: List[AnyRef]) = 
    args match {
      case Nil => Signal
      case (v : AnyRef) :: Nil => v
      case (vs : List[_]) => OrcTuple(vs)
    }
}