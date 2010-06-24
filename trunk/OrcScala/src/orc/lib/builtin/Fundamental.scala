package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

object IfT extends PartialSite with UntypedSite {
  override def name = "IfT"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Signal)
      case List(Literal(false)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object IfF extends PartialSite with UntypedSite {
  override def name = "IfF"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => None
      case List(Literal(false)) => Some(Signal)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object Eq extends TotalSite with UntypedSite {
  override def name = "Eq"
  def evaluate(args: List[Value]) =
    args match {
      case List(a,b) => Literal(a equals b)
      case _ => throw new ArityMismatchException(2, args.size)
  }
}

object Let extends TotalSite with UntypedSite {
  override def name = "let"
  def evaluate(args: List[Value]) = args match {
    case Nil => Signal
    case v :: Nil => v
    case vs => OrcTuple(vs)
    OrcTuple(args)
  }
}