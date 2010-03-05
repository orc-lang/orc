//
// ImmutableContainerType.java -- Java class ImmutableContainerType
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.type.tycon;

import java.util.LinkedList;
import java.util.List;

public abstract class ImmutableContainerType extends Tycon {

	@Override
	public List<Variance> variances() {
		final List<Variance> vs = new LinkedList<Variance>();
		vs.add(Variance.COVARIANT);
		return vs;
	}

}
