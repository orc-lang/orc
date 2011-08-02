//
// MakeGuard.scala -- Scala class MakeGuard
// Project OrcScala
//
// $Id: MakeGuard.scala 2843 2011-05-24 20:56:21Z dkitchin $
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.Handle
import orc.values.sites.TotalSite0
import orc.values.sites.Site0
import orc.values.sites.TypedSite
import orc.values.sites.UntypedSite
import orc.types.SimpleFunctionType
import java.util.concurrent.atomic.AtomicBoolean


object MakeGuard extends TotalSite0 with TypedSite {
  override def name = "Guard"
  def eval() = new GuardInstance()
  def orcType() = SimpleFunctionType(SimpleFunctionType())
    
}

/* 
 * Note: This is a weakened implementation of Guard.
 * It is assumed to be used only by the translation of ++.
 * Thus it exploits the invariants that a guard is used only in the
 * immediate children of the transaction in which it was created,
 * and that it is called at most once in each child. 
 */
class GuardInstance() extends UntypedSite with Site0 {
  
  def call(h: Handle) { h.publish() }
  
}
