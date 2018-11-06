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

import orc.{ DirectInvoker, Invoker, OrcRuntime }
import orc.error.compiletime.typing.{ ArgumentTypecheckingException, ExpectedType, TupleSizeException }
import orc.error.runtime.HaltException
import orc.types.{ BinaryCallableType, IntegerConstantType, IntegerType, SimpleCallableType, StrictCallableType, TupleType, Type }
import orc.values.OrcTuple
import orc.values.sites.{ FunctionalSite, LocalSingletonSite, Site, OverloadedDirectInvokerMethod2, SiteMetadata, TalkativeSite, InlinableInvoker }

// TODO: Replace current tuple values with object and _n fields.

@SerialVersionUID(-2029136642088898630L)
object TupleConstructor extends Site with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override def name = "Tuple"

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    new DirectInvoker with InlinableInvoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        target eq TupleConstructor
      }
      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        orc.run.StopWatches.implementation {
          OrcTuple(arguments)
        }
      }
    }
  }

  override def publicationMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = Some(OrcTuple(args.toArray))

  def orcType() = new SimpleCallableType with StrictCallableType {
    def call(argTypes: List[Type]) = { TupleType(argTypes) }
  }
}

/*
 * Verifies that a Tuple t has a given number of elements.
 * If the check succeeds, the Some(t) is returned,
 * else None.
 */
@SerialVersionUID(-5168873507961952728L)
object TupleArityChecker extends OverloadedDirectInvokerMethod2[AnyRef, Number] with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "TupleArityChecker"
  def getInvokerSpecialized(t: AnyRef, arity: Number) = {
    invoker(t, arity)((t, arity) => t match {
      case t: OrcTuple =>
        if (t.values.length == arity.intValue) {
          t
        } else {
          throw new HaltException
        }
      case _ =>
        throw new HaltException
      })
  }

  override def publicationMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = args match {
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
