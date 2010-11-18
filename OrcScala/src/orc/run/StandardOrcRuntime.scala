//
// StandardOrcRuntime.scala -- Scala class StandardOrcRuntime
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.OrcRuntime
import orc.run.extensions._

class StandardOrcRuntime extends OrcRuntime
with Orc
with StandardInvocationBehavior
with CappedActorBasedScheduler
with SupportForClasses
with SupportForSynchronousExecution
with SupportForRtimer
with SupportForStdout
with SwappableASTs


/* The first behavior in the trait list will be tried last */
trait StandardInvocationBehavior extends InvocationBehavior
with ErrorOnUndefinedInvocation
with SupportForSiteInvocation
with SupportForJavaObjectInvocation
with SupportForXMLInvocation
