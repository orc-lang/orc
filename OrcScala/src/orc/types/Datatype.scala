//
// Datatype.scala -- Scala trait Datatype and child classes
// Project OrcScala
//
// Created by dkitchin on Dec 5, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

trait Datatype {
  var constructorTypes: Option[List[RecordType]] = None
  var optionalDatatypeName: Option[String] = None
  override def toString = optionalDatatypeName.getOrElse("`datatype")
}

class MonomorphicDatatype extends Type with Datatype
class PolymorphicDatatype(val variances: List[Variance]) extends TypeConstructor with Datatype
