//
// PrimitiveForms.scala -- Scala object PrimitiveForms
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import scala.language.reflectiveCalls

import orc.ast.oil.named._
import orc.ast.oil.named.Conversions._
import orc.lib.builtin._
import orc.lib.builtin.structured._
import orc.ast.oil._
import orc.ast.ext
import orc.values.{ Signal, Field }
import orc.values.sites.Site


object PrimitiveForms {

  def nullaryBuiltinCall(s: Site)() = Call(Constant(s), Nil, None)
  def unaryBuiltinCall(s: Site)(a: Argument) = Call(Constant(s), List(a), None)
  def binaryBuiltinCall(s: Site)(a: Argument, b: Argument) = Call(Constant(s), List(a, b), None)

  val callIft = unaryBuiltinCall(Ift) _
  val callIff = unaryBuiltinCall(Iff) _
  val callEq = binaryBuiltinCall(Eq) _

  val callCons = binaryBuiltinCall(ConsConstructor) _
  val callIsCons = unaryBuiltinCall(ConsExtractor) _
  val callNil = nullaryBuiltinCall(NilConstructor) _
  val callIsNil = unaryBuiltinCall(NilExtractor) _

  val callSome = unaryBuiltinCall(SomeConstructor) _
  val callIsSome = unaryBuiltinCall(SomeExtractor) _
  val callNone = nullaryBuiltinCall(NoneConstructor) _
  val callIsNone = unaryBuiltinCall(NoneExtractor) _
  val callTupleArityChecker = binaryBuiltinCall(TupleArityChecker) _

  def callRecordMatcher(a: Argument, shape: List[String]) = {
    val shapeArgs = shape map { s: String => Constant(Field(s)) }
    Call(Constant(RecordMatcher), a :: shapeArgs, None)
  }

  def makeUnapply(constructor: Argument, a: Argument) = {
    val extractor = new BoundVar()
    val getExtractor = Call(Constant(ProjectUnapply), List(constructor), None)
    val invokeExtractor = Call(extractor, List(a), None)
    getExtractor > extractor > invokeExtractor
  }

  def makeNth(a: Argument, i: Int) = Call(Constant(GetElem), List(a, Constant(BigInt(i))), None)

  def makeLet(args: List[Argument]): Expression = {
    args match {
      case Nil => Constant(Signal)
      case List(a) => a
      case _ => makeTuple(args)
    }
  }

  def makeTuple(elements: List[Argument]) = Call(Constant(TupleConstructor), elements, None)

  def makeList(elements: List[Argument]) = {
    val nil: Expression = callNil()
    def cons(h: Argument, t: Expression): Expression = {
      val y = new BoundVar()
      t > y > callCons(h, y)
    }
    elements.foldRight(nil)(cons)
  }

  def makeRecord(tuples: List[Argument]) = Call(Constant(RecordConstructor), tuples, None)

  def makeDatatype(declaredVariant: BoundTypevar, variantArity: Int, constructors: List[ext.Constructor], translator: Translator) = {
    val datatypeSite = Constant(DatatypeBuilder)
    val datatypePairs =
      for (ext.Constructor(name, types) <- constructors) yield {
        val cname = Constant(name)
        val carity = Constant(BigInt(types.size))
        makeTuple(List(cname, carity))
      }
    val typeParameter =
      if (variantArity > 0) {
        TypeApplication(declaredVariant, List.fill(variantArity)(Top()))
      } else {
        declaredVariant
      }
    unfold(datatypePairs, { Call(datatypeSite, _, Some(List(typeParameter))) })
  }

  def makeConditional(test: Expression, trueBranch: Expression, falseBranch: Expression) = {
    val b = new BoundVar()
    val nb = new BoundVar()
    ((callIft(b) >> trueBranch) || (callIff(b) >> falseBranch)) < b < test
  }

  def makeConditionalFalseOnHalt(test: Expression, trueBranch: Expression, falseBranch: Expression) = {
    val b = new BoundVar()
    val nb = new BoundVar()
    ((callIft(b) >> trueBranch) < b < test) ow falseBranch
  }

  /*
   * Return a composite expression with the following behavior:
   * 
   * If source publishes a value, bind that value to x, and then
   * execute target.
   * 
   * If source halts without publishing a value, execute fail.
   * 
   */
  def makeMatch(source: Expression, x: BoundVar, target: Expression, fail: Expression) = {
    fail match {
      case Stop() => source > x > target
      case _ => {
        val y = new BoundVar()
        val z = new BoundVar()
        (
          (source > z > callSome(z)) ow (callNone())) > y >
          (
            (callIsSome(y) > x > target) || (callIsNone(y) >> fail))
      }
    }
  }

}
