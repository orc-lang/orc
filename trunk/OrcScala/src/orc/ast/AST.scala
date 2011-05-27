//
// AST.scala -- Scala class AST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

import orc.error.OrcException
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.util.parsing.input.{NoPosition, Position, Positional}

trait AST extends Positional {
  
  /**
   * Metadata transfer.
   */
  def ->>[B <: AST](that : B): B = { 
    that.pushDownPosition(this.pos)
    transferOptionalVariableName(this, that)
    that 
  }
  
  /**
   * Metadata-preserving transform.
   */
  def ->[B <: AST](f: this.type => B): B = {
    this ->> f(this)
  }
  
  /**
   * Set source location at this node and propagate
   * the change to any children without source locations.
   */
  def pushDownPosition(p : Position): Unit = {
    this.pos match {
      case NoPosition => {
        this.setPos(p)
        this.subtrees map { _.pushDownPosition(p) }
      }
      case _ => {  }
    }
  }
  
  /**
   * If both AST nodes have an optional variable name,
   * copy that name from this node to the other.
   */
  def transferOptionalVariableName(source: AST, target: AST) {
    (source, target) match {
      case (x: hasOptionalVariableName, y: hasOptionalVariableName) => {
        y.optionalVariableName = x.optionalVariableName
      }
      case _ => {}
    }
    
    
  }
  
  /**
   * All AST node children of this node, as a single list
   */
  def subtrees: Iterable[AST] = {

    def flattenAstNodes(x: Any, flatList: Buffer[AST]) {
      def isGood(y: Any): Boolean = y match {
        case _: AST => true
        case i: Traversable[_] => i.forall(isGood(_))
        case _ => false
      }
      def traverseAndAdd(z: Any) {
        z match {
          case a: AST => flatList += a
          case i: Traversable[_] => i.foreach(traverseAndAdd(_))
        }
      }
      if (isGood(x)) traverseAndAdd(x)
    }

    val goodKids = new ArrayBuffer[AST]();
    for (f <- this.productIterator) {
      if (f.isInstanceOf[AST] || f.isInstanceOf[scala.collection.Iterable[_]]) {
        flattenAstNodes(f, goodKids);
      }
    }
    goodKids.toList
  }
  def productIterator: Iterator[Any] //Subclasses that are case classes will supply automatically
}


trait OrcSyntaxConvertible {
  def toOrcSyntax: String
}


trait hasOptionalVariableName extends AST {
  var optionalVariableName: Option[String] = None
}
