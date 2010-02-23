//
// Transformer.java -- Java class Transformer
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

package orc.ast.oil.visitor;

import java.util.LinkedList;

import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.type.Type;

/**
 * Abstract base class tree transformer for Oil expressions.
 * 
 * @author quark
 */
public abstract class Transformer implements Visitor<Expression> {

	public Expression visit(final Parallel expr) {
		return new Parallel(expr.left.accept(this), expr.right.accept(this));
	}

	public Expression visit(final Call expr) {
		final LinkedList<Argument> newArgs = new LinkedList<Argument>();
		for (final Argument a : expr.args) {
			newArgs.add((Argument) a.accept(this));
		}
		LinkedList<Type> newTypeArgs = null;
		if (expr.typeArgs != null) {
			newTypeArgs = new LinkedList<Type>();
			for (final Type t : expr.typeArgs) {
				newTypeArgs.add(visit(t));
			}
		}
		return new Call((Argument) expr.callee.accept(this), newArgs, newTypeArgs);
	}

	public Type visit(final Type type) {
		return type;
	}

	public Expression visit(final Constant arg) {
		return arg;
	}

	public Expression visit(final DeclareDefs expr) {
		final LinkedList<Def> newDefs = new LinkedList<Def>();
		for (final Def d : expr.defs) {
			newDefs.add(visit(d));
		}
		return new DeclareDefs(newDefs, expr.body.accept(this));
	}

	public Def visit(final Def d) {
		LinkedList<Type> newArgTypes = null;
		if (d.argTypes != null) {
			newArgTypes = new LinkedList<Type>();
			for (final Type t : d.argTypes) {
				newArgTypes.add(visit(t));
			}
		}
		Type newResultType = null;
		if (d.resultType != null) {
			newResultType = visit(d.resultType);
		}
		return new Def(d.arity, d.body.accept(this), d.typeArity, newArgTypes, newResultType, d.location, d.name);
	}

	public Expression visit(final Field arg) {
		return arg;
	}

	public Expression visit(final HasType hasType) {
		return new HasType(hasType.body.accept(this), visit(hasType.type), hasType.checkable);
	}

	public Expression visit(final Pruning expr) {
		return new Pruning(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expression visit(final Sequential expr) {
		return new Sequential(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expression visit(final Otherwise expr) {
		return new Otherwise(expr.left.accept(this), expr.right.accept(this));
	}

	public Expression visit(final Stop expr) {
		return expr;
	}

	public Expression visit(final Site arg) {
		return arg;
	}

	public Expression visit(final DeclareType typeDecl) {
		return new DeclareType(visit(typeDecl.type), typeDecl.body.accept(this));
	}

	public Expression visit(final Variable arg) {
		return arg;
	}

	public Expression visit(final WithLocation expr) {
		return new WithLocation(expr.body.accept(this), expr.location);
	}

	public Expression visit(final Throw throwExpr) {
		return new Throw(throwExpr.exception.accept(this));
	}

	public Expression visit(final Catch catchExpr) {
		return new Catch(this.visit(catchExpr.handler), catchExpr.tryBlock.accept(this));
	}
}
