//
// Logger.scala -- Scala object Logger
// Project PorcE
//
// Created by jthywiss on Nov 14, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

/** Logger for the orc.run.porce.distrib subsystem.
  *
  * @author jthywiss
  */
object Logger extends orc.util.Logger("orc.run.porce.distrib") {

  /** Logger for runtime network connection setup/teardown */
  object Connect extends orc.util.Logger("orc.run.porce.distrib.connect") {}
  
  /** Logger for Orc program loading/unloading */
  object ProgLoad extends orc.util.Logger("orc.run.porce.distrib.progload") {}
  
  /** Logger for message send/receive */
  object Message extends orc.util.Logger("orc.run.porce.distrib.message") {}
  
  /** Logger for value location/policy mapping */
  object ValueLocation extends orc.util.Logger("orc.run.porce.distrib.marshal") {}
  
  /** Logger for marsahling/unmarshaling */
  object Marshal extends orc.util.Logger("orc.run.porce.distrib.marshal") {}
  
  /** Logger for invocation interception and publications */
  object Invoke extends orc.util.Logger("orc.run.porce.distrib.invoke") {}
  
  /** Logger for group/counter/terminator proxying */
  object Proxy extends orc.util.Logger("orc.run.porce.distrib.proxy") {}
  
  /** Logger for remote futures */
  object Futures extends orc.util.Logger("orc.run.porce.distrib.futures") {}
  
  /** Logger for calls into the underlying Orc runtime */
  object Downcall extends orc.util.Logger("orc.run.porce.distrib.downcall") {}

}
