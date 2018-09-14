//
// Sites.scala -- Scala traits ValueMetadata and HasMembersMetadata
// Project OrcScala
//
// Created by dkitchin on Aug, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

trait ValueMetadata {

}

trait HasMembersMetadata extends ValueMetadata {
  /** Return a metadata about a site in a field.
    *
    * A None return value means that this field will not return a method
    * or value with fields.
    */
  def fieldMetadata(f: Field): Option[ValueMetadata] = None
}
