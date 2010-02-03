//
// None.java -- Java class None
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

package orc.runtime.sites.core;

import java.util.LinkedList;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.OptionType;
import orc.type.structured.TupleType;

/**
 * Implements the "none" option constructor site.
 * 
 * @author quark
 */
public class None extends Site {
	// since tags are compared by object equality,
	// we need to share a tag amongst all instances of this site
	static final Datasite data = new Datasite("None");

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		data.callSite(args, caller);
	}

	@Override
	public Type type() throws TypeException {
		final Type OptionBot = new OptionType().instance(Type.BOT);
		final Type OptionTop = new OptionType().instance(Type.TOP);

		final Type construct = new ArrowType(OptionBot);
		final Type destruct = new ArrowType(OptionTop, new TupleType(new LinkedList<Type>()));

		final DotType both = new DotType(construct);
		both.addField("?", destruct);

		return both;
	}
}
