package orc.lib.builtin

import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.compiletime.typing._
import orc.types._

object DatatypeBuilder extends TotalSite with TypedSite {
  
  override def name = "Datatype"
  def evaluate(args: List[AnyRef]) = {
    val datasites: List[AnyRef] = 
      for ( OrcTuple(List(name: String, arity: BigInt)) <- args) yield {
        new DataSite(name,arity.intValue)
      }
    OrcTuple(datasites)
  }
     
  def orcType() = new CallableType {
    
    def call(typeArgs: List[Type], argTypes: List[Type]) = {
      
      // verify that the argument types are of the correct form
      for (t <- argTypes) {
        t assertSubtype TupleType(List(StringType, IntegerType))
      }
      
      // Extract the datatype that's been smuggled in via the type argument
      typeArgs match {
        case List(d: MonomorphicDatatype) => {
          val constructorTypes = 
            for ((_, params) <- d.variants) yield { 
              SimpleFunctionType(params, d) 
            }
          TupleType(constructorTypes.toList)
        }
        case List(TypeInstance(d: PolymorphicDatatype,_)) => {
          val constructorTypes = 
            for ((_, params) <- d.variants) yield {
              FunctionType(d.typeFormals, params, TypeInstance(d, d.typeFormals))
            }
          TupleType(constructorTypes.toList)
        }
        case _ => throw new MalformedDatatypeCallException() 
      }
      
    }
   
  }
}

class DataSite(name: String, arity: Int) extends TotalSite with Extractable  {
  
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