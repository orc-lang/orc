//
// ConstraintSet.scala -- Scala class ConstraintSet
// Project OrcScala
//
// Created by dkitchin on Nov 28, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.typecheck

import orc.types._
import orc.error.compiletime.typing._

/** Sets of constraints on type variables.
  *
  * Each constraint is of the form
  *
  *  S <: X <: T
  *
  * @author dkitchin
  */
class ConstraintSet(val bounds: List[(TypeVariable, (Type, Type))]) {

  def this(xs: Set[TypeVariable]) = {
    this(xs.toList map { (_, (Bot, Top)) })
  }

  def this(s: Type, x: TypeVariable, t: Type) = {
    this(List((x, (s, t))))
  }

  val constraints: Map[TypeVariable, (Type, Type)] = {
    bounds.toMap.withDefaultValue((Bot, Top))
  }

  def lowerBoundOn(x: TypeVariable): Type = constraints(x)._1
  def upperBoundOn(x: TypeVariable): Type = constraints(x)._2

  def meet(that: ConstraintSet): ConstraintSet = {
    val newVars = (this.constraints.keySet) union (that.constraints.keySet)
    val newConstraints =
      for (x <- newVars) yield {
        val newLowerBound = (this lowerBoundOn x) join (that lowerBoundOn x)
        val newUpperBound = (this upperBoundOn x) meet (that upperBoundOn x)
        if (newLowerBound < newUpperBound) {
          (x, (newLowerBound, newUpperBound))
        } else {
          throw new OverconstrainedTypeVariableException()
        }
      }
    new ConstraintSet(newConstraints.toList)
  }

  /* Find the minimal substitution of the type variables
   * from this constraint set into the given type R.
   *
   * If there is no minimal type, invoke warnNoMinimal
   * to emit a warning and report the type that was guessed.
   */
  def minimalSubstitution(R: Type, warnNoMinimal: Type => Unit): Map[TypeVariable, Type] = {
    val vars = constraints.keys
    val subs =
      for (x <- vars) yield {
        R varianceOf x match {
          case Constant | Covariant => (x, this lowerBoundOn x)
          case Contravariant => (x, this upperBoundOn x)
          case Invariant => {
            val S = this lowerBoundOn x
            val T = this upperBoundOn x
            if (S equals T) {
              (x, S)
            } else {
              val bestGuess =
                S match {
                  case Bot => T
                  case _ => S
                }
              warnNoMinimal(bestGuess)
              (x, bestGuess)
            }
          }
        }
      }
    subs.toMap
  }

  /* Find any such substitution; it need not be minimal. */
  def anySubstitution: Map[TypeVariable, Type] = {
    val vars = constraints.keys
    val subs = vars map { x => (x, lowerBoundOn(x)) }
    subs.toMap
  }

}

object ConstraintSet {

  def meetAll(C: Iterable[ConstraintSet]) = {
    C.toList.foldLeft(NoConstraints: ConstraintSet) { _ meet _ }
  }

}

object NoConstraints extends ConstraintSet(Nil)
