//
// Variance.scala -- Scala trait Variance and its child object
// Project OrcScala
//
// Created by dkitchin on Nov 27, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import scala.language.implicitConversions

/** Variances of type variables and type constructor arguments.
  *
  * @author dkitchin
  */
trait Variance {
  def apply(that: Variance): Variance
  def &(that: Variance): Variance
}

object Variance {

  class RichVarianceList(variances: List[Variance]) {
    lazy val combined = {
      variances.foldLeft(Constant: Variance)({ (u: Variance, v: Variance) => u & v })
    }
  }

  implicit def enrichVarianceList(variances: List[Variance]): RichVarianceList = new RichVarianceList(variances)

}

case object Covariant extends Variance {
  def apply(that: Variance) = that
  def &(that: Variance) = {
    that match {
      case Constant | Covariant => this
      case _ => Invariant
    }
  }
}

case object Contravariant extends Variance {
  def apply(that: Variance) = {
    that match {
      case Covariant => Contravariant
      case Contravariant => Covariant
      case v => v
    }
  }
  def &(that: Variance) = {
    that match {
      case Constant | Contravariant => this
      case _ => Invariant
    }
  }
}

case object Invariant extends Variance {
  def apply(that: Variance) = this
  def &(that: Variance) = this
}

case object Constant extends Variance {
  def apply(that: Variance) = that
  def &(that: Variance) = that
}
