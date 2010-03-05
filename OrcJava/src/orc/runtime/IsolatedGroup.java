//
// IsolatedGroup.java -- Java class IsolatedGroup
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

package orc.runtime;

/**
 * A token in an isolated group is protected from forced termination.
 * @author quark
 */
public class IsolatedGroup extends Group {
	private final Group parent;

	public IsolatedGroup(final Group parent) {
		this.parent = parent;
		// NB: do not add ourselves to parent group; when
		// the parent is killed and recursively kills
		// its children, it won't include us so we
		// won't be killed.
	}

	public Group getParent() {
		return parent;
	}
}
