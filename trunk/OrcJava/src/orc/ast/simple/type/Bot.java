//
// Bot.java -- Java class Bot
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

package orc.ast.simple.type;

import orc.env.Env;

/**
 * The type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 */
public class Bot extends Type {

	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) {
		return orc.ast.oil.type.Type.BOT;
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return this;
	}

	@Override
	public String toString() {
		return "Bot";
	}
}
