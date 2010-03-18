//
// AstEditOperation.java -- Java class AstEditOperation
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

import orc.runtime.Token;

/**
 * An instance of AstEditOperation represents one tree-edit operation in a tree-edit script. A script is simply
 * a list of operations, which modify an OIL AST into a new OIL AST.
 *
 * @see AstEditScript
 * @author jthywiss
 */
public abstract class AstEditOperation {
	/**
	 * @param token
	 * @return
	 */
	public abstract boolean isTokenAffected(final Token token);

	/**
	 * @param token
	 * @return
	 */
	public abstract boolean isTokenSafe(final Token token);

	/**
	 * @param token
	 * @return
	 */
	public abstract boolean migrateToken(final Token token);

}
