//
// SemiRegion.java -- Java class SemiRegion
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

package orc.runtime.regions;

import orc.runtime.Token;

public final class SemiRegion extends SubRegion {
	Token t;

	/* Create a new group region with the given parent and coupled group cell */
	public SemiRegion(final Region parent, final Token t) {
		super(parent);
		this.t = t;
	}

	@Override
	protected void onClose() {
		if (t != null) {
			t.unsetQuiescent();
			t.activate();
		}
		super.onClose();
	}

	public void cancel() {
		if (t != null) {
			t.unsetQuiescent();
			t.die();
			t = null;
		}
	}
}
