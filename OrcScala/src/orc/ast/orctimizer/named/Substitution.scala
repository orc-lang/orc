//
// Substitution.scala -- Scala trait Substitution
// Project OrcScala
//
// Created by dkitchin on Jul 13, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.orctimizer.named

import swivel.Zipper

/** Direct substitutions on named ASTs.
  *
  * @author dkitchin
  */
trait Substitution[X <: NamedAST] {
  self: NamedAST =>

  def subst(a: Argument, x: Argument): X = Substitution(a, x)(this.toZipper()).asInstanceOf[X]
  def subst(a: Argument, s: String): X = Substitution(a, UnboundVar(s))(this.toZipper()).asInstanceOf[X]

  def substAll(sublist: List[(Argument, String)]): X = {
    val subs = new scala.collection.mutable.HashMap[Argument, Argument]()
    for ((a, s) <- sublist) {
      val x = UnboundVar(s)
      assert(!subs.contains(x))
      subs += ((x, a))
    }
    Substitution.allArgs(subs)(this.toZipper()).asInstanceOf[X]
  }
  def substAll(subs: Map[Argument, Argument]): X = {
    Substitution.allArgs(subs)(this.toZipper()).asInstanceOf[X]
  }

  def subst(t: Type, u: Typevar): X = Substitution(t, u)(this.toZipper()).asInstanceOf[X]
  def subst(t: Typevar, s: String): X = Substitution(t, UnboundTypevar(s))(this.toZipper()).asInstanceOf[X]
  def substAllTypes(sublist: List[(Type, String)]): X = {
    val subs = new scala.collection.mutable.HashMap[Typevar, Type]()
    for ((t, s) <- sublist) {
      val u = UnboundTypevar(s)
      assert(!subs.isDefinedAt(u))
      subs += ((u, t))
    }
    Substitution.allTypes(subs)(this.toZipper()).asInstanceOf[X]
  }
  def substAllTypes(subs: Map[Typevar, Type]): X = {
    Substitution.allTypes(subs)(this.toZipper()).asInstanceOf[X]
  }
}

object Substitution {

  def apply(a: Argument, x: Argument) =
    new Transform {
      override val onArgument = {
        case Zipper(`x`, _) => a
        case y: Argument.Z => y.value
      }
    }

  def apply(t: Type, u: Typevar) =
    new Transform {
      override val onType = {
        case Zipper(`u`, _) => t
        case w: Typevar.Z => w.value
      }
    }

  def allArgs(subs: scala.collection.Map[Argument, Argument]) =
    new Transform {
      override val onArgument = {
        case x: Var.Z => {
          if (subs.isDefinedAt(x.value)) { subs(x.value) } else { x.value }
        }
      }
    }

  def allTypes(subs: scala.collection.Map[Typevar, Type]) =
    new Transform {
      override val onType = {
        case u: Typevar.Z => {
          if (subs.isDefinedAt(u.value)) { subs(u.value) } else { u.value }
        }
      }
    }

}

trait ContextualSubstitution {
  self: Expression =>

  def subst(subContext: Map[String, Argument], subTypeContext: Map[String, Type]): Expression = {
    val transform =
      new Transform {
        override val onArgument = {
          case x @ UnboundVar.Z(s) => subContext.getOrElse(s, x.value)
        }
        override val onType = {
          case x @ UnboundTypevar.Z(s) => subTypeContext.getOrElse(s, x.value)
        }
      }
    transform(this.toZipper())
  }

}
