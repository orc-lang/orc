//
// Tuples.scala -- Implementations of tuple manipulation sites
// Project OrcScala
//
// Created by dkitchin on March 31, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin.structured

import orc.{ Invoker, OnlyDirectInvoker, OrcRuntime }
import orc.error.compiletime.typing.{ ArgumentTypecheckingException, ExpectedType, TupleSizeException }
import orc.error.runtime.HaltException
import orc.types.{ BinaryCallableType, IntegerConstantType, IntegerType, SimpleCallableType, StrictCallableType, TupleType, Type }
import orc.values.OrcTuple
import orc.values.sites.{ FunctionalSite, InvokerMethod, OverloadedDirectInvokerMethod2, SiteMetadata, TalkativeSite }

// TODO: Replace current tuple values with object and _n fields.

object TupleConstructor extends InvokerMethod with FunctionalSite with TalkativeSite {
  override def name = "Tuple"

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    new OnlyDirectInvoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        target eq TupleConstructor
      }
      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
        try {
          OrcTuple(arguments)
        } finally {
          orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
        }
      }
    }
  }

  override def returnMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = Some(OrcTuple(args.toArray))

  def orcType() = new SimpleCallableType with StrictCallableType {
    def call(argTypes: List[Type]) = { TupleType(argTypes) }
  }
}

/*
 * Verifies that a Tuple t has a given number of elements.
 * If the check succeeds, the Some(t) is returned,
 * else None.
 */
object TupleArityChecker extends OverloadedDirectInvokerMethod2[OrcTuple, Number] with FunctionalSite {
  override def name = "TupleArityChecker"
  def getInvokerSpecialized(t: OrcTuple, arity: Number): Invoker = {
    invoker(t, arity)((t, arity) =>
      if (t.values.length == arity.intValue) {
        t
      } else {
        throw HaltException.SINGLETON
      })
  }

  override def returnMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = args match {
    case List(_, Some(arity: BigInt)) => Some(OrcTuple((0 until arity.toInt).map(_ => null).toArray))
    case List(_, Some(arity: java.lang.Long)) => Some(OrcTuple((0 until arity.toInt).map(_ => null).toArray))
    case _ => None
  }

  def orcType() = new BinaryCallableType with StrictCallableType {
    def call(r: Type, s: Type): Type = {
      (r, s) match {
        case (t @ TupleType(elements), IntegerConstantType(i)) => {
          if (elements.size != i) {
            throw new TupleSizeException(i.toInt, elements.size)
          }
          OptionType(t)
        }
        case (t: TupleType, IntegerType) => {
          OptionType(t)
        }
        case (_: TupleType, t) => throw new ArgumentTypecheckingException(1, IntegerType, t)
        case (t, _) => throw new ArgumentTypecheckingException(0, ExpectedType("of some tuple"), t)
      }
    }
  }

}
