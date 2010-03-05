//
// Nil.java -- Java class Nil
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

import orc.error.compiletime.typing.TypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NilValue;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;

/**
 * Implements the empty list constructor.
 * 
 * @author dkitchin
 */
public class Nil extends EvalSite {

	@Override
	public Object evaluate(final Args args) {
		return NilValue.singleton;
	}

	@Override
	public Type type() throws TypeException {
		return new ArrowType(new ListType().instance(Type.BOT));
	}

}
