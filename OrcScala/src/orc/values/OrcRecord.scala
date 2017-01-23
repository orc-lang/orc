//
// OrcRecord.scala -- Scala class OrcRecord
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

import orc.values.sites.UntypedSite
import orc.values.sites.PartialSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.NoSuchMemberException
import scala.collection.immutable.Map
import orc.run.core.BoundValue
import orc.run.core.Binding
import orc.values.sites.NonBlockingSite

/** @author dkitchin
  */
case class OrcRecord(entries: Map[String, AnyRef]) extends HasMembers {

  def this(entries: (String, AnyRef)*) = {
    this(entries.toMap)
  }

  def this(entries: List[(String, AnyRef)]) = this(entries.toMap)

  override def toOrcSyntax() = {
    val formattedEntries =
      for ((s, v) <- entries) yield {
        s + " = " + Format.formatValue(v)
      }
    val contents =
      formattedEntries.toList match {
        case Nil => ""
        case l => l reduceRight { _ + ", " + _ }
      }
    "{. " + contents + " .}"
  }

  /* Create a new record, extending this record with the bindings of the other record.
   * When there is overlap, the other record's bindings override this record.
   */
  def +(other: OrcRecord): OrcRecord = {
    val empty = this.entries.empty
    OrcRecord(empty ++ this.entries ++ other.entries)
  }

  /* Aliased for easier use in Java code */
  def extendWith(other: OrcRecord): OrcRecord = this + other

  def getMember(field: Field) = {
    entries.get(field.field) match {
      case Some(v) => BoundValue(v)
      case None => throw new NoSuchMemberException(this, "this record")
    }
  }

  override def hasMember(field: Field): Boolean = {
    entries.contains(field.field)
  }
}
