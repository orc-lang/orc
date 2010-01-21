//
// ${file_name} -- Java class ${type_name}
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
 * Context-carrying visitor for OIL expressions.
 * 
 * @author dkitchin
 *
 * @param <E> Return type of the visitor.
 * @param <C> Context type.
 */
public interface ContextualVisitor<E, C> {
	public E visit(Parallel expr, C context);

	public E visit(Call expr, C context);

	public E visit(DeclareDefs expr, C context);

	public E visit(Stop expr, C context);

	public E visit(Pruning expr, C context);

	public E visit(Sequential expr, C context);

	public E visit(Otherwise expr, C context);

	public E visit(WithLocation expr, C context);

	public E visit(Constant arg, C context);

	public E visit(Field arg, C context);

	public E visit(Site arg, C context);

	public E visit(Variable arg, C context);

	public E visit(Atomic atomic, C context);

	public E visit(HasType hasType, C context);

	public E visit(DeclareType typeDecl, C context);

	public E visit(Throw expr, C context);

	public E visit(Catch catchExpr, C context);
}
