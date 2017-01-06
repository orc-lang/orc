//
// ErrorOnUndefinedInvocation.scala -- Scala trait ErrorOnUndefinedInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.InvocationBehavior
import orc.Handle
import orc.values.Format
import orc.error.runtime.UncallableValueException

/** @author dkitchin
  */
trait ErrorOnUndefinedInvocation extends InvocationBehavior {
  /* This replaces the default behavior because it does not call super */
  override def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) {
    val error = "You can't call the " + (if (v != null) v.getClass().toString() else "null") + " \" " + Format.formatValue(v) + " \""
    h !! new UncallableValueException(error)
  }
}
