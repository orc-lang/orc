//
// AST.scala -- Scala class AST
// Project OrcScala
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

import scala.collection.mutable.{ ArrayBuffer, Buffer }

import orc.compile.parse.OrcSourceRange
import scala.util.matching.Regex

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

trait ASTForSwivel extends Positioned {
  /** Metadata transfer.
    */
  def ->>[B <: ASTForSwivel](that: B): B = {
    that.pushDownPosition(this.sourceTextRange)
    transferOptionalVariableName(this, that)
    that
  }

  /** Metadata-preserving transform.
    */
  def ->[B <: ASTForSwivel](f: this.type => B): B = {
    this ->> f(this)
  }

  def subtrees: Iterable[ASTForSwivel]
  
  /** If the argument has an earlier file position than
    * this AST node, reassign this node's position.
    *
    * If either position is undefined, choose the defined one.
    */
  def aggregatePosWith[B <: ASTForSwivel](that: B): this.type = {
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
  def transferOptionalVariableName(source: ASTForSwivel, target: ASTForSwivel) {
    (source, target) match {
      case (x: hasOptionalVariableName, y: hasOptionalVariableName) if x.optionalVariableName.isDefined && y.optionalVariableName.isEmpty => {
        y.optionalVariableName = x.optionalVariableName
      }
      case _ => {}
    }

  }

  def equalsIgnoreChildren(that: AnyRef): Boolean = {
    if (this eq that) {
      return true
    } else if (this.getClass == that.getClass) {
      val thatT = that.asInstanceOf[this.type]
      def p[A, B](a: A, b: B): Boolean = {
        ((a.isInstanceOf[ASTForSwivel] && b.isInstanceOf[ASTForSwivel]) || (a.isInstanceOf[scala.collection.Iterable[_]] && b.isInstanceOf[scala.collection.Iterable[_]]) || a.equals(b))
      }
      this.productIterator.toSeq.corresponds(thatT.productIterator.toSeq)(p)
    } else {
      return false
    }
  }

  def productIterator: Iterator[Any] //Subclasses that are case classes will supply automatically

}

trait AST extends ASTForSwivel {
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

  def dump(prefix: String = ""): this.type = {
    Console.println(prefix + getClass().getCanonicalName() + " at " + sourceTextRange + ": " + toString())
    subtrees foreach { _.dump(prefix + "  ") }
    this
  }
}


trait OrcSyntaxConvertible {
  def toOrcSyntax: String
}

trait hasOptionalVariableName extends ASTForSwivel {
  var optionalVariableName: Option[String] = None
}

object hasOptionalVariableName {
  implicit class VariableNameInterpolator(sc: StringContext) {
    import sc._
    import scala.StringContext._
    import orc.util.StringExtension._
    
    def id(args: Any*): String = {
      checkLengths(args)
      val pi = parts.iterator
      val ai = args.iterator
      val bldr = new java.lang.StringBuilder(treatEscapes(pi.next()))
      while (ai.hasNext) {
        val p = pi.next()
        val (p1, useVariableName, truncate) = p match {
          case p if p.startsWith("@") => (p.substring(1), true, true)
          case p if p.startsWith("~") => (p.substring(1), false, false)
          case p => (p, false, true)
        }
        val v = ai.next match {
          case v: hasOptionalVariableName if useVariableName && v.optionalVariableName.isDefined => v.optionalVariableName.get
          case c: OrcSyntaxConvertible => c.toOrcSyntax
          //case p: Positioned if p.sourceTextRange.isDefined => p.sourceTextRange.get.content
          case o => o
        }
        val s = cleanIdentifierString(v.toString)
        bldr append (if (truncate) s.removeMiddleTo(40, "…") else s)
        bldr append treatEscapes(p1)
      }
      getNextVariableName(bldr.toString)
    }
    
  }
  
  def unusedVariable = id"_"
  
  private val BAD_CHARACTERS = new Regex("[^`a-zA-Z0-9'…]+(?=[`a-zA-Z0-9'…])")
  private val BAD_CHARACTERS_ENDS = new Regex("^[^`a-zA-Z0-9'…]+|[^`a-zA-Z0-9'…]+$")
  
  private def cleanIdentifierString(s: String) = BAD_CHARACTERS_ENDS.replaceAllIn(BAD_CHARACTERS.replaceAllIn(s, "_"), "")
  
  private val nextVarMap: collection.mutable.Map[String, Int] = collection.mutable.Map()
  private val generateUniqueVariableNames = false
  
  private def getNextVariableName(s: String): String = {
    if (generateUniqueVariableNames) synchronized {
      val nextVar = nextVarMap.getOrElse(s, 0)
      nextVarMap += s -> (nextVar + 1)
      s"${if (s.startsWith("`")) "" else "`"}$s${if (s.endsWith("_")) "" else "_"}$nextVar"
    } else s
  }
}
