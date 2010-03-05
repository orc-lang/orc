//
// Walker.java -- Java class Walker
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

import java.util.List;

import orc.ast.extended.ASTNode;
import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.declaration.IncludeDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.def.DefMember;
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
import orc.ast.extended.expression.Expression;
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
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.pattern.WildcardPattern;

/**
 * 
 *
 * @author jthywiss
 */
public class Walker implements Visitor<Void> {
	//FIXME; Walk into type AST nodes (commented below)

	public void enterScope(final ASTNode node) {
	}

	public void leaveScope(final ASTNode node) {
	}

	public boolean enter(final ASTNode node) {
		return true;
	}

	public Void visit(final AssertType expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.body.accept(this);
		//		expr.type.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final AssertType expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final AssertType expr) {
	}

	public Void visit(final Call expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.caller.accept(this);
		for (final Expression arg : expr.args) {
			arg.accept(this);
		}
		//		for (Type typeArg : expr.typeArgs) typeArg.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Call expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Call expr) {
	}

	
	public Void visit(final Capsule expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Capsule expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Capsule expr) {
	}
	
	
	public Void visit(final Catch expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.tryBlock.accept(this);
		for (final CatchHandler handler : expr.handlers) {
			handler.accept(this);
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Catch expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Catch expr) {
	}

	public Void visit(final ConsExpr expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.h.accept(this);
		expr.t.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final ConsExpr expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final ConsExpr expr) {
	}

	public Void visit(final Declare expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.enterScope(expr);
		expr.d.accept(this);
		expr.e.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Declare expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Declare expr) {
	}

	public Void visit(final Dot expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.target.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Dot expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Dot expr) {
	}

	public Void visit(final Field expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Field expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Field expr) {
	}

	public Void visit(final HasType expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.body.accept(this);
		//		expr.type.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final HasType expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final HasType expr) {
	}

	public Void visit(final IfThenElse expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.condition.accept(this);
		expr.consequent.accept(this);
		expr.alternative.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final IfThenElse expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final IfThenElse expr) {
	}

	public Void visit(final Lambda expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.enterScope(expr);
		for (final List<Pattern> ps : expr.formals) {
			for (final Pattern formal : ps) {
				formal.accept(this);
			}
		}
		//		expr.resultType.accept(this);
		expr.body.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Lambda expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Lambda expr) {
	}

	public Void visit(final Let expr) {
		if (!this.enter(expr)) {
			return null;
		}
		for (final Expression arg : expr.args) {
			arg.accept(this);
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Let expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Let expr) {
	}

	public Void visit(final ListExpr expr) {
		if (!this.enter(expr)) {
			return null;
		}
		for (final Expression arg : expr.es) {
			arg.accept(this);
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final ListExpr expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final ListExpr expr) {
	}

	public Void visit(final Literal expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Literal expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Literal expr) {
	}

	public Void visit(final Name expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Name expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Name expr) {
	}

	public Void visit(final NilExpr expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final NilExpr expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final NilExpr expr) {
	}

	public Void visit(final Parallel expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Parallel expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Parallel expr) {
	}

	public Void visit(final Otherwise expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Otherwise expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Otherwise expr) {
	}

	public Void visit(final Sequential expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.left.accept(this);
		this.enterScope(expr);
		expr.p.accept(this);
		expr.right.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Sequential expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Sequential expr) {
	}

	public Void visit(final Stop expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Stop expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Stop expr) {
	}

	public Void visit(final Temporary expr) {
		if (!this.enter(expr)) {
			return null;
		}
		this.leave(expr);
		return null;
	}

	public boolean enter(final Temporary expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Temporary expr) {
	}

	public Void visit(final Throw expr) {
		if (!this.enter(expr)) {
			return null;
		}
		expr.exception.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Throw expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Throw expr) {
	}

	public Void visit(final Pruning expr) {
		if (!this.enter(expr)) {
			return null;
		}
		//TODO: Desired order of traversal? (l-p-r or r-p-l?)
		this.enterScope(expr);
		expr.left.accept(this);
		expr.p.accept(this);
		this.leaveScope(expr);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public boolean enter(final Pruning expr) {
		return enter((ASTNode) expr);
	}

	public void leave(final Pruning expr) {
	}

	//TODO: Type's subtypes

	public Void visit(final CatchHandler handler) {
		if (!this.enter(handler)) {
			return null;
		}
		this.enterScope(handler);
		for (final Pattern patn : handler.catchPattern) {
			patn.accept(this);
		}
		handler.body.accept(this);
		this.leaveScope(handler);
		this.leave(handler);
		return null;
	}

	public boolean enter(final CatchHandler handler) {
		return enter((ASTNode) handler);
	}

	public void leave(final CatchHandler handler) {
	}

	public Void visit(final IncludeDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		for (final Declaration aDecl : decl.decls) {
			aDecl.accept(this);
		}
		this.leave(decl);
		return null;
	}

	public boolean enter(final IncludeDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final IncludeDeclaration decl) {
	}

	public Void visit(final ClassDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		this.leave(decl);
		return null;
	}

	public boolean enter(final ClassDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final ClassDeclaration decl) {
	}

	public Void visit(final DefsDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		for (final DefMember def : decl.defs) {
			def.accept(this);
		}
		this.leave(decl);
		return null;
	}

	public boolean enter(final DefsDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final DefsDeclaration decl) {
	}

	public Void visit(final SiteDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		this.leave(decl);
		return null;
	}

	public boolean enter(final SiteDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final SiteDeclaration decl) {
	}

	public Void visit(final ValDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		decl.p.accept(this);
		decl.e.accept(this);
		this.leave(decl);
		return null;
	}

	public boolean enter(final ValDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final ValDeclaration decl) {
	}

	public Void visit(final DefMemberClause defn) {
		if (!this.enter(defn)) {
			return null;
		}
		this.enterScope(defn);
		for (final List<Pattern> ps : defn.formals) {
			for (final Pattern formal : ps) {
				formal.accept(this);
			}
		}
		//		defn.resultType.accept(this);
		defn.body.accept(this);
		this.leaveScope(defn);
		this.leave(defn);
		return null;
	}

	public boolean enter(final DefMemberClause defn) {
		return enter((ASTNode) defn);
	}

	public void leave(final DefMemberClause defn) {
	}

	public Void visit(final DefMemberType defn) {
		if (!this.enter(defn)) {
			return null;
		}
		this.enterScope(defn);
		//		for (Type arg : defn.argTypes) arg.accept(this);
		//		defn.resultType.accept(this);
		this.leaveScope(defn);
		this.leave(defn);
		return null;
	}

	public boolean enter(final DefMemberType defn) {
		return enter((ASTNode) defn);
	}

	public void leave(final DefMemberType defn) {
	}

	public Void visit(final DatatypeDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		//		for (Constructor member : decl.members) member.accept(this);
		this.leave(decl);
		return null;
	}

	public boolean enter(final DatatypeDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final DatatypeDeclaration decl) {
	}

	public Void visit(final TypeAliasDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		//		decl.t.accept(this);
		this.leave(decl);
		return null;
	}

	public boolean enter(final TypeAliasDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final TypeAliasDeclaration decl) {
	}

	public Void visit(final TypeDeclaration decl) {
		if (!this.enter(decl)) {
			return null;
		}
		this.leave(decl);
		return null;
	}

	public boolean enter(final TypeDeclaration decl) {
		return enter((ASTNode) decl);
	}

	public void leave(final TypeDeclaration decl) {
	}

	public Void visit(final AsPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		patn.p.accept(this);
		this.leave(patn);
		return null;
	}

	public boolean enter(final AsPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final AsPattern patn) {
	}

	public Void visit(final CallPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		patn.p.accept(this);
		this.leave(patn);
		return null;
	}

	public boolean enter(final CallPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final CallPattern patn) {
	}

	public Void visit(final ConsPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		patn.h.accept(this);
		patn.t.accept(this);
		this.leave(patn);
		return null;
	}

	public boolean enter(final ConsPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final ConsPattern patn) {
	}

	public Void visit(final EqPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final EqPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final EqPattern patn) {
	}

	public Void visit(final ListPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		for (final Pattern p : patn.ps) {
			p.accept(this);
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final ListPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final ListPattern patn) {
	}

	public Void visit(final LiteralPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		patn.lit.accept(this);
		this.leave(patn);
		return null;
	}

	public boolean enter(final LiteralPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final LiteralPattern patn) {
	}

	public Void visit(final NilPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final NilPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final NilPattern patn) {
	}

	public Void visit(final TuplePattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		for (final Pattern arg : patn.args) {
			arg.accept(this);
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final TuplePattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final TuplePattern patn) {
	}

	public Void visit(final TypedPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		patn.p.accept(this);
		//		patn.t.accept(this);
		this.leave(patn);
		return null;
	}

	public boolean enter(final TypedPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final TypedPattern patn) {
	}

	public Void visit(final VariablePattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final VariablePattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final VariablePattern patn) {
	}

	public Void visit(final WildcardPattern patn) {
		if (!this.enter(patn)) {
			return null;
		}
		this.leave(patn);
		return null;
	}

	public boolean enter(final WildcardPattern patn) {
		return enter((ASTNode) patn);
	}

	public void leave(final WildcardPattern patn) {
	}

}
