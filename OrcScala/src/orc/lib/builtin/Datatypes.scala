//
// Datatypes.scala -- Scala class DataSite and Object DatatypeBuilder
// Project OrcScala
//
// Created by dkitchin on June 24, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.error.compiletime.typing.MalformedDatatypeCallException
import orc.error.runtime.ArityMismatchException
import orc.types._
import orc.util.TypeListEnrichment._
import orc.values._
import orc.values.sites.{ FunctionalSite, PartialSite1, TotalSite, TypedSite }

object DatatypeBuilder extends TotalSite with TypedSite with FunctionalSite {

  override def name = "Datatype"
  def evaluate(args: Array[AnyRef]) = {
    val datasites: Array[AnyRef] =
      for (OrcTuple(Array(siteName: String, arity: BigInt)) <- args) yield {
        val tag = new Tag(siteName)
        new OrcRecord(
          "apply" -> new DatatypeConstructor(arity.intValue, tag) { override def name = siteName + ".apply" },
          "unapply" -> new DatatypeExtractor(tag) { override def name = siteName + ".unapply" })
      }
    OrcValue.letLike(datasites)
  }

  def orcType() = new CallableType with StrictCallableType {

    def call(typeArgs: List[Type], argTypes: List[Type]) = {

      // verify that the argument types are of the correct form
      for (t <- argTypes) {
        t assertSubtype TupleType(List(StringType, IntegerType))
      }

      /* Extract the constructor types from the datatype
       * passed as a type argument
       */

      /*
       * There is a special case for datatypes with a single constructor. Instead of
       * using a tuple type we simply use the type of the single constructor.
       *
       * This matches the pattern generated in Translator.scala.
       */
      typeArgs match {
        case List(d: MonomorphicDatatype) => {
          d.constructorTypes.get.condense
        }
        case List(TypeInstance(d: PolymorphicDatatype, _)) => {
          d.constructorTypes.get.condense
        }
        case _ => throw new MalformedDatatypeCallException()
      }

    }

  }
}

class DatatypeConstructor(arity: Int, tag: Tag) extends TotalSite with FunctionalSite {
  def evaluate(args: Array[AnyRef]): AnyRef = {
    if (args.size != arity) {
      throw new ArityMismatchException(arity, args.size)
    } else {
      TaggedValue(tag, args)
    }
  }
}
class DatatypeExtractor(tag: Tag) extends PartialSite1 with FunctionalSite {
  def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case TaggedValue(`tag`, values) => Some(OrcValue.letLike(values))
      case _ => None
    }
  }
}
