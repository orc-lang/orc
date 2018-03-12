//
// Type.scala -- Scala traits TypeInterface and Type
// Project OrcScala
//
// Created by dkitchin on Nov 28, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.types

import orc.error.compiletime.typing.{ OverconstrainedTypeVariableException, SubtypeFailureException }
import orc.types.Variance.enrichVarianceList

/* Used for reference only. */
trait TypeInterface {
  def join(that: Type): Type
  def meet(that: Type): Type
  def <(that: Type): Boolean
  def subst(sigma: Map[TypeVariable, Type]): Type
}

trait Type extends TypeInterface {

  override def toString = this.getClass.toString

  /* Default join:
   * Return the maximum of the two types;
   * if they are unrelated, return Top.
   */
  def join(that: Type): Type = {
    if (this < that) { that }
    else if (that < this) { this }
    else { Top }
  }

  /* Default meet:
   * Return the minimum of the two types;
   * if they are unrelated, return Bot.
   */
  def meet(that: Type): Type = {
    if (this < that) { this }
    else if (that < this) { that }
    else { Bot }
  }

  /* Default substitution:
   * Assume there are no free type variables or children,
   * and return the type unchanged.
   */
  def subst(sigma: Map[TypeVariable, Type]): Type = { this }

  /* Default < relation:
   * For all T,
   *     T < T
   * and T < Top
   */
  def <(that: Type): Boolean = {
    (that eq this) || (that match {
      case Top => true
      case JavaObjectType(cl, _) => cl isAssignableFrom classOf[java.lang.Object]
      case _ => false
    })
  }

  /* Eliminate all variables X in this type for which V(X) is true.
   * Produce the least supertype of this type with such variables eliminated.
   * It is possible for elimination to fail if a variable occurs in
   * an invariant position (or in both covariant and contravariant positions),
   * since neither Top nor Bot can be chosen in that case.
   */
  def elim(V: TypeVariable => Boolean)(implicit variance: Variance): Type = {
    this match {
      case x: TypeVariable => {
        if (V(x)) {
          variance match {
            case Covariant => Top
            case Contravariant => Bot
            case Invariant => throw new OverconstrainedTypeVariableException()
            case Constant => Top
          }
        } else {
          this
        }
      }
      case TupleType(elements) => TupleType(elements map { _ elim V })
      case RecordType(entries) => RecordType(entries mapValues { _ elim V })
      case FunctionType(typeFormals, argTypes, returnType) => {
        def Vscope(x: TypeVariable) = {
          (!(typeFormals contains x)) && (V(x))
        }
        val newArgTypes = {
          val newVariance = Contravariant(variance)
          argTypes map { _.elim(Vscope)(newVariance) }
        }
        val newReturnType = returnType elim Vscope
        FunctionType(typeFormals, newArgTypes, newReturnType)
      }
      case TypeInstance(tycon, args) => {
        val newArgs =
          for ((v, t) <- tycon.variances zip args) yield {
            val newVariance = v(variance)
            t.elim(V)(newVariance)
          }
        TypeInstance(tycon, newArgs)
      }
      case _ => this
    }
  }

  /* Promotion is a special case of elimination */
  def promote(V: TypeVariable => Boolean): Type = {
    this.elim(V)(Covariant)
  }
  /* Demotion is a special case of elimination */
  def demote(V: TypeVariable => Boolean): Type = {
    this.elim(V)(Contravariant)
  }
  /* Eliminate _all_ free type variables */
  def clean: Type = {
    this.elim({ _ => true })(Covariant)
  }

  /* Find the variance of a given variable in this type */
  def varianceOf(x: TypeVariable): Variance = {
    this match {
      case `x` => Covariant
      case _: TypeVariable => Constant
      case TupleType(elements) => {
        val variances = elements map { _ varianceOf x }
        variances.combined
      }
      case RecordType(entries) => {
        val variances = entries.values map { _ varianceOf x }
        variances.toList.combined
      }
      case FunctionType(typeFormals, argTypes, returnType) => {
        assert(!(typeFormals contains x))
        val argVariances = argTypes map { _ varianceOf x } map { Contravariant(_) }
        val returnVariance = returnType varianceOf x
        argVariances.combined & returnVariance
      }
      case TypeInstance(tycon, args) => {
        val variances =
          for ((v, t) <- tycon.variances zip args) yield {
            v(t varianceOf x)
          }
        variances.toList.combined
      }
      case _ => Constant
    }
  }

  /* Default equality:
   * S = T  iff  S < T and T < S
   *
   * Note: this default equality will not hold if bounded polymorphism
   * is added to the system. In this case, make the equality method
   * abstract; types must supply their own equality.
   */
  def equals(that: Type): Boolean = {
    (this < that) && (that < this)
  }

  /* Convenience methods */
  def assertSubtype(that: Type) {
    if (!(this < that)) {
      throw new SubtypeFailureException(that, this)
    }
  }
  def subst(T: Type, X: TypeVariable): Type = subst(List(T), List(X))
  def subst(T: List[Type], X: List[TypeVariable]): Type = {
    val bindings = {
      assert(T.size == X.size)
      (X zip T).toMap
    }
    if (bindings.isEmpty) { this } else { subst(bindings) }
  }

  def letLike(ts: List[Type]): Type = {
    ts match {
      case Nil => SignalType
      case List(t) => t
      case _ => TupleType(ts)
    }
  }

}
