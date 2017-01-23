//
// Tuples.scala -- Implementations of tuple manipulation sites
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

package orc.lib.builtin.structured

import orc.error.compiletime.typing.{ ArgumentTypecheckingException, ExpectedType, TupleSizeException }
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types._
import orc.values._
import orc.values.sites._

object TupleConstructor extends TotalSite with TypedSite with FunctionalSite with TalkativeSite {
  override def name = "Tuple"
  def evaluate(args: Array[AnyRef]) = OrcTuple(args)

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
object TupleArityChecker extends PartialSite2 with TypedSite with FunctionalSite {
  override def name = "TupleArityChecker"
  def eval(x: AnyRef, y: AnyRef) =
    (x, y) match {
      case (t @ OrcTuple(elems), arity: BigInt) =>
        if (elems.length == arity.toInt) {
          Some(t)
        } else {
          None
        }
      case (_: OrcTuple, a) => throw new ArgumentTypeMismatchException(1, "Integer", if (a != null) a.getClass().toString() else "null")
      case (a, _) => None // Not a Tuple
    }

  override def returnMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = args match {
    case List(_, Some(arity: BigInt)) => Some(OrcTuple((0 until arity.toInt).map(_ => null).toArray))
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
