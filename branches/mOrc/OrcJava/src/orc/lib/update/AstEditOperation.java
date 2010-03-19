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
	 * Determine whether a token would be in the scope of this <code>AstEditOperation</code>.
	 * 
	 * @param token Token to check
	 * @return true if affected
	 */
	public abstract boolean isTokenAffected(final Token token);

	/**
	 * Determine whether a token is "safe" to migrate under this <code>AstEditOperation</code>.
	 * Will return true for tokens that are not affected by this operation, which may be
	 * unsafe to move under some other operation in the edit script.
	 *
	 * @param token Token to check
	 * @return true if safe to migrate
	 */
	public abstract boolean isTokenSafe(final Token token);

	/**
	 * Attempt to move a token as specified by this <code>AstEditOperation</code>.
	 * Tokens not affected by (in the scope of) this operation will be ignored.
	 *
	 * @param token Token to move
	 * @return true if successfully moved
	 */
	public abstract boolean migrateToken(final Token token);

	/**
	 * Update the closures in this token's environment to reflect the changes specified
	 * by the given <code>AstEditScript</code>. 
	 *
	 * @param token Token containing environments to update
	 * @param editList Edit script to apply to token's environments
	 * @see orc.env.Env
	 */
	public void migrateClosures(final Token token, final AstEditScript editList) {
		for (final AstEditOperation editOperation : editList) {
			editOperation.migrateClosures(token);
		}
	}

	/**
	 * Update the closures in this token's environment to reflect the changes specified
	 * by this <code>AstEditOperation</code>. 
	 *
	 * @param token Token containing environments to update
	 * @see orc.env.Env
	 */
	protected abstract void migrateClosures(Token token);

	/**
	 * Update the frame stack (continuations) in this token to reflect the changes specified
	 * by the given <code>AstEditScript</code>. 
	 *
	 * @param token Token containing frame stack to update
	 * @param editList Edit script to apply to token's environments
	 * @see orc.runtime.Token.FrameContinuation
	 */
	public void migrateFrameStack(final Token token, final AstEditScript editList) {
		for (final AstEditOperation editOperation : editList) {
			editOperation.migrateFrameStack(token);
		}
	}

	/**
	 * Update the frame stack (continuations) in this token to reflect the changes specified
	 * by this <code>AstEditOperation</code>. 
	 *
	 * @param token Token containing frame stack to update
	 * @see orc.runtime.Token.FrameContinuation
	 */
	protected abstract void migrateFrameStack(Token token);

}
