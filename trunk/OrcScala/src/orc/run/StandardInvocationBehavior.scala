//
// StandardInvocationBehavior.scala -- Scala class/trait/object StandardInvocationBehavior
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run

import orc.InvocationBehavior
import orc.run.extensions.ErrorOnUndefinedInvocation
import orc.run.extensions.SupportForSiteInvocation
import orc.run.extensions.SupportForJavaObjectInvocation
import orc.run.extensions.SupportForXMLInvocation

/**
 * 
 *
 * @author dkitchin
 */

/* The first behavior in the trait list will be tried last */
trait StandardInvocationBehavior extends InvocationBehavior
with ErrorOnUndefinedInvocation
with SupportForSiteInvocation
with SupportForJavaObjectInvocation
with SupportForXMLInvocation