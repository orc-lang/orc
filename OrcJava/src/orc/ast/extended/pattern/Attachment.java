//
// Attachment.java -- Java class Attachment
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

package orc.ast.extended.pattern;

import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.HasType;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.type.Type;

public class Attachment {

	public Variable v;
	public Expression e;

	public Attachment(final Variable v, final Expression e) {
		this.v = v;
		this.e = e;
	}

	public Expression attach(final Expression f) {
		return attach(f, null);
	}

	public Expression attach(final Expression f, final Type t) {

		Expression g = e;
		if (t != null) {
			g = new HasType(g, t, true);
		}

		return new Pruning(f, g, v);
	}

}
