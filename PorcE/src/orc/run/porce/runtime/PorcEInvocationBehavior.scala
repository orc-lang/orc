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

import orc.InvocationBehavior
import orc.run.extensions._
import orc.values.Field
import orc.values.sites.Site
import orc.Invoker
import orc.ErrorAccessor
import orc.Accessor
import orc.Handle
import orc.FutureReader
import orc.error.runtime.JavaException
import orc.error.OrcException

/** @author dkitchin
  */
/* The first behavior in the trait list will be tried last */
trait PorcEInvocationBehavior extends InvocationBehavior
  with ErrorOnUndefinedInvocation
  with SupportForPorcEClosure
  with SupportForApply
  with SupportForJavaObjectInvocation
  with SupportForSiteInvocation
