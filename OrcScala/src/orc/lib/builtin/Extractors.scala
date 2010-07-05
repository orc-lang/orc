package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

object NoneExtractor extends PartialSite with UntypedSite {
  override def name = "None?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(None)) => Some(Signal)
      case List(OrcOption(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object SomeExtractor extends PartialSite with UntypedSite {
  override def name = "Some?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(Some(v))) => Some(v)
      case List(OrcOption(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}



object NilExtractor extends PartialSite with UntypedSite {
  override def name = "Nil?"
  def evaluate(args: List[Value]) = {
    args match {
      case List(OrcList(Nil)) => Some(Signal)
      case List(OrcList(_)) => None
      case List(Literal(OrcList(Nil))) => Some(Signal)
      case List(Literal(OrcList(_))) => None
      case List(Literal(arr: Array[AnyRef])) if (arr.size == 0) => Some(Signal)
      case List(Literal(arr: Array[AnyRef])) if (arr.size != 0) => None
      case List(Literal(c:java.util.List[Any])) if (c.size == 0) => Some(Signal)
      case List(Literal(c:java.util.List[Any])) if (c.size != 0) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
  }
}

object ConsExtractor extends PartialSite with UntypedSite {
  override def name = "Cons?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(v :: vs)) => Some(OrcTuple(List(v, OrcList(vs))))
      case List(OrcList(_)) => None
      case List(Literal(OrcList(v :: vs))) => Some(OrcTuple(List(v, OrcList(vs))))
      case List(Literal(OrcList(_))) => None
      case List(Literal(c:java.util.List[Any])) if (c.size != 0) => {
        Some(OrcTuple(List(Literal(c.get(0)),Literal(c.subList(1,c.size)))))
      }
      case List(Literal(c:java.util.List[Any])) if (c.size == 0) => None
      case List(Literal(arr: Array[AnyRef])) if (arr.size != 0) => { // Allow List-like pattern matching on arrays.
        Some(OrcTuple(List(Literal(arr(0)),Literal(arr.slice(1,arr.size)))))
      }
      case List(Literal(arr: Array[AnyRef])) if (arr.size == 0) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

/* 
 * Checks if a Tuple t has a given number of elements.
 * If the check succeeds, the Some(t) is returned,
 * else None.
 */
object TupleArityChecker extends PartialSite with UntypedSite {
  override def name = "TupleArityChecker?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcTuple(elems),Literal(arity:Int)) =>
        if (elems.size == arity) {
          Some(OrcTuple(elems))
        } else {
          None
        }
      case List(OrcTuple(_),_) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object FindExtractor extends TotalSite with UntypedSite {
  override def name = "FindExtractor"
  def evaluate(args: List[Value]) =
    args match {
      case List(s : Site) => s.extract match {
        case Some(extractor) => extractor
        case None => throw new Exception("Could not find extractor for site"+s)
      }
    }
}