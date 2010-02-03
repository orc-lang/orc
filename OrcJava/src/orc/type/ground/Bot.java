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

package orc.type.ground;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

/**
 * The bottom type. Subtype of every other type.
 * 
 * Bot is the type of expressions which will never publish.
 * 
 * @author dkitchin
 */
public final class Bot extends Type {

	@Override
	public boolean subtype(final Type that) throws TypeException {
		return true;
	}

	@Override
	public boolean equal(final Type that) {
		return that.isBot();
	}

	@Override
	public Type join(final Type that) throws TypeException {
		return that;
	}

	@Override
	public Type meet(final Type that) {
		return this;
	}

	@Override
	public Type call(final List<Type> args) {
		return this;
	}

	@Override
	public boolean isBot() {
		return true;
	}

	@Override
	public String toString() {
		return "Bot";
	}
}
