//
// Execution.java -- Java class Execution
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

import orc.runtime.OrcEngine;

/**
 * This class does nothing except serve
 * as a root for the execution.
 * @author quark
 */
public final class Execution extends Region {
	private final OrcEngine engine;

	public Execution(final OrcEngine engine) {
		this.engine = engine;
	}

	@Override
	protected void onClose() {
		// do nothing
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		engine.terminate();
	}
}
