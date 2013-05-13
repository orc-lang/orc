//
// AST.scala -- Scala class AST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

import orc.error.OrcException
import scala.collection.mutable.{ ArrayBuffer, Buffer }
import scala.util.parsing.input.{ NoPosition, Position, Positional }

trait AST extends Positional {

  /** Metadata transfer.
    */
  def ->>[B <: AST](that: B): B = {
    that.pushDownPosition(this.pos)
    transferOptionalVariableName(this, that)
    that
  }

  /** Metadata-preserving transform.
    */
  def ->[B <: AST](f: this.type => B): B = {
    this ->> f(this)
  }

  /** If the argument has an earlier file position than
    * this AST node, reassign this node's position.
    *
    * If either position is undefined, choose the defined one.
    */
  def takeEarlierPos[B <: AST](that: B): this.type = {
    (this.pos, that.pos) match {
      case (NoPosition, NoPosition) => {}
      case (NoPosition, p) => this.pushDownPosition(p)
      case (p, NoPosition) => this.pushDownPosition(p)
      case (thisp, thatp) => if (thatp < thisp) { this.pushDownPosition(thatp) }
    }
    this
  }

  /** Set source location at this node and propagate
    * the change to any children without source locations.
    */
  def pushDownPosition(p: Position): Unit = {
    this.pos match {
      case NoPosition => {
        this.setPos(p)
        this.subtrees map { _.pushDownPosition(p) }
      }
      case _ => {}
    }
  }

  /** If both AST nodes have an optional variable name,
    * copy that name from this node to the other.
    */
  def transferOptionalVariableName(source: AST, target: AST) {
    (source, target) match {
      case (x: hasOptionalVariableName, y: hasOptionalVariableName) => {
        if( x.optionalVariableName.isDefined )
          y.optionalVariableName = x.optionalVariableName
      }
      case _ => {}
    }

  }

  /** All AST node children of this node, as a single list
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
    goodKids
  }

  def equalsIgnoreChildren(that: AnyRef): Boolean = {
    if (this eq that) {
      return true
    } else if (this.getClass == that.getClass) {
      val thatT = that.asInstanceOf[this.type]
      def p[A, B](a: A, b: B): Boolean = {
        ((a.isInstanceOf[AST] && b.isInstanceOf[AST]) || (a.isInstanceOf[scala.collection.Iterable[_]] && b.isInstanceOf[scala.collection.Iterable[_]]) || a.equals(b))
      }
      this.productIterator.toSeq.corresponds(thatT.productIterator.toSeq)(p)
    } else {
      return false
    }
  }

  def productIterator: Iterator[Any] //Subclasses that are case classes will supply automatically

  def dump(prefix: String = ""): this.type = {
    Console.println(prefix + getClass().getCanonicalName() + " at " + pos + ": " + toString())
    subtrees foreach { _.dump(prefix + "  ") }
    this
  }

}

trait OrcSyntaxConvertible {
  def toOrcSyntax: String
}

trait hasOptionalVariableName extends AST {
  var optionalVariableName: Option[String] = None
}
