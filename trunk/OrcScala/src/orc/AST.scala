//
// AST.scala -- Scala class AST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import scala.util.parsing.input.Positional


class PositionalException(msg: String) extends Exception(msg) with Positional

abstract class AST extends Positional {

  def ->[B <: AST](f: this.type => B): B = {
      val location = this.pos
      val result = f(this)
      result.pos = location
      result
  }
  
  // Location transfer.
  // x ->> y  is equivalent to  x -> (_ => y)
  def ->>[B <: AST](that : B): B = { that.pos = this.pos ; that }

  def !!(exn : PositionalException): Nothing = {
      exn.pos = this.pos
      throw exn
  }
  
  
  // Remove this overloading to uncover uses of !! that do not carry a specific exception type
  def !!(msg : String): Nothing = { 
    val exn = new PositionalException(msg)
    this.!!(exn)
  }
}
