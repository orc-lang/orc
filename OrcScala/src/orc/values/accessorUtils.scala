//
// accessorUtils.scala -- Utilities for implementing Accessors.
// Project OrcScala
//
// Created by amp on Aug, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

import orc.error.runtime.NoSuchMemberException
import orc.ErrorAccessor
import orc.error.runtime.DoesNotHaveMembersException

/** A accessor sentinel representing the fact that unknownMember does not exist on the given value.
  */
case class NoSuchMemberAccessor(target: AnyRef, unknownMember: String) extends ErrorAccessor {
  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    throw new NoSuchMemberException(target, unknownMember)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

case class NoSuchMemberOfClassAccessor(cls: Class[_], unknownMember: String) extends ErrorAccessor {
  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    throw new NoSuchMemberException(target, unknownMember)
  }

  def canGet(target: AnyRef): Boolean = {
    cls.isInstance(target)
  }
}




/** A accessor sentinel representing the fact that the value does not have members.
  */
case class DoesNotHaveMembersAccessor(target: AnyRef) extends ErrorAccessor {
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef = {
    throw new DoesNotHaveMembersException(target)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

/** A accessor sentinel representing the fact that the class does not have members.
  */
case class ClassDoesNotHaveMembersAccessor(targetCls: Class[_]) extends ErrorAccessor {
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef = {
    throw new DoesNotHaveMembersException(target)
  }

  def canGet(target: AnyRef): Boolean = {
    targetCls.isInstance(target)
  }
}
