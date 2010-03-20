//
// ReplaceNode.java -- Java class ReplaceNode
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Mar 17, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.update;

import java.util.ArrayList;
import java.util.List;

import orc.ast.oil.AstNode;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.runtime.Token;
import orc.runtime.Token.FrameContinuation;
import orc.runtime.values.Closure;

/**
 * Edit operation that is a to-one mapping of an AST node. 
 *
 * @author jthywiss
 */
public class ReplaceNode extends AstEditOperation {
	private final AstNode oldNode;
	private final AstNode newNode;

	/**
	 * Constructs an object of class ReplaceNode.
	 *
	 * @param oldNode
	 * @param newNode
	 */
	public ReplaceNode(final AstNode oldNode, final AstNode newNode) {
		super();
		this.oldNode = oldNode;
		this.newNode = newNode;
	}

	/* (non-Javadoc)
	 * @see orc.lib.update.AstEditOperation#isTokenAffected(orc.runtime.Token)
	 */
	@Override
	public boolean isTokenAffected(final Token token) {
		return token.getNode() == oldNode;
	}

	/* (non-Javadoc)
	 * @see orc.lib.update.AstEditOperation#isTokenSafe(orc.runtime.Token)
	 */
	@Override
	public boolean isTokenSafe(final Token token) {
		return true;
	}

	/* (non-Javadoc)
	 * @see orc.lib.update.AstEditOperation#migrateToken(orc.runtime.Token)
	 */
	@Override
	public boolean migrateToken(final Token token) {
		if (isTokenAffected(token)) {
			System.err.println(">>Move " + token + " from " + oldNode + " to " + newNode);
			token.move((Expression) newNode);
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see orc.lib.update.AstEditOperation#migrateClosures(orc.runtime.Token)
	 */
	@Override
	protected void migrateClosures(final Token token) {
		migrateClosures(token.getEnvironment().items(), new ArrayList<Closure>());
	}

	private void migrateClosures(final List<Object> os, final List<Closure> completedClosures) {
		for (final Object o : os) {
			if (o instanceof Closure) {
				final Closure c = (Closure) o;
				if (c.def == oldNode) {
					c.def = (Def) newNode;
				}
				final List<Object> cEnv = c.env.items();
				while (cEnv.removeAll(completedClosures)) {
					// Remove any recursive bindings
				}
				completedClosures.add(c);
				migrateClosures(cEnv, completedClosures);
			}
		}
	}

	/* (non-Javadoc)
	 * @see orc.lib.update.AstEditOperation#migrateFrameStack(orc.runtime.Token)
	 */
	@Override
	protected void migrateFrameStack(final Token token) {
		if (token.getContinuation() == null) {
			return;
		}
		for (FrameContinuation c = token.getContinuation(); c != null; c = c.parent) {
			if (c.callPoint == oldNode) {
				c.callPoint = (Expression) newNode;
			}
		}
	}

}
