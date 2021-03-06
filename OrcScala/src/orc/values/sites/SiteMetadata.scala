//
// SiteMetadata.scala -- Scala trait SiteMetadata and associated ADTs
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.values.ValueMetadata

sealed trait Delay {
  def max(o: Delay): Delay
  def min(o: Delay): Delay
}
object Delay {
  case object NonBlocking extends Delay {
    def max(o: Delay): Delay = o
    def min(o: Delay): Delay = this
  }
  case object Blocking extends Delay {
    def max(o: Delay): Delay = o match {
      case Forever => o
      case _ => this
    }
    def min(o: Delay): Delay = o match {
      case NonBlocking => o
      case _ => this
    }
  }
  case object Forever extends Delay {
    def max(o: Delay): Delay = this
    def min(o: Delay): Delay = o
  }
}

sealed trait Effects {
  def max(o: Effects): Effects
  def min(o: Effects): Effects

  def <=(o: Effects): Boolean = {
    (this max o) == o
  }
}
object Effects {
  case object None extends Effects {
    def max(o: Effects): Effects = o
    def min(o: Effects): Effects = this
  }
  case object BeforePub extends Effects {
    def max(o: Effects): Effects = o match {
      case Anytime => o
      case _ => this
    }
    def min(o: Effects): Effects = o match {
      case None => o
      case _ => this
    }
  }
  case object Anytime extends Effects {
    def max(o: Effects): Effects = this
    def min(o: Effects): Effects = o
  }
}

trait SiteMetadata extends ValueMetadata {
  def name: String = orc.util.GetScalaTypeName(this)

  def publications: Range = Range(0, None)
  def timeToPublish: Delay = Delay.Blocking
  def timeToHalt: Delay = Delay.Blocking
  def effects: Effects = Effects.Anytime

  /** Return a metadata about a site returned from a call to this site with args.
    *
    * A None argument says that any value may be passed in this position at runtime.
    *
    * A None return value means that this call will not return a method
    * or value with fields.
    */
  def publicationMetadata(args: List[Option[AnyRef]]): Option[ValueMetadata] = None
}

trait DirectSiteMetadata extends SiteMetadata {
}

