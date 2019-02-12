//
// Logger.scala -- Scala object Logger
// Project OrcScala
//
// Created by jthywiss on Nov 14, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** Logger for the orc.run.distrib subsystem.
  *
  * @author jthywiss
  */
object Logger extends orc.util.Logger("orc.run.distrib") {

  /** Logger for runtime network connection setup/teardown */
  object Connect extends orc.util.Logger("orc.run.distrib.connect") {}
  
  /** Logger for Orc program loading/unloading */
  object ProgLoad extends orc.util.Logger("orc.run.distrib.progload") {}
  
  /** Logger for message send/receive */
  object Message extends orc.util.Logger("orc.run.distrib.message") {}
  
  /** Logger for value location/policy mapping */
  object ValueLocation extends orc.util.Logger("orc.run.distrib.valueloc") {}
  
  /** Logger for marsahling/unmarshaling */
  object Marshal extends orc.util.Logger("orc.run.distrib.marshal") {}
  
  /** Logger for invocation interception and publications */
  object Invoke extends orc.util.Logger("orc.run.distrib.invoke") {}
  
  /** Logger for group/counter/terminator proxying */
  object Proxy extends orc.util.Logger("orc.run.distrib.proxy") {}
  
  /** Logger for remote futures */
  object Futures extends orc.util.Logger("orc.run.distrib.futures") {}
  
  /** Logger for calls into the underlying Orc runtime */
  object Downcall extends orc.util.Logger("orc.run.distrib.downcall") {}

}
