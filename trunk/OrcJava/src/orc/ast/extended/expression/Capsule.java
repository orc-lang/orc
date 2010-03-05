//
// Capsule.java -- Java class Capsule
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

package orc.ast.extended.expression;

import java.util.ArrayList;
import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.declaration.def.DefMember;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 * Expression enclosing the body of a definition annotated with "capsule".
 *
 * @author amshali, dkitchin
 */
public class Capsule extends Expression {

	public Expression body;

	public Capsule(final Expression body) {
		this.body = body;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		// perform capsule translation
		body = makeNewBody(body, new ArrayList<String>());
		
		// Protect the capsule body with a thunk
		// lambda() = capsule_body
		body = Lambda.makeThunk(body);
		
		// Convert the thunk to a site
		// Site(lambda () = capsule_body)
		body = new Call("Site", body);
		
		// Force the now-protected thunk
		// Site(...)() 
		body = new Call(body);
		
		return new WithLocation(body.simplify(), getSourceLocation());
	}

	
	/**
	 * This function recursively looks for definitions inside the 
	 * capsule declaration (body) and put them all in defFunctions list.
	 * At the end  
	 * @param body Body of the capsule declaration.
	 * @param defFunctions List of definitions inside the capsule.
	 * @return Returns a new body
	 * @throws CompilationException
	 */
	private orc.ast.extended.expression.Expression makeNewBody(orc.ast.extended.expression.Expression body, List<String> defFunctions) throws CompilationException {
		if (body instanceof Declare) {
			Declare decl = (Declare) body;
			Declaration defs = decl.d;
			if (defs instanceof DefsDeclaration) {
				for (DefMember d : ((DefsDeclaration) defs).defs) {
					if (d instanceof DefMemberClause) {
						defFunctions.add(d.name);
					}
				}
			}
			return new Declare(decl.d, makeNewBody(decl.e, defFunctions));
		} else {
			if (defFunctions.size() == 0) {
				CompilationException exc = new CompilationException("A capsule must contain at least one def");
				exc.setSourceLocation(this.getSourceLocation());
				throw exc;
			} else {
				List<orc.ast.extended.expression.Expression> recordArgs = makeRecordArgs(defFunctions);
				Call recordCall = new Call(new Name("Record"), recordArgs);

				// Semantics: body as active process
				return new Parallel(new Sequential(body, new Stop()), recordCall);

				// Alternative semantics: body as initializer
				// return new Otherwise(new Sequential(body, new Stop()), recordCall);
			}
		}
	}

	/**
	 * This method gets a list of definitions, defFunctions, and returns 
	 * a Record site with the definitions protected by Site.
	 * @param defFunctions
	 * @return returns the a Record site with the name of definitions and the 
	 * definitions protected by a Site.
	 */
	private List<orc.ast.extended.expression.Expression> makeRecordArgs(List<String> defFunctions) {
		List<orc.ast.extended.expression.Expression> args = new ArrayList<orc.ast.extended.expression.Expression>();
		for (String s : defFunctions) {
			args.add(new Literal(s));
			args.add(new Call(new Name("Site"), new Name(s)));
		}
		return args;
	}
	
	
	@Override
	public String toString() {
		return "(capsule (" + body + "))";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
