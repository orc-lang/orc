//
// Visitor.java -- Java interface Visitor
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

package orc.ast.extended.visitor;

import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.declaration.IncludeDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.declaration.def.DefMemberType;
import orc.ast.extended.declaration.type.DatatypeDeclaration;
import orc.ast.extended.declaration.type.TypeAliasDeclaration;
import orc.ast.extended.declaration.type.TypeDeclaration;
import orc.ast.extended.expression.AssertType;
import orc.ast.extended.expression.Call;
import orc.ast.extended.expression.Capsule;
import orc.ast.extended.expression.Catch;
import orc.ast.extended.expression.CatchHandler;
import orc.ast.extended.expression.ConsExpr;
import orc.ast.extended.expression.Declare;
import orc.ast.extended.expression.Dot;
import orc.ast.extended.expression.Field;
import orc.ast.extended.expression.HasType;
import orc.ast.extended.expression.IfThenElse;
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
import orc.ast.extended.expression.Temporary;
import orc.ast.extended.expression.Throw;
import orc.ast.extended.pattern.AsPattern;
import orc.ast.extended.pattern.CallPattern;
import orc.ast.extended.pattern.ConsPattern;
import orc.ast.extended.pattern.EqPattern;
import orc.ast.extended.pattern.ListPattern;
import orc.ast.extended.pattern.LiteralPattern;
import orc.ast.extended.pattern.NilPattern;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.pattern.WildcardPattern;

/**
 * Visitor for extended ASTs.
 *
 * @author jthywiss
 * @param <E> Return type of the visitor.
 */
public interface Visitor<E> {
	public E visit(AssertType expr);

	public E visit(Call expr);
	
	public E visit(Capsule capsule);

	public E visit(Catch expr);

	public E visit(ConsExpr expr);

	public E visit(Declare expr);

	public E visit(Dot expr);

	public E visit(Field expr);

	public E visit(HasType expr);

	public E visit(IfThenElse expr);

	public E visit(Lambda expr);

	public E visit(Let expr);

	public E visit(ListExpr expr);

	public E visit(Literal expr);

	public E visit(Name expr);

	public E visit(NilExpr expr);

	public E visit(Parallel expr);

	public E visit(Otherwise expr);

	public E visit(Sequential expr);

	public E visit(Stop expr);

	public E visit(Temporary expr);

	public E visit(Throw expr);

	public E visit(Pruning expr);

	//TODO: public E visit(Type expr); 
	public E visit(CatchHandler handler);

	public E visit(IncludeDeclaration decl);

	public E visit(ClassDeclaration decl);

	public E visit(DefsDeclaration decl);

	public E visit(SiteDeclaration decl);

	public E visit(ValDeclaration decl);

	public E visit(DefMemberClause defn);

	public E visit(DefMemberType defn);

	public E visit(DatatypeDeclaration decl);

	public E visit(TypeAliasDeclaration decl);

	public E visit(TypeDeclaration decl);

	public E visit(AsPattern patn);

	public E visit(CallPattern patn);

	public E visit(ConsPattern patn);

	public E visit(EqPattern patn);

	public E visit(ListPattern patn);

	public E visit(LiteralPattern patn);

	public E visit(NilPattern patn);

	public E visit(TuplePattern patn);

	public E visit(TypedPattern patn);

	public E visit(VariablePattern patn);

	public E visit(WildcardPattern patn);

}
