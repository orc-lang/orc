package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException


object TupleConstructor extends TotalSite with UntypedSite {
  override def name = "Tuple"
  def evaluate(args: List[Value]) = OrcTuple(args)
}

object NoneConstructor extends TotalSite with UntypedSite {
  override def name = "None"
  def evaluate(args: List[Value]) =
    args match {
      case List() => OrcOption(None)
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = Some(NoneExtractor)
}

object SomeConstructor extends TotalSite with UntypedSite {
  override def name = "Some"
  def evaluate(args: List[Value]) =
    args match {
      case List(v) => OrcOption(Some(v))
      case _ => throw new ArityMismatchException(1, args.size)
  }
  override def extract = Some(SomeExtractor)
}



object NilConstructor extends TotalSite with UntypedSite {
  override def name = "Nil"
  def evaluate(args: List[Value]) =
    args match {
      case List() => OrcList(Nil)
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = Some(NilExtractor)
}

object ConsConstructor extends TotalSite with UntypedSite {
  override def name = "Cons"
  def evaluate(args: List[Value]) =
    args match {
      case List(v, OrcList(vs)) => OrcList(v :: vs)
      case List(v1, Literal(v2 : AnyRef)) => throw new ArgumentTypeMismatchException(1, "List", v2.getClass().toString())
      case List(v1, v2) => throw new ArgumentTypeMismatchException(1, "List", v2.getClass().toString())
      case _ => throw new ArityMismatchException(2, args.size)
  }
  override def extract = Some(ConsExtractor)
}

object RecordConstructor extends UnimplementedSite //FIXME:TODO: Implement