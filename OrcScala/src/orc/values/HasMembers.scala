//
// HasMembers.scala -- Scala trait HasMembers
// Project OrcScala
//
// Created by amp on Jan 16, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

import orc.{ Accessor, OrcRuntime }
import orc.values.HasMembersMetadata

/** The common interface for all Orc values that have members (fields).
  *
  * @author amp
  */
trait HasMembers extends OrcValue with HasMembersMetadata {
  /** Get an accessor which extracts a given field value from this target.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Accessor for the given classes or an
    *         instance of AccessorError if there is no accessor.
    *
    * @see NoSuchMemberAccessor, DoesNotHaveMembersAccessor
    */
  def getAccessor(runtime: OrcRuntime, field: Field): Accessor
}
