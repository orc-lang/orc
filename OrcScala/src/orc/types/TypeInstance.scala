//
// TypeInstance.scala -- Scala class TypeInstance
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 26, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.UncallableTypeException
import orc.error.compiletime.typing.TypeHasNoFieldsException

/** Type instances, type constructors, variances
  *
  * @author dkitchin
  */
case class TypeInstance(tycon: TypeConstructor, args: List[Type]) extends CallableType with HasFieldsType {

  override def toString = tycon.toString + args.mkString("[", ",", "]")

  override def join(that: Type): Type = {
    that match {
      case TypeInstance(`tycon`, otherArgs) => {
        val joinArgs = {
          for ((v, (t, u)) <- (tycon.variances) zip (args zip otherArgs)) yield {
            v match {
              case Covariant => t join u
              case Contravariant => t meet u
              case Invariant => if (t equals u) { t } else { return Top }
              case Constant => Bot
            }
          }
        }
        TypeInstance(tycon, joinArgs)
      }
      case _ => super.join(that)
    }
  }

  override def meet(that: Type): Type = {
    that match {
      case TypeInstance(`tycon`, otherArgs) => {
        val meetArgs = {
          for ((v, (t, u)) <- (tycon.variances) zip (args zip otherArgs)) yield {
            v match {
              case Covariant => t meet u
              case Contravariant => t join u
              case Invariant => if (t equals u) { t } else { return Bot }
              case Constant => Top
            }
          }
        }
        TypeInstance(tycon, meetArgs)
      }
      case _ => super.meet(that)
    }
  }

  override def <(that: Type) = {
    that match {
      case TypeInstance(`tycon`, otherArgs) => {
        val perArgSubtype =
          for ((v, (t, u)) <- (tycon.variances) zip (args zip otherArgs)) yield {
            v match {
              case Covariant => t < u
              case Contravariant => u < t
              case Invariant => t equals u
              case Constant => true
            }
          }
        perArgSubtype forall { b => b }
      }
      // We allow only pointwise comparison of different type operators.
      case TypeInstance(otherTycon, otherArgs) if (tycon < otherTycon) => {
        (args zip otherArgs) forall { case (a, b) => a eq b }
      }
      case _ => super.<(that)
    }
  }

  override def subst(sigma: Map[TypeVariable, Type]): Type = {
    TypeInstance(tycon, args map { _ subst sigma })
  }

  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    tycon.instance(args) match {
      case u: TypeInstance => {
        /* Avoiding an infinte loop */
        throw new UncallableTypeException(u)
      }
      case ct: CallableType => {
        ct.call(typeArgs, argTypes)
      }
      case u => {
        throw new UncallableTypeException(u)
      }
    }
  }
  
  def getField(f: FieldType): Type = {
    tycon.instance(args) match {
      case u: TypeInstance => {
        /* Avoiding an infinte loop */
        throw new TypeHasNoFieldsException(u)
      }
      case ft: HasFieldsType => {
        ft.getField(f)
      }
      case u => {
         // FIXME: What exception should this be?
       throw new TypeHasNoFieldsException(u)
      }
    }
  }
  def hasField(f: FieldType): Boolean = {
    tycon.instance(args) match {
      case u: TypeInstance => {
        /* Avoiding an infinte loop */
        // FIXME: What exception should this be?
        throw new TypeHasNoFieldsException(u)
      }
      case ft: HasFieldsType => {
        ft.hasField(f)
      }
      case u => {
         // FIXME: What exception should this be?
       throw new TypeHasNoFieldsException(u)
      }
    }
  }

}
