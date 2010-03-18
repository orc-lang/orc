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

import orc.ast.oil.AstNode;
import orc.ast.oil.expression.Expression;
import orc.runtime.Token;

/**
 * 
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
			System.err.println(">>Move "+token+" from "+oldNode+" to "+newNode);
			token.move((Expression) newNode);
			return true;
		} else {
			return false;
		}
	}

}
