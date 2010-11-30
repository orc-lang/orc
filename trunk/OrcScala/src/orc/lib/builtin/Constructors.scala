package orc.lib.builtin

import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.types.SimpleCallableType
import orc.types._


object OptionType extends SimpleTypeConstructor("Option", Covariant)
object ListType extends SimpleTypeConstructor("List", Covariant)


object TupleConstructor extends TotalSite with TypedSite {
  override def name = "Tuple"
  def evaluate(args: List[AnyRef]) = OrcTuple(args)
  
  val orcType = new SimpleCallableType {
    def call(argTypes: List[Type]) = { TupleType(argTypes) }
  }
}

object NoneConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "None"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List() => None
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = NoneExtractor
  
  val orcType = SimpleFunctionType(OptionType(Bot))
}

object SomeConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Some"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(v) => Some(v)
      case _ => throw new ArityMismatchException(1, args.size)
  }
  override def extract = SomeExtractor
  
  val orcType = new UnaryCallableType { def call(t: Type) = OptionType(t) }
}



object NilConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Nil"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List() => Nil
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = NilExtractor
  
  val orcType = SimpleFunctionType(ListType(Bot))
}

object ConsConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Cons"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(v, vs : List[_]) => v :: vs
      case List(_, vs) => throw new ArgumentTypeMismatchException(1, "List", if (vs != null) vs.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(2, args.size)
  }
  override def extract = ConsExtractor
  
  val orcType = {
    val X = new TypeVariable()
    FunctionType(List(X), List(X, ListType(X)), ListType(X))
  }
}

/* 
 * Input to a RecordConstructor is a sequence of tuples, each tuple
 * being a (string,site) mapping. Eg: (("x",Site(x)), ("y", Site(y)), ("z", Site(z))..))
 * 
 * Note that even though a Record pattern exists, the RecordConstructor
 * is not Extractable, since record extraction is a specific two-step process,
 * parametrized by the target shape of the record.
 */
object RecordConstructor extends TotalSite with TypedSite {
  override def name = "Record"
  override def evaluate(args: List[AnyRef]) = {
    val valueMap = new scala.collection.mutable.HashMap[String,AnyRef]()
    args.zipWithIndex map
      { case (v: AnyRef, i: Int) =>
          v match {
            case OrcTuple(List(key: String, value : AnyRef)) =>
              valueMap += ( (key,value) )
            case _ => throw new ArgumentTypeMismatchException(i, "(String, _)", if (v != null) v.getClass().toString() else "null")
          }
      }
    OrcRecord(scala.collection.immutable.HashMap.empty ++ valueMap)
  }
  
  val orcType = new SimpleCallableType {
    def call(argTypes: List[Type]) = { 
      val bindings = 
        (argTypes.zipWithIndex) map {
          case (TupleType(List(FieldType(f), t)), _) => (f, t)
          case (t, i) => throw new ArgumentTypeMismatchException(i, "(Field, _)", t.toString)
        }
      RecordType(bindings.toMap)
    }
  }
}



