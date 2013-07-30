//
// Records.scala -- Implementations of record manipulation sites
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on March 31, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/* 
 * Input to a RecordConstructor is a sequence of pairs.
 * Each pair is a (field, site) mapping.
 * Example: ( (.x,Site(x)), (.y, Site(y)), (.z, Site(z)) )
 */

package orc.lib.builtin.structured

import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.compiletime.typing.ArgumentTypecheckingException
import orc.error.compiletime.typing.RecordShapeMismatchException
import orc.error.compiletime.typing.ExpectedType
import orc.values.{ OrcRecord, OrcValue, OrcTuple, Field }
import orc.values.sites._
import orc.types._
import orc.util.OptionMapExtension._

object RecordConstructor extends TotalSite with TypedSite with DirectTotalSite {
  override def name = "Record"
  override def evaluate(args: List[AnyRef]) = {
    val valueMap = new scala.collection.mutable.HashMap[String, AnyRef]()
    args.zipWithIndex map
      {
        case (v: AnyRef, i: Int) =>
          v match {
            case OrcTuple(List(Field(key), value: AnyRef)) =>
              valueMap += ((key, value))
            case _ => throw new ArgumentTypeMismatchException(i, "(Field, _)", if (v != null) v.getClass().getCanonicalName() else "null")
          }
      }
    OrcRecord(scala.collection.immutable.HashMap.empty ++ valueMap)
  }

  def orcType() = new SimpleCallableType with StrictType {
    def call(argTypes: List[Type]) = {
      val bindings =
        (argTypes.zipWithIndex) map {
          case (TupleType(List(FieldType(f), t)), _) => (f, t)
          case (t, i) => throw new ArgumentTypecheckingException(i, TupleType(List(ExpectedType("of some field"), Top)), t)
        }
      RecordType(bindings.toMap)
    }
  }
  override val effectFree = true
}

object RecordMatcher extends PartialSite with TypedSite with DirectPartialSite  {
  override def name = "RecordMatcher"

  override def evaluate(args: List[AnyRef]): Option[AnyRef] =
    args match {
      case List(OrcRecord(entries), shape @ _*) => {
        val matchedValues: Option[List[AnyRef]] =
          shape.toList.zipWithIndex optionMap {
            case (Field(f), _) => entries get f
            case (a, i) => throw new ArgumentTypeMismatchException(i + 1, "Field", if (a != null) a.getClass().toString() else "null")
          }
        matchedValues map { OrcValue.letLike }
      }
      case List(_, _*) => None
      case _ => throw new AssertionError("Record match internal failure (RecordMatcher.evaluate match error on args list)")
    }

  def orcType() = new SimpleCallableType with StrictType {
    def call(argTypes: List[Type]): Type = {
      argTypes match {
        case List(rt @ RecordType(entries), shape @ _*) => {
          val matchedElements =
            shape.toList.zipWithIndex map {
              case (FieldType(f), _) => entries.getOrElse(f, throw new RecordShapeMismatchException(rt, f))
              case (t, i) => throw new ArgumentTypecheckingException(i + 1, ExpectedType("of some field"), t)
            }
          letLike(matchedElements)
        }
        case List(t, _*) => throw new ArgumentTypecheckingException(0, RecordType(Nil.toMap), t)
        case _ => throw new AssertionError("Record type checking internal failure (RecordMatcher.orcType.<new SimpleCallableType with StrictType>.call match error)")
      }
    }
  }
  override val effectFree = true
}
