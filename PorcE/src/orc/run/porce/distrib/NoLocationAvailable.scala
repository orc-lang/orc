//
// NoLocationAvailable.scala -- Scala class NoLocationAvailable
// Project ProcE
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.error.runtime.TokenException

/** Exception thrown when no allowed common location can be found to execute a dOrc operation.
  *
  * @author jthywiss
  */
class NoLocationAvailable(vals: List[AnyRef]) extends TokenException("No common location for " + vals)
