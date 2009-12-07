//
// ExceptionsOnChecker.java -- Java class ExceptionsOnChecker
// Project OrcJava
//
// $Id$
//
// Created by matsuoka on Sep 7, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import java.util.LinkedList;

import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.Isolated;
import orc.ast.oil.expression.WithLocation;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 *
 * @author dkitchin
 */
public class IsolatedOnChecker extends Walker {
	
	private LinkedList<CompilationException> problems = new LinkedList<CompilationException>();
	private SourceLocation location;
	
	public static void check(Expression expr) throws CompilationException {
		IsolatedOnChecker checker = new IsolatedOnChecker();
		expr.accept(checker);
		if (checker.problems.size() > 0)
			throw checker.problems.getFirst();
	}
	
	public Void visit(Isolated isolatedExpr){
		CompilationException e = new CompilationException(
		"'isolated' expression found, but isolated keyword is not enabled.");
		e.setSourceLocation(location);
		problems.add(e);
		return null;
	}
	
	public Void visit(WithLocation expr) {
		this.location = expr.location;
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}

}
