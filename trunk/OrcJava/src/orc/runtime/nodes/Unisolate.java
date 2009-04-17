/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.error.runtime.SiteException;
import orc.runtime.IsolatedGroup;
import orc.runtime.Token;
import orc.runtime.regions.IsolatedRegion;

/**
 * Leave an isolated region (protected from external termination).
 * See also {@link Isolate}.
 * @author quark
 */
public class Unisolate extends Node {
	private static final long serialVersionUID = 1L;
	public Node body;
	public Unisolate(Node body) {
		this.body = body;
	}

	public void process(Token t) {
		try {
			t.popLtimer();
		} catch (SiteException e) {
			t.error(e);
			return;
		}
		IsolatedRegion r = (IsolatedRegion)t.getRegion();
		t.setRegion(r.getParent());
		IsolatedGroup g = (IsolatedGroup)t.getGroup();
		t.setGroup(g.getParent());
		t.move(body).activate();
	}
}