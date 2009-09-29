/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
package orc.ast.extended;

import java.util.List;

import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.declaration.IncludeDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.def.DefMember;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.declaration.def.DefMemberType;
import orc.ast.extended.declaration.type.Constructor;
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
import orc.ast.extended.expression.Expression;
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
import orc.ast.oil.expression.argument.Argument;
import orc.ast.simple.argument.FreeVariable;

/**
 * 
 *
 * @author jthywiss
 */
public class Walker implements Visitor<Void> {
	//FIXME; Walk into type AST nodes (commented below)

	public void enterScope(ASTNode node) {
	}

	public void leaveScope(ASTNode node) {
	}


	public boolean enter(ASTNode node) {
		return true;
	}


	public Void visit(AssertType expr) {
		if (!this.enter(expr)) return null;
		expr.body.accept(this);
//		expr.type.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(AssertType expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(AssertType expr) {
	}


	public Void visit(Atomic expr) {
		if (!this.enter(expr)) return null;
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Atomic expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Atomic expr) {
	}


	public Void visit(Call expr) {
		if (!this.enter(expr)) return null;
		expr.caller.accept(this);
		for (Expression arg : expr.args) arg.accept(this);
//		for (Type typeArg : expr.typeArgs) typeArg.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Call expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Call expr) {
	}


	public Void visit(Catch expr) {
		if (!this.enter(expr)) return null;
		expr.tryBlock.accept(this);
		for (CatchHandler handler : expr.handlers) handler.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Catch expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Catch expr) {
	}


	public Void visit(ConsExpr expr) {
		if (!this.enter(expr)) return null;
		expr.h.accept(this);
		expr.t.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(ConsExpr expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(ConsExpr expr) {
	}


	public Void visit(Declare expr) {
		if (!this.enter(expr)) return null;
		this.enterScope(expr);
		expr.d.accept(this);
		expr.e.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Declare expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Declare expr) {
	}


	public Void visit(Dot expr) {
		if (!this.enter(expr)) return null;
		expr.target.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Dot expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Dot expr) {
	}


	public Void visit(Field expr) {
		if (!this.enter(expr)) return null;
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Field expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Field expr) {
	}


	public Void visit(HasType expr) {
		if (!this.enter(expr)) return null;
		expr.body.accept(this);
//		expr.type.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(HasType expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(HasType expr) {
	}


	public Void visit(IfThenElse expr) {
		if (!this.enter(expr)) return null;
		expr.condition.accept(this);
		expr.consequent.accept(this);
		expr.alternative.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(IfThenElse expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(IfThenElse expr) {
	}


	public Void visit(Isolated expr) {
		if (!this.enter(expr)) return null;
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Isolated expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Isolated expr) {
	}


	public Void visit(Lambda expr) {
		if (!this.enter(expr)) return null;
		this.enterScope(expr);
		for (List<Pattern> ps : expr.formals)
			for (Pattern formal : ps) 
				formal.accept(this);
//		expr.resultType.accept(this);
		expr.body.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Lambda expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Lambda expr) {
	}


	public Void visit(Let expr) {
		if (!this.enter(expr)) return null;
		for (Expression arg : expr.args) arg.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Let expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Let expr) {
	}


	public Void visit(ListExpr expr) {
		if (!this.enter(expr)) return null;
		for (Expression arg : expr.es) arg.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(ListExpr expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(ListExpr expr) {
	}


	public Void visit(Literal expr) {
		if (!this.enter(expr)) return null;
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Literal expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Literal expr) {
	}


	public Void visit(Name expr) {
		if (!this.enter(expr)) return null;
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Name expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Name expr) {
	}


	public Void visit(NilExpr expr) {
		if (!this.enter(expr)) return null;
		this.leave(expr);
		return null;
	}
	
	public boolean enter(NilExpr expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(NilExpr expr) {
	}


	public Void visit(Parallel expr) {
		if (!this.enter(expr)) return null;
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Parallel expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Parallel expr) {
	}


	public Void visit(Otherwise expr) {
		if (!this.enter(expr)) return null;
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Otherwise expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Otherwise expr) {
	}


	public Void visit(Sequential expr) {
		if (!this.enter(expr)) return null;
		expr.left.accept(this);
		this.enterScope(expr);
		expr.p.accept(this);
		expr.right.accept(this);
		this.leaveScope(expr);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Sequential expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Sequential expr) {
	}


	public Void visit(Stop expr) {
		if (!this.enter(expr)) return null;
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Stop expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Stop expr) {
	}


	public Void visit(Throw expr) {
		if (!this.enter(expr)) return null;
		expr.exception.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Throw expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Throw expr) {
	}


	public Void visit(Pruning expr) {
		if (!this.enter(expr)) return null;
		//TODO: Desired order of traversal? (l-p-r or r-p-l?)
		this.enterScope(expr);
		expr.left.accept(this);
		expr.p.accept(this);
		this.leaveScope(expr);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	
	public boolean enter(Pruning expr) {
		return enter((ASTNode)expr);
	}
	
	public void leave(Pruning expr) {
	}


	//TODO: Type's subtypes


	public Void visit(CatchHandler handler) {
		if (!this.enter(handler)) return null;
		this.enterScope(handler);
		for (Pattern patn : handler.catchPattern) patn.accept(this);
		handler.body.accept(this);
		this.leaveScope(handler);
		this.leave(handler);
		return null;
	}
	
	public boolean enter(CatchHandler handler) {
		return enter((ASTNode)handler);
	}
	
	public void leave(CatchHandler handler) {
	}


	public Void visit(IncludeDeclaration decl) {
		if (!this.enter(decl)) return null;
		for (Declaration aDecl : decl.decls)
			aDecl.accept(this);
		this.leave(decl);
		return null;
	}
	
	public boolean enter(IncludeDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(IncludeDeclaration decl) {
	}


	public Void visit(ClassDeclaration decl) {
		if (!this.enter(decl)) return null;
		this.leave(decl);
		return null;
	}
	
	public boolean enter(ClassDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(ClassDeclaration decl) {
	}


	public Void visit(DefsDeclaration decl) {
		if (!this.enter(decl)) return null;
		for (DefMember def : decl.defs) def.accept(this);
		this.leave(decl);
		return null;
	}
	
	public boolean enter(DefsDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(DefsDeclaration decl) {
	}


	public Void visit(SiteDeclaration decl) {
		if (!this.enter(decl)) return null;
		this.leave(decl);
		return null;
	}
	
	public boolean enter(SiteDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(SiteDeclaration decl) {
	}


	public Void visit(ValDeclaration decl) {
		if (!this.enter(decl)) return null;
		decl.p.accept(this);
		decl.f.accept(this);
		this.leave(decl);
		return null;
	}
	
	public boolean enter(ValDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(ValDeclaration decl) {
	}


	public Void visit(DefMemberClause defn) {
		if (!this.enter(defn)) return null;
		this.enterScope(defn);
		for (List<Pattern> ps : defn.formals)
			for (Pattern formal : ps) 
				formal.accept(this);
//		defn.resultType.accept(this);
		defn.body.accept(this);
		this.leaveScope(defn);
		this.leave(defn);
		return null;
	}
	
	public boolean enter(DefMemberClause defn) {
		return enter((ASTNode)defn);
	}
	
	public void leave(DefMemberClause defn) {
	}


	public Void visit(DefMemberType defn) {
		if (!this.enter(defn)) return null;
		this.enterScope(defn);
//		for (Type arg : defn.argTypes) arg.accept(this);
//		defn.resultType.accept(this);
		this.leaveScope(defn);
		this.leave(defn);
		return null;
	}
	
	public boolean enter(DefMemberType defn) {
		return enter((ASTNode)defn);
	}
	
	public void leave(DefMemberType defn) {
	}


	public Void visit(DatatypeDeclaration decl) {
		if (!this.enter(decl)) return null;
//		for (Constructor member : decl.members) member.accept(this);
		this.leave(decl);
		return null;
	}
	
	public boolean enter(DatatypeDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(DatatypeDeclaration decl) {
	}


	public Void visit(TypeAliasDeclaration decl) {
		if (!this.enter(decl)) return null;
//		decl.t.accept(this);
		this.leave(decl);
		return null;
	}
	
	public boolean enter(TypeAliasDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(TypeAliasDeclaration decl) {
	}


	public Void visit(TypeDeclaration decl) {
		if (!this.enter(decl)) return null;
		this.leave(decl);
		return null;
	}
	
	public boolean enter(TypeDeclaration decl) {
		return enter((ASTNode)decl);
	}
	
	public void leave(TypeDeclaration decl) {
	}


	public Void visit(AsPattern patn) {
		if (!this.enter(patn)) return null;
		patn.p.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(AsPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(AsPattern patn) {
	}


	public Void visit(CallPattern patn) {
		if (!this.enter(patn)) return null;
		patn.p.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(CallPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(CallPattern patn) {
	}


	public Void visit(ConsPattern patn) {
		if (!this.enter(patn)) return null;
		patn.h.accept(this);
		patn.t.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(ConsPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(ConsPattern patn) {
	}


	public Void visit(EqPattern patn) {
		if (!this.enter(patn)) return null;
		this.leave(patn);
		return null;
	}
	
	public boolean enter(EqPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(EqPattern patn) {
	}


	public Void visit(ListPattern patn) {
		if (!this.enter(patn)) return null;
		for (Pattern p : patn.ps) p.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(ListPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(ListPattern patn) {
	}


	public Void visit(LiteralPattern patn) {
		if (!this.enter(patn)) return null;
		patn.lit.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(LiteralPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(LiteralPattern patn) {
	}


	public Void visit(NilPattern patn) {
		if (!this.enter(patn)) return null;
		this.leave(patn);
		return null;
	}
	
	public boolean enter(NilPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(NilPattern patn) {
	}


	public Void visit(TuplePattern patn) {
		if (!this.enter(patn)) return null;
		for (Pattern arg : patn.args) arg.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(TuplePattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(TuplePattern patn) {
	}


	public Void visit(TypedPattern patn) {
		if (!this.enter(patn)) return null;
		patn.p.accept(this);
//		patn.t.accept(this);
		this.leave(patn);
		return null;
	}
	
	public boolean enter(TypedPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(TypedPattern patn) {
	}


	public Void visit(VariablePattern patn) {
		if (!this.enter(patn)) return null;
		this.leave(patn);
		return null;
	}
	
	public boolean enter(VariablePattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(VariablePattern patn) {
	}


	public Void visit(WildcardPattern patn) {
		if (!this.enter(patn)) return null;
		this.leave(patn);
		return null;
	}
	
	public boolean enter(WildcardPattern patn) {
		return enter((ASTNode)patn);
	}
	
	public void leave(WildcardPattern patn) {
	}

}
