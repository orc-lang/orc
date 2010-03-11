//
// CounterType.java -- Java class CounterType
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

package orc.lib.state.types;

import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;

public class CounterType extends DotType {

	@Override
	public String toString() {
		return "Counter";
	}

	public CounterType() {
		super();
		final Type t = new ArrowType(Type.SIGNAL); /* A method which takes no arguments and returns a signal */
		addField("inc", t);
		addField("dec", t);
		addField("onZero", t);
		addField("value", new ArrowType(Type.INTEGER));
	}

}
