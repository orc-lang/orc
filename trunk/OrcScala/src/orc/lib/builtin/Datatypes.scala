package orc.lib.builtin

import orc.ast.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

object DatatypeBuilder extends TotalSite with UntypedSite {
  
  override def name = "Datatype"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(OrcTuple(vs)) => {
        val datasites: List[AnyRef] = 
          for ( OrcTuple(List(name: String, arity: BigInt)) <- vs)
            yield new DataSite(name,arity.intValue)
        OrcTuple(datasites)
      }
    }
}

class DataSite(name: String, arity: Int) extends TotalSite with Extractable with UntypedSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
      if(args.size != arity) {
        throw new ArityMismatchException(arity, args.size)
      }
      TaggedValue(this,args)
  }
  
  override def extract = new PartialSite  with UntypedSite {
 
    override def evaluate(args: List[AnyRef]) = {
        args match {
          case List(TaggedValue(tag,values)) if (tag == DataSite.this) => Some(OrcValue.letLike(values))
          case List(_) => None
          case _ => throw new ArityMismatchException(1, args.size)
        }
    }
  }
  
  override def toOrcSyntax() = name
}