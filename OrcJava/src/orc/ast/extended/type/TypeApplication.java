//
// TypeApplication.java -- Java class TypeApplication
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

package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.type.FreeTypeVariable;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public String name;
	public List<Type> params;

	public TypeApplication(final String name, final List<Type> params) {
		this.name = name;
		this.params = params;
	}

	@Override
	public orc.ast.simple.type.Type simplify() {

		final List<orc.ast.simple.type.Type> ts = new LinkedList<orc.ast.simple.type.Type>();
		for (final Type t : params) {
			ts.add(t.simplify());
		}

		return new orc.ast.simple.type.TypeApplication(new FreeTypeVariable(name), ts);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append(name);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(params.get(i));
		}
		s.append(']');

		return s.toString();
	}

}
