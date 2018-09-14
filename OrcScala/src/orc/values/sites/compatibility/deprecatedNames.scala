//
// deprecatedNames.scala -- Temporary forwarding names for moved APIs
// Project OrcScala
//
// Created by amp on July 12, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility

import orc.DirectInvoker

@deprecated("Use DirectInvoker", "now")
trait OnlyDirectInvoker extends DirectInvoker
