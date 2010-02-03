//
// GroupRegion.java -- Java class GroupRegion
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

import orc.runtime.values.GroupCell;

public final class GroupRegion extends SubRegion {
	GroupCell cell;

	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(final Region parent, final GroupCell cell) {
		super(parent);
		this.cell = cell;
	}

	@Override
	protected void onClose() {
		super.onClose();
		cell.close();
	}
}
