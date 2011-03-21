//
// Datatypes.scala -- Scala class DataSite and Object DatatypeBuilder
// Project OrcScala
//
// $Id: Datatypes.scala 2581 2011-03-21 07:41:38Z dkitchin $
//
// Created by dkitchin on June 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
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
      
      /* Extract the constructor types from the datatype
       * passed as a type argument
       */
      typeArgs match {
        case List(d: MonomorphicDatatype) => {
          TupleType(d.constructorTypes.get)
        }
        case List(TypeInstance(d: PolymorphicDatatype,_)) => {
          TupleType(d.constructorTypes.get)
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
  
  val extractor = new PartialSite1 with UntypedSite {
    override def eval(arg: AnyRef): Option[AnyRef] = {
      arg match {
        case TaggedValue(tag,values) if (tag == DataSite.this) => Some(OrcValue.letLike(values))
        case _ => None
      }
    }
  }
  
  override def toOrcSyntax() = name
}
