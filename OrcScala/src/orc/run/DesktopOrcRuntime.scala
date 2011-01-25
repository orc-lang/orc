//
// DesktopOrcRuntime.scala -- Scala class/trait/object DesktopOrcRuntime
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

import orc.run.extensions.SupportForPrintUsingStdout
import orc.run.extensions.SupportForPromptUsingSwing
import orc.run.extensions.SupportForBrowseUsingSystemBrowser

/**
 * 
 *
 * @author dkitchin
 */
class DesktopOrcRuntime extends StandardOrcRuntime
with SupportForPrintUsingStdout
with SupportForPromptUsingSwing
with SupportForBrowseUsingSystemBrowser