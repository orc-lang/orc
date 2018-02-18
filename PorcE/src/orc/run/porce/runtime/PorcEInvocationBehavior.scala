//
// StandardInvocationBehavior.scala -- Scala trait StandardInvocationBehavior
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porce.runtime

import orc.{ InvocationBehavior, OrcRuntime }
import orc.run.extensions.{ ErrorOnUndefinedInvocation, SupportForJavaObjectInvocation, SupportForSiteInvocation }

/** @author dkitchin
  */
/* The first behavior in the trait list will be tried last */
trait PorcEInvocationBehavior extends InvocationBehavior
  with ErrorOnUndefinedInvocation
  with SupportForJavaObjectInvocation
  with SupportForSiteInvocation {
  this: OrcRuntime =>
}
