//
// OrcTuple.scala -- Scala class OrcTuple
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

import java.util.Arrays

import orc.{ DirectInvoker, OrcRuntime }
import orc.run.distrib.DOrcMarshalingReplacement
import orc.util.ArrayExtensions.Array1
import orc.values.sites.{ NonBlockingSite, PartialSite, UntypedSite, IllegalArgumentInvoker, InlinableInvoker }

/** @author dkitchin
  */
case class OrcTuple(values: Array[AnyRef]) extends PartialSite with UntypedSite with NonBlockingSite with DOrcMarshalingReplacement with Product {
  assert(values.length > 1)

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = {
    args match {
      case Array1(bi: BigInt) => {
        val size = values.length

        new DirectInvoker {
          def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
            (target, args) match {
              case (t: OrcTuple, Array1(bi: BigInt)) =>
                t.values.length == size
              case _ =>
                false
            }
          }

          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            orc.run.StopWatches.implementation {
              target.asInstanceOf[OrcTuple].values(arguments(0).asInstanceOf[BigInt].intValue())
            }
          }
        }
      }
      case Array1(i: java.lang.Long) => {
        val size = values.length

        new DirectInvoker with InlinableInvoker {
          def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
            (target, args) match {
              case (t: OrcTuple, Array1(bi: java.lang.Long)) =>
                t.values.length == size
              case _ =>
                false
            }
          }

          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            orc.run.StopWatches.implementation {
              target.asInstanceOf[OrcTuple].values(arguments(0).asInstanceOf[java.lang.Long].intValue())
            }
          }
        }
      }
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
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

  override def hashCode() = scala.util.hashing.MurmurHash3.productHash(this)
  override def equals(o: Any): Boolean = o match {
    case o: OrcTuple => Arrays.equals(values, o.values)
    case _ => false
  }

}
