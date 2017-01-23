//
// HasFieldsType.scala -- Scala trait HasFieldsType
// Project OrcScala
//
// Created by dkitchin on Nov 26, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing._

/** A type with fields
  *
  * @author amp
  */
trait HasFieldsType extends Type {
  /** Returns the type of the given field.
    *
    * If there is no such member it will throw.
    */
  @throws(classOf[NoSuchMemberException])
  def getField(f: FieldType): Type

  /** Return true if this type has the given field, otherwise false.
    */
  def hasField(f: FieldType): Boolean
}
