//
// HasFields.scala -- Scala trait HasFields
// Project OrcScala
//
// Created by amp on Jan 16, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

import orc.error.runtime.NoSuchMemberException
import orc.run.core.Binding
import orc.values.sites.AccessorValue
import orc.Accessor
import orc.error.runtime.DoesNotHaveMembersException

// TODO: This should replace OrcObjectInterface and HasFields

/** The common interface for all Orc values that have members (fields).
  *
  * This supports leniently computed members. However, there are several
  * implicit assumptions that users of this interface can make:
  * # Members are monotonic (once they are resolved they stay resolved)
  * # Members are immutable (they will never change once resolved)
  * # The set of members on a value is will not change once the object is created,
  * so the `hasMember(f)` will always return the same value for a given `f`.
  *
  * @author amp
  */
@deprecated("Implement AccessorValue", "3.0")
trait HasMembers extends OrcValue with AccessorValue {
  /** Get the binding of a member.
    *
    * The returned value may be an orc.Future.
    */
  @throws[NoSuchMemberException]("If member does not exist")
  def getMember(f: Field): AnyRef

  /** Return true iff this object has the field `f`.
    *
    * If `hasMember(f)` is true, then `getMember(f)` will never throw NoSuchMemberException.
    *
    * This method should be overriden, if possible, to improve performance and avoid exception handling during the check.
    */
  def hasMember(f: Field): Boolean = {
    try {
      getMember(f)
      true
    } catch {
      case _: NoSuchMemberException =>
        false
    }
  }
  
  @throws[NoSuchMemberException]
  def getAccessor(field: Field): Accessor = {
    if(hasMember(field)) {
      new HasMemberAccessor(field)
    } else {
      throw new NoSuchMemberException(this, field.name)
    }
  }
}

final class HasMemberAccessor(field: Field) extends Accessor {
  def canGet(target: AnyRef): Boolean = {
    target.isInstanceOf[HasMembers]
  }

  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    target.asInstanceOf[HasMembers].getMember(field)
  }
}