//
// AstEditScript.java -- Java class AstEditScript
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Mar 16, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.update;

import java.util.ArrayList;
import java.util.Iterator;

import orc.ast.oil.AstNode;

/**
 * An instance of AstEditScript represents a tree-edit script that modifies an OIL AST into a new OIL AST.
 * An AstEditScript is simply a list of AstEditOperations.
 *
 * @see AstEditOperation
 * @author jthywiss
 */
public class AstEditScript extends ArrayList<AstEditOperation> {

	/**
	 * @param oldOilAst
	 * @param newOilAst
	 * @return
	 */
	public static AstEditScript computeEditScript(final AstNode oldOilAst, final AstNode newOilAst) {
		AstEditScript editScript = new AstEditScript();
		r(editScript, oldOilAst, newOilAst);
		return editScript;
	}

	private static AstEditScript r(AstEditScript script, final AstNode oldNode, final AstNode newNode) {
		script.add(new ReplaceNode(oldNode, newNode));
		Iterator<AstNode> oldIter = oldNode.getChildren().iterator();
		Iterator<AstNode> newIter = newNode.getChildren().iterator();
		while (oldIter.hasNext()) {
			if (oldIter.hasNext() != newIter.hasNext()) {
				throw new AssertionError("Uh oh");
			}
			r(script, oldIter.next(), newIter.next());
		}
		if (oldIter.hasNext() != newIter.hasNext()) {
			throw new AssertionError("Uh oh");
		}

		return script;
	}
}
