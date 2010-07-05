package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

object DatatypeBuilder extends TotalSite with UntypedSite {
  
  override def name = "Datatype"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcTuple(vs)) => {
        val datasites: List[Value] = 
          for (OrcTuple(Literal(name: String) :: List(Literal(arity: Int))) <- vs)
            yield new DataSite(name,arity)
        OrcTuple(datasites)
      }
    }
}

class DataSite(name: String, arity: Int) extends TotalSite with UntypedSite {
  
  def evaluate(args: List[Value]): Value = {
      if(args.size != arity) {
        throw new ArityMismatchException(arity, args.size)
      }
      TaggedValues(this,args)
  }
  
  override def extract = Some(new PartialSite  with UntypedSite {
 
    override def evaluate(args: List[Value]) = {
        args match {
          case List(TaggedValues(tag,values)) => {
            if (tag == DataSite.this)
              Some(OrcTuple(values))
            else 
              None
          }
          case _ => None
        }
    }
  })
  
  override def toOrcSyntax() = name
}