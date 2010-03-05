//
// Field.java -- Java class Field
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression.argument;

import orc.ast.oil.visitor.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.ground.Message;

/**
 * Field access argument. Embeds a String key.
 * 
 * @author dkitchin
 */

public class Field extends Argument implements Comparable<Field> {
	public String key;

	public Field(final String key) {
		this.key = key;
	}

	public int compareTo(final Field that) {
		return this.key.compareTo(that.key);
	}

	@Override
	public boolean equals(final Object that) {
		return that.getClass().equals(Field.class) && this.compareTo((Field) that) == 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return key == null ? 0 : key.hashCode();
	}

	@Override
	public Object resolve(final Env<Object> env) {
		return new orc.runtime.values.Field(key);
	}

	@Override
	public String toString() {
		return "#field(" + key + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		return new Message(this);
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Field(key);
	}
}
