//
// ArrayProxy.scala -- Scala class/trait/object ArrayProxy
// Project OrcScala
//
// $Id$
//
// Created by srosario on Jun 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

import orc.values.Value
import orc.values.Literal
import orc.values.Field
import orc.values.Signal
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

/**
 * @author srosario
 */
class ArrayProxy[E](values: Array[E]) extends TotalSite with UntypedSite {
  
  override def evaluate(args: List[Value]) = {
    args match {
      case List(Literal(i: Int)) => new RefSite(i)
      case List(Literal(i: BigInt)) => evaluate(List(Literal(i.toInt)))
      case List(Field("slice")) => {
        new TotalSite with UntypedSite {
          def evaluate(as: List[Value]) = {
            as match {
              case List(Literal(from: Int), Literal(until: Int)) => Literal(values.slice(from, until))
              case List(Literal(from: BigInt), Literal(until:  BigInt)) => Literal(values.slice(from.toInt, until.toInt))
              case List(a,b) => throw new ArgumentTypeMismatchException("Expected Int, found"
                  +a.getClass().toString()+"; Expected Int, found "+b.getClass().toString())
              case _ => throw new ArityMismatchException(2, args.size)
            }
          }
        }
      }
      case List(Field("length")) => {
        new TotalSite with UntypedSite {
          def evaluate(as: List[Value]) = {
            as match {
              case List() => Literal(values.length)
              case _ => throw new ArityMismatchException(0, args.size)
            }
          }
        }
      }
      case List(Field("fill")) => {
        new TotalSite with UntypedSite {
          def evaluate(as: List[Value]) = {
            as match {
              case List(e: E) => {
                for (i <- (0 until values.length-1)) values(i) = e
                Signal
              }
              case _ => throw new ArityMismatchException(0, args.size)
            }
          }
        }
      }
      case List(Literal(a: AnyRef)) => throw new ArgumentTypeMismatchException(0, "Integer", "Literal("+a.getClass.toString()+")")
      case List(a) => throw new ArgumentTypeMismatchException(0, "Integer", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  class RefSite(index: Int) extends TotalSite with UntypedSite {
    
    def evaluate(args: List[Value]) = {
      args match {
        case List(Field("read")) => new TotalSite with UntypedSite {
         def evaluate(as: List[Value]) = {
           as match {
             case List() => Literal(values(index))
             case _ => throw new ArityMismatchException(0, args.size)
           }
         }
        }
        case List(Field("write")) => new TotalSite with UntypedSite {
          def evaluate(as: List[Value]) = {
            as match {
              case List(Literal(e: E)) => {
                values(index) = e
                Signal  
              }
              case _ => throw new ArityMismatchException(0, args.size)
            }
          }
        }
        case _ => throw new ArityMismatchException(1, args.size)
      }
    }
  }  
  
}
