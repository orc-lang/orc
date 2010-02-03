//
// SubRegion.java -- Java class SubRegion
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

/**
 * Regions are used to track when some (sub-)computation terminates.
 * 
 * <p>Currently the region methods must be synchronized, because tokens
 * can be killed by independent threads (such as site calls in progress),
 * triggering an update on the region. Maybe we should have a separate
 * queue deal with dead tokens so this isn't necessary.
 */
public abstract class SubRegion extends Region {
	protected Region parent;

	public SubRegion(final Region parent) {
		this.parent = parent;
		this.parent.add(this);
	}

	@Override
	protected void onClose() {
		this.parent.remove(this);
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		parent.removeActive();
	}

	@Override
	protected void activate() {
		super.activate();
		parent.addActive();
	}

	public final Region getParent() {
		return parent;
	}
}
