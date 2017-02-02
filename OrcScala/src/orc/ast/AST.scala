//
// AST.scala -- Scala class AST
// Project OrcScala
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

import scala.collection.mutable.{ ArrayBuffer, Buffer }

import orc.compile.parse.OrcSourceRange

trait Positioned {
  /** The source position (range) of this AST node, initially set to undefined. */
  var sourceTextRange: Option[OrcSourceRange] = None

  /** If current source range is undefined, update it with given position `newRange`
    * @return  the object itself
    */
  def fillSourceTextRange(newRange: Option[OrcSourceRange]): this.type = {
    if (!sourceTextRange.isDefined) sourceTextRange = newRange
    this
  }
}

trait AST extends Positioned {
  /** Metadata transfer.
    */
  def ->>[B <: AST](that: B): B = {
    that.pushDownPosition(this.sourceTextRange)
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
  def aggregatePosWith[B <: AST](that: B): this.type = {
    (this.sourceTextRange.isDefined, that.sourceTextRange.isDefined) match {
      case (false, false) => {}
      case (false, true) => pushDownPosition(that.sourceTextRange)
      case (true, false) => pushDownPosition(this.sourceTextRange)
      case (true, true) => {
        assert(this.sourceTextRange.get.start.resource == that.sourceTextRange.get.start.resource && this.sourceTextRange.get.end.resource == that.sourceTextRange.get.end.resource, "aggregatePosWith with differing resources")
        val minStart = if (this.sourceTextRange.get.start <= that.sourceTextRange.get.start) this.sourceTextRange.get.start else that.sourceTextRange.get.start
        val maxEnd = if (this.sourceTextRange.get.end >= that.sourceTextRange.get.end) this.sourceTextRange.get.start else that.sourceTextRange.get.start
        pushDownPosition(Some(new OrcSourceRange((minStart, maxEnd))))
      }
    }
    this
  }

  /** Set source location at this node and propagate
    * the change to any children without source locations.
    */
  def pushDownPosition(p: Option[OrcSourceRange]): this.type = {
    if (!this.sourceTextRange.isDefined) {
      this.fillSourceTextRange(p)
      this.subtrees map { _.pushDownPosition(p) }
    }
    this
  }

  /** If both AST nodes have an optional variable name,
    * copy that name from this node to the other.
    */
  def transferOptionalVariableName(source: AST, target: AST) {
    (source, target) match {
      case (x: hasOptionalVariableName, y: hasOptionalVariableName) if x.optionalVariableName.isDefined => {
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
    Console.println(prefix + getClass().getCanonicalName() + " at " + sourceTextRange + ": " + toString())
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

trait hasAutomaticVariableName extends hasOptionalVariableName {
  def autoName(namePrefix: String): Unit = {
    optionalVariableName = optionalVariableName match {
      case Some(n) => Some(n)
      case None =>
        Some(hasAutomaticVariableName.getNextVariableName(namePrefix))
    }
  }
}

object hasAutomaticVariableName {
  private var nextVar: Int = 0
  def getNextVariableName(s: String): String = synchronized {
    nextVar += 1
    s"`$s$nextVar"
  }
}
