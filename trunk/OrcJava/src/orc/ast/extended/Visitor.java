/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
package orc.ast.extended;

import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.Declaration;
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
import orc.ast.extended.expression.Atomic;
import orc.ast.extended.expression.Call;
import orc.ast.extended.expression.Catch;
import orc.ast.extended.expression.CatchHandler;
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
import orc.ast.extended.expression.Parallel;
import orc.ast.extended.expression.Otherwise;
import orc.ast.extended.expression.Sequential;
import orc.ast.extended.expression.Stop;
import orc.ast.extended.expression.Throw;
import orc.ast.extended.expression.Pruning;
import orc.ast.extended.pattern.AsPattern;
import orc.ast.extended.pattern.CallPattern;
import orc.ast.extended.pattern.ConsPattern;
import orc.ast.extended.pattern.EqPattern;
import orc.ast.extended.pattern.ListPattern;
import orc.ast.extended.pattern.LiteralPattern;
import orc.ast.extended.pattern.NilPattern;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.pattern.WildcardPattern;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.FreeVariable;

/**
 * Visitor for extended ASTs.
 *
 * @author jthywiss
 * @param <E> Return type of the visitor.
 */
public interface Visitor<E> {
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
	public E visit(Parallel expr);
	public E visit(Otherwise expr);
	public E visit(Sequential expr);
	public E visit(Stop expr);
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
