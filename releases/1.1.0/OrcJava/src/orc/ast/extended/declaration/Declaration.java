//
// Declaration.java -- Java class Declaration
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

package orc.ast.extended.declaration;

import orc.ast.extended.ASTNode;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A unit of syntax that encapsulates some declaration. Declarations affect the environment, 
 * for example by adding new bindings, but do not typically do computation on their own.
 * 
 * A declaration is scoped in the abstract syntax tree by a Declare object.
 * 
 * @author dkitchin
 *
 */
public abstract class Declaration implements ASTNode, Locatable {
	protected SourceLocation location;

	public abstract orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) throws CompilationException;

	public void setSourceLocation(final SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
