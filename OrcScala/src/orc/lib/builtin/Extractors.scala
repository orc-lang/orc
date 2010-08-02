package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

trait Extractable extends Site {
  def extract: PartialSite
}


object NoneExtractor extends PartialSite with UntypedSite {
  override def name = "None?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(None) => Some(Signal)
      case List(Some(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}


object SomeExtractor extends PartialSite with UntypedSite {
  override def name = "Some?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(Some(v : AnyRef)) => Some(v)
      case List(None) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}


object NilExtractor extends PartialSite with UntypedSite {
  override def name = "Nil?"
  def evaluate(args: List[AnyRef]) = {
    args match {
      case List(Nil) => Some(Signal)
      case List(_::_) => None
      case List(arr: Array[AnyRef]) if (arr.size == 0) => Some(Signal)
      case List(arr: Array[AnyRef]) if (arr.size != 0) => None
      case List(c:java.util.List[Any]) if (c.size == 0) => Some(Signal)
      case List(c:java.util.List[Any]) if (c.size != 0) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
  }
}


object ConsExtractor extends PartialSite with UntypedSite {
  override def name = "Cons?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List((v : AnyRef) :: vs) => Some(OrcTuple(List(v, vs)))
      case List(Nil) => None
      case List(c:java.util.List[AnyRef]) if (c.size != 0) => {
        Some(OrcTuple(List(c.get(0),c.subList(1,c.size))))
      }
      case List(c:java.util.List[AnyRef]) if (c.size == 0) => None
      case List(arr: Array[AnyRef]) if (arr.size != 0) => { // Allow List-like pattern matching on arrays.
        Some(OrcTuple(List(arr(0), arr.slice(1,arr.size))))
      }
      case List(arr: Array[AnyRef]) if (arr.size == 0) => None
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
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(OrcTuple(elems), arity: BigInt) =>
        if (elems.size == arity) {
          Some(OrcTuple(elems))
        } else {
          None
        }
      case List(a,_) => throw new ArgumentTypeMismatchException(0, "Tuple", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}


object FindExtractor extends TotalSite with UntypedSite {
  override def name = "FindExtractor"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(s: Extractable) => s.extract
      case List(s: Site) => throw new Exception("Could not find extractor for site"+s)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Site", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
}
