//
// Records.scala -- Implementations of record manipulation sites
// Project OrcScala
//
// Created by dkitchin on March 31, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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

import orc.{ DirectInvoker, OrcRuntime }
import orc.error.compiletime.typing.{ ArgumentTypecheckingException, ExpectedType, RecordShapeMismatchException }
import orc.error.runtime.HaltException
import orc.types.{ RecordType, SimpleCallableType, StrictCallableType, Top, TupleType, Type, FieldType }
import orc.util.ArrayExtensions.{ Array2, ArrayN }
import orc.util.OptionMapExtension.addOptionMapToList
import orc.values.{ Field, OrcRecord, OrcTuple, OrcValue }
import orc.values.sites.{ FunctionalSite, IllegalArgumentInvoker, PartialSite, TotalSite, TypedSite }

object RecordConstructor extends TotalSite with TypedSite with FunctionalSite {
  override def name = "Record"

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    val nArgs = args.size
    val invoker = new DirectInvoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        arguments.length == nArgs && {
          (0 until nArgs).forall { v =>
            arguments(v) match {
              case OrcTuple(Array2(Field(_), _)) =>
                true
              case _ =>
                false
            }
          }
        }
      }

      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        val valueMap = new scala.collection.mutable.HashMap[String, AnyRef]()
        args.zipWithIndex foreach {
          case (v: AnyRef, i: Int) =>
            v match {
              case OrcTuple(Array2(Field(key), value: AnyRef)) =>
                valueMap += ((key, value))
            }
        }
        OrcRecord(scala.collection.immutable.HashMap.empty ++ valueMap)
      }
    }
    if (invoker.canInvoke(this, args)) {
      invoker
    } else {
      new IllegalArgumentInvoker(this, args)
    }
  }

  def orcType() = new SimpleCallableType with StrictCallableType {
    def call(argTypes: List[Type]) = {
      val bindings =
        (argTypes.zipWithIndex) map {
          case (TupleType(List(FieldType(f), t)), _) => (f, t)
          case (t, i) => throw new ArgumentTypecheckingException(i, TupleType(List(ExpectedType("of some field"), Top)), t)
        }
      RecordType(bindings.toMap)
    }
  }
}

object RecordMatcher extends PartialSite with TypedSite with FunctionalSite {
  override def name = "RecordMatcher"

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    val nArgs = args.size
    val invoker = new DirectInvoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        arguments.length == nArgs && {
          (1 until nArgs).forall { arguments(_).isInstanceOf[Field] }
        }
      }

      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        arguments match {
          case ArrayN(OrcRecord(entries), shape @ _*) => {
            val matchedValues: Option[List[AnyRef]] = shape.toList optionMap {
                case Field(f) => entries get f
              }
            matchedValues map { OrcValue.letLike } getOrElse { throw new HaltException }
          }
          case _ => throw new HaltException
        }
      }
    }
    if (invoker.canInvoke(this, args)) {
      invoker
    } else {
      new IllegalArgumentInvoker(this, args)
    }
  }

  def orcType() = new SimpleCallableType with StrictCallableType {
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
}
