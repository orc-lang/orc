//
// RecordType.scala -- Scala class RecordType
// Project OrcScala
//
// $Id: RecordType.scala 2773 2011-04-20 01:12:36Z jthywissen $
//
// Created by dkitchin on Nov 20, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import scala.collection.immutable.HashMap
import orc.error.compiletime.typing.NoSuchMemberException
import orc.error.compiletime.typing.UncallableTypeException


object EmptyRecordType extends RecordType(Map[String,Type]())

/**
 * 
 * Semantic type of records.
 *
 * @author dkitchin
 */
case class RecordType(entries: Map[String,Type]) extends CallableType with StrictType {

  def this(entries: (String, Type)*) = {
    this(entries.toMap)
  }
  
  override def call(typeArgs: List[Type], argTypes: List[Type]) = {
    argTypes match {
      case List(FieldType(f)) => entries.getOrElse(f, throw new NoSuchMemberException(this, f))
      case _ => {
        (entries get "apply") match {
          case Some(c : CallableType) => c.call(typeArgs, argTypes)
          case _ => throw new UncallableTypeException(this)
        }
      }
    }
  }
  
  override def toString = {
    val ks = entries.keys.toList
    val entryStrings = ks map { k => k + " :: " + entries(k) }
    entryStrings.mkString("{. ",", "," .}")
  }
  
  override def join(that: Type): Type = {
     that match {
       case RecordType(otherEntries) => {
         val joinKeySet = entries.keySet intersect otherEntries.keySet
         val joinEntries =
           for (k <- joinKeySet) yield {
             val t = entries(k)
             val u = otherEntries(k)
             (k, t join u)
           }
         RecordType(HashMap.empty ++ joinEntries)
       }
       case _ => super.join(that)
     }
   }
  
  override def meet(that: Type): Type = {
    that match {
      case RecordType(otherEntries) => {
        val meetKeySet = entries.keySet union otherEntries.keySet
        val meetEntries =
          for (k <- meetKeySet) yield {
            val t = entries.getOrElse(k, Top)
            val u = otherEntries.getOrElse(k, Top)
            (k, t meet u)
          }
        RecordType(HashMap.empty ++ meetEntries)
      }
      case _ => super.join(that)
    }
  }
  
  override def <(that: Type): Boolean = {
    that match {
      case RecordType(otherEntries) => {
        val keys = entries.keySet
        val otherKeys = otherEntries.keySet
        
        (otherKeys subsetOf keys) &&
        (otherKeys forall { k => entries(k) < otherEntries(k) })
      }
      case _ => super.<(that)
    }
  }
  
  override def subst(sigma: Map[TypeVariable, Type]): Type = {
    RecordType(entries mapValues { _ subst sigma })
  }
  
  /* Create a new record type, extending this record type with the bindings of the other record type.
   * When there is overlap, the other record type's bindings override this record type.
   */
  def +(other: RecordType): RecordType = {
    val empty = this.entries.empty
    RecordType(empty ++ this.entries ++ other.entries)
  }

}
