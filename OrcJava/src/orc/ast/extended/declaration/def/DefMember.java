//
// DefMember.java -- Java class DefMember
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

package orc.ast.extended.declaration.def;

import orc.ast.extended.ASTNode;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public abstract class DefMember implements ASTNode, Locatable {

	public String name;
	private SourceLocation location;

	public abstract void extend(AggregateDef adef) throws CompilationException;

	public void setSourceLocation(final SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

}
