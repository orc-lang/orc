//
// Visitor.java -- Java interface Visitor
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
 * Visitor for OIL expressions.
 * @author quark
 *
 * @param <E> Return type of the visitor.
 */
public interface Visitor<E> {
	public E visit(Parallel expr);

	public E visit(Call expr);

	public E visit(DeclareDefs expr);

	public E visit(Stop expr);

	public E visit(Pruning expr);

	public E visit(Sequential expr);

	public E visit(Otherwise expr);

	public E visit(WithLocation expr);

	public E visit(Constant arg);

	public E visit(Field arg);

	public E visit(Site arg);

	public E visit(Variable arg);

	public E visit(HasType hasType);

	public E visit(DeclareType typeDecl);

	public E visit(Throw expr);

	public E visit(Catch catchExpr);
}
