//
// Group.java -- Java class Group
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

import java.util.HashSet;

/**
 * Every token belongs to a group which decides whether
 * that token is active or not. Groups form a tree structure.
 * Groups can be forcibly killed.
 * When a group is killed, all of its children are also killed
 * immediately. The tokens, however, are killed lazily as they
 * become active.
 * 
 * @author quark
 */
public class Group {
	/** Is this group currently alive? */
	protected boolean alive = true;
	private final HashSet<Group> children = new HashSet<Group>();

	/** Add a subgroup. */
	public final void add(final Group child) {
		if (!alive) {
			return;
		}
		this.children.add(child);
	}

	/** Remove a subgroup. */
	public final void remove(final Group child) {
		if (!alive) {
			return;
		}
		this.children.remove(child);
	}

	/**
	 * Forcibly terminate this group and all of its
	 * children.
	 */
	public final void kill() {
		if (!alive) {
			return;
		}
		alive = false;
		onKill();
		for (final Group child : children) {
			child.kill();
		}
		children.clear();
	}

	/** Override this. */
	public void onKill() {
	}

	/** Is this group currently alive? */
	public final boolean isAlive() {
		return alive;
	}
}
