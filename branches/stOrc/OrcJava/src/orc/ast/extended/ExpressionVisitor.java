//
// ExpressionVisitor.java -- Java class ExpressionVisitor
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Aug 20, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended;

import orc.ast.extended.expression.AssertType;
import orc.ast.extended.expression.Atomic;
import orc.ast.extended.expression.Call;
import orc.ast.extended.expression.Catch;
import orc.ast.extended.expression.ConsExpr;
import orc.ast.extended.expression.Declare;
import orc.ast.extended.expression.Dot;
import orc.ast.extended.expression.Field;
import orc.ast.extended.expression.HasType;
import orc.ast.extended.expression.IfThenElse;
import orc.ast.extended.expression.Isolated;
import orc.ast.extended.expression.Lambda;
import orc.ast.extended.expression.Let;
import orc.ast.extended.expression.ListExpr;
import orc.ast.extended.expression.Literal;
import orc.ast.extended.expression.Name;
import orc.ast.extended.expression.NilExpr;
import orc.ast.extended.expression.Otherwise;
import orc.ast.extended.expression.Parallel;
import orc.ast.extended.expression.Pruning;
import orc.ast.extended.expression.Sequential;
import orc.ast.extended.expression.Stop;
import orc.ast.extended.expression.Throw;

/**
 * Visitor for expressions in the extended AST.
 *
 * @author dkitchin
 * @param <E> Return type of the visitor.
 */
public interface ExpressionVisitor<E> {

	public E visit(AssertType expr);
	public E visit(Atomic expr);
	public E visit(Call expr);
	public E visit(Catch expr);
	public E visit(ConsExpr expr);
	public E visit(Declare expr);
	public E visit(Dot expr);
	public E visit(Field expr);
	public E visit(HasType expr);
	public E visit(IfThenElse expr);
	public E visit(Isolated expr);
	public E visit(Lambda expr);
	public E visit(Let expr);
	public E visit(ListExpr expr);
	public E visit(Literal expr);
	public E visit(Name expr);
	public E visit(NilExpr expr);
	public E visit(Otherwise expr);
	public E visit(Parallel expr);
	public E visit(Pruning expr);
	public E visit(Sequential expr);
	public E visit(Stop expr);
	public E visit(Throw expr);
	
}
