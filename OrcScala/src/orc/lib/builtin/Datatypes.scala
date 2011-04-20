//
// Datatypes.scala -- Scala class DataSite and Object DatatypeBuilder
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on June 24, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
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
      for ( OrcTuple(List(siteName: String, arity: BigInt)) <- args) yield {
        val tag = new Tag(siteName)
        new OrcRecord(
          "apply" -> new DatatypeConstructor(arity.intValue, tag) { override def name = siteName + ".apply" },
          "unapply" -> new DatatypeExtractor(tag) { override def name = siteName + ".unapply" }
        )
      }
    OrcTuple(datasites)
  }
     
  def orcType() = new CallableType with StrictType {
    
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

class DatatypeConstructor(arity: Int, tag: Tag) extends TotalSite {
  def evaluate(args: List[AnyRef]): AnyRef = {
    if (args.size != arity) {
      throw new ArityMismatchException(arity, args.size)
    }
    else {
      TaggedValue(tag, args)
    }
  }
}
class DatatypeExtractor(tag: Tag) extends PartialSite1 {
  def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case TaggedValue(`tag`,values) => Some(OrcValue.letLike(values))
      case _ => None
    }
  }
}
