//
// Ternary.scala -- Scala class Ternary
// Project OrcScala
//
// Created by amp on Jun 28, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

sealed abstract class Ternary extends Product with Serializable {
  def isTrue: Boolean
  def isFalse: Boolean
  def isUnknown: Boolean

  def unary_! : Ternary
  def &&(o: Ternary): Ternary
  def ||(o: Ternary): Ternary
}

object Ternary {
  import scala.language.implicitConversions
  
  @inline
  implicit def boolean2Ternary(b: Boolean) = {
    if(b)
      TTrue
    else
      TFalse
  }
}

case object TTrue extends Ternary {
  def isTrue: Boolean = true
  def isFalse: Boolean = false
  def isUnknown: Boolean = false

  def unary_! : Ternary = TFalse
  def &&(o: Ternary): Ternary = o
  def ||(o: Ternary): Ternary = this
  
  override def toString() = "true"
}

case object TUnknown extends Ternary {
  def isTrue: Boolean = false
  def isFalse: Boolean = false
  def isUnknown: Boolean = true

  def unary_! : Ternary = TUnknown
  def &&(o: Ternary): Ternary = o match {
    case TFalse => TFalse
    case _ => TUnknown
  }
  def ||(o: Ternary): Ternary = o match {
    case TTrue => TTrue
    case _ => TUnknown
  } 
  
  override def toString() = "unknown"
}

case object TFalse extends Ternary {
  def isTrue: Boolean = false
  def isFalse: Boolean = true
  def isUnknown: Boolean = false

  def unary_! : Ternary = TTrue
  def &&(o: Ternary): Ternary = this
  def ||(o: Ternary): Ternary = o
  
  override def toString() = "false"
}

