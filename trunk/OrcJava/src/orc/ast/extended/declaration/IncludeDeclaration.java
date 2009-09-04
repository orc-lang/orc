//
// IncludeDeclaration.java -- Java class IncludeDeclaration
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Sep 3, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.declaration;

import java.io.File;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.CompilationException;

/**
 * Group together a series of declarations which were included from the same file.
 *
 * @author dkitchin
 */
public class IncludeDeclaration extends Declaration {

	public List<Declaration> decls;
	public String sourceFile;
	
	public IncludeDeclaration(List<Declaration> decls, String sourceFile) {
		this.decls = decls;
		this.sourceFile = sourceFile;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.declaration.Declaration#bindto(orc.ast.simple.expression.Expression)
	 */
	@Override
	public Expression bindto(Expression target) throws CompilationException {
	
		for(int i = decls.size() - 1; i >= 0; i--) {
			Declaration d = decls.get(i);
			target = d.bindto(target);
		}
		
		return target;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.extended.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		// TODO Auto-generated method stub
		return visitor.visit(this);
	}

}
