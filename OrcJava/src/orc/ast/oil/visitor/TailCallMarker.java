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

package orc.ast.oil.visitor;

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
	public Void visit(final Parallel expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Call)
	 */
	public Void visit(final Call expr) {
		expr.isTailCall = true;
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.DeclareDefs)
	 */
	public Void visit(final DeclareDefs expr) {
		expr.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Stop)
	 */
	public Void visit(final Stop expr) {
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Pruning)
	 */
	public Void visit(final Pruning expr) {
		expr.left.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Sequential)
	 */
	public Void visit(final Sequential expr) {
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Otherwise)
	 */
	public Void visit(final Otherwise expr) {
		expr.right.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.WithLocation)
	 */
	public Void visit(final WithLocation expr) {
		expr.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Constant)
	 */
	public Void visit(final Constant arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Field)
	 */
	public Void visit(final Field arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Site)
	 */
	public Void visit(final Site arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.argument.Variable)
	 */
	public Void visit(final Variable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.HasType)
	 */
	public Void visit(final HasType hasType) {
		hasType.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.DeclareType)
	 */
	public Void visit(final DeclareType typeDecl) {
		typeDecl.body.accept(this);
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Throw)
	 */
	public Void visit(final Throw expr) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.Visitor#visit(orc.ast.oil.expression.Catch)
	 */
	public Void visit(final Catch catchExpr) {
		// TODO Auto-generated method stub
		return null;
	}

}
