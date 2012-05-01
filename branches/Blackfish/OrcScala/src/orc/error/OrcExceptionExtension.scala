//
// OrcExceptionExtension.scala -- Scala class ExtendedOrcException and object OrcExceptionExtension
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 16, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error

/** Implicitly extend Orc exceptions with an additional method to set their location.
  * This is in Scala so that the exception's specific type can be maintained through the call.
  * Plus, this is a hook to add additional functionality to exceptions later.
  *
  * @author dkitchin
  */
object OrcExceptionExtension {

  implicit def extendOrcException[E <: OrcException](e: E): ExtendedOrcException[E] = ExtendedOrcException[E](e)

}

case class ExtendedOrcException[E <: OrcException](e: E) {

  def at(ast: orc.ast.AST): E = {
    e.setPosition(ast.pos);
    e
  }

}
