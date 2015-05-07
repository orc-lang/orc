//
// AbstractValue.java -- Java class AbstractValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import orc.trace.Terms;

public abstract class AbstractValue implements Value {
	@Override
	public String toString() {
		return Terms.printToString(this);
	}
}
