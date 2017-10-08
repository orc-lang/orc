//
// OrcTuple.scala -- Scala class OrcTuple
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

import orc.{ ClassDoesNotHaveMembersAccessor, IllegalArgumentInvoker, OnlyDirectInvoker, OrcRuntime }
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException, TupleIndexOutOfBoundsException }
import orc.run.distrib.DOrcMarshalingReplacement
import orc.util.ArrayExtensions.Array1
import orc.values.sites.{ AccessorValue, InvokerMethod, NonBlockingSite, PartialSite, UntypedSite }
import java.util.Arrays

/** @author dkitchin
  */
case class OrcTuple(values: Array[AnyRef]) extends InvokerMethod with AccessorValue with PartialSite with UntypedSite with NonBlockingSite with DOrcMarshalingReplacement with Product {
  assert(values.length > 1)
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = {
    args match {
      case Array1(bi: BigInt) => {
        val size = values.length
        
        new OnlyDirectInvoker {
          def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
            (target, args) match { 
              case (t: OrcTuple, Array1(bi: BigInt)) =>
                t.values.length == size
              case _ =>
                false
            }
          }
        
          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            target.asInstanceOf[OrcTuple].values(arguments(0).asInstanceOf[BigInt].intValue())
          }
        }
      }
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
  }
  
  def getAccessor(runtime: OrcRuntime, f: Field) = {
    ClassDoesNotHaveMembersAccessor(classOf[OrcTuple])
  }

  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array1(bi: BigInt) => {
        val i: Int = bi.intValue
        // TODO: PERFORMANCE: It would probably be faster to let the array reference throw IndexOutOfBounds. The JVM knows how to optimize that better I suspect.
        if (0 <= i && i < values.size) { Some(values(i)) }
        else { throw new TupleIndexOutOfBoundsException(i) }
      }
      case Array1(a) => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  override def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values exists marshalValueWouldReplace

  override def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef =
    OrcTuple(values map marshaler)

  override def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values exists unmarshalValueWouldReplace

  override def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef): AnyRef =
    OrcTuple(values map unmarshaler)

  override def toOrcSyntax() = "(" + Format.formatSequence(values) + ")"

  override def productElement(n: Int): Any = values(n)

  override def productArity: Int = values.length

  override def hashCode() = Arrays.hashCode(values)
  override def equals(o: Any): Boolean = o match {
    case o: OrcTuple => Arrays.equals(values, o.values)
    case _ => false
  }

}
