//
// TailCallMarker.java -- Java class TailCallMarker
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Jan 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;

/**
 * 
 * Mark all calls in tail call contexts in this expression.
 *
 * @author dkitchin
 */
public class TailCallMarker implements Visitor<Void> {

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Parallel)
	 */
	public Void visit(Parallel expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Call)
	 */
	public Void visit(Call expr) {
		expr.isTailCall = true;
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.DeclareDefs)
	 */
	public Void visit(DeclareDefs expr) {
		expr.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Stop)
	 */
	public Void visit(Stop expr) {
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Pruning)
	 */
	public Void visit(Pruning expr) {
		expr.left.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Sequential)
	 */
	public Void visit(Sequential expr) {
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Otherwise)
	 */
	public Void visit(Otherwise expr) {
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.WithLocation)
	 */
	public Void visit(WithLocation expr) {
		expr.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Constant)
	 */
	public Void visit(Constant arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Field)
	 */
	public Void visit(Field arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Site)
	 */
	public Void visit(Site arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Variable)
	 */
	public Void visit(Variable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Atomic)
	 */
	public Void visit(Atomic atomic) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.HasType)
	 */
	public Void visit(HasType hasType) {
		hasType.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.DeclareType)
	 */
	public Void visit(DeclareType typeDecl) {
		typeDecl.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Throw)
	 */
	public Void visit(Throw expr) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Catch)
	 */
	public Void visit(Catch catchExpr) {
		// TODO Auto-generated method stub
		return null;
	}

}
