//
// Message.java -- Java class Message
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

import orc.ast.oil.expression.argument.Field;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

public class Message extends Type {

	public Field f;

	public Message(final Field f) {
		this.f = f;
	}

	/* Message types are subtypes only of Top and of equal message types */
	@Override
	public boolean subtype(final Type that) throws TypeException {
		return that.isTop() || that.equal(this);
	}

	@Override
	public boolean equal(final Type that) {
		return that.getClass().equals(Message.class) && this.f.equals(((Message) that).f);
	}

	@Override
	public String toString() {
		return "." + f.key;
	}

}
