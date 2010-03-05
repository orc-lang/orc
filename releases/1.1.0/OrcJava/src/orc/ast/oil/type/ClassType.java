//
// ClassType.java -- Java class ClassType
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

package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

/**
 * A type which refers to a Java class (which we will treat as an Orc type).
 * @author quark, dkitchin
 */
public class ClassType extends Type {

	public String classname;

	public ClassType(final String classname) {
		this.classname = classname;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return classname == null ? 0 : classname.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ClassType other = (ClassType) obj;
		if (classname == null) {
			if (other.classname != null) {
				return false;
			}
		} else if (!classname.equals(other.classname)) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {
		return ctx.resolveClassType(classname);
	}

	@Override
	public String toString() {
		return classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.ClassnameType(classname);
	}

}
