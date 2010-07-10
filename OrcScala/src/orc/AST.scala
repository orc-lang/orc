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

import orc.error.OrcException
import scala.util.parsing.input.Positional
import scala.util.parsing.input.Position
import scala.util.parsing.input.NoPosition

trait AST extends Positional {

  // Location-preserving transform
  def ->[B <: AST](f: this.type => B): B = {
      val location = this.pos
      val result = f(this)
      result.pushDownPosition(location)
      result
  }
  
  // Location transfer.
  // x ->> y  is equivalent to  x -> (_ => y)
  def ->>[B <: AST](that : B): B = { that.pushDownPosition(this.pos); that }

  def !!(exn : OrcException): Nothing = {
      exn.setPosition(this.pos)
      throw exn
  }
  
  // TODO: remove this overloading to uncover uses of !! that do not carry a specific exception type
  def !!(msg : String): Nothing = { 
    val exn = new OrcException(msg)
    this !! exn
  }
  
  // Emit a warning
  def !?(msg : String): Unit = { 
    //FIXME: This should use the compile message recorder -- this is broken for GUI or Web Orc versions 
    Console.err.println("Warning " + this.pos + ": " + msg)
  }
  
  // Set source location at this node and propagate
  // the change to any children without source locations.
  def pushDownPosition(p : Position): Unit = {
    this.pos match {
      case NoPosition => {
        this.setPos(p)
        this.subtrees map { _.pushDownPosition(p) }
      }
      case _ => {  }
    }
  }
  
  // Used to propagate source location information
  // Currently only implemented for OIL
  val subtrees: List[AST] = Nil
  
  
}
