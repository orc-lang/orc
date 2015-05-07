package orc.ast.oil;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Isolated;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;

/**
 * Abstract base class tree walker for Oil expressions.
 * 
 * @author quark
 */
public abstract class Walker implements Visitor<Void> {
	public void enterScope(int n) {}
	public void leaveScope(int n) {}
	
	public Void visit(Atomic expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Isolated expr) {};	
	public void leave(Isolated expr) {};
	
	public Void visit(Isolated expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Atomic expr) {};	
	public void leave(Atomic expr) {};
	
	public Void visit(Parallel expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Parallel expr) {};	
	public void leave(Parallel expr) {};

	public Void visit(Call expr) {
		this.enter(expr);
		expr.callee.accept(this);
		for (Argument a : expr.args) a.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Call expr) {};	
	public void leave(Call expr) {};

	public Void visit(DeclareDefs expr) {
		this.enter(expr);
		this.enterScope(expr.defs.size());
		for (Def def : expr.defs) {
			this.enter(def);
			this.enterScope(def.arity);
			def.body.accept(this);
			this.leaveScope(def.arity);
			this.leave(def);
		}
		expr.body.accept(this);
		this.leaveScope(expr.defs.size());
		this.leave(expr);
		return null;
	}
	public void enter(Def def) {};	
	public void leave(Def def) {};
	public void enter(DeclareDefs expr) {};	
	public void leave(DeclareDefs expr) {};

	public Void visit(Stop arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Stop arg) {};
	public void leave(Stop arg) {};

	public Void visit(Pruning expr) {
		this.enter(expr);
		this.enterScope(1);
		expr.left.accept(this);
		this.leaveScope(1);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Pruning expr) {};
	public void leave(Pruning expr) {};

	public Void visit(Sequential expr) {
		this.enter(expr);
		expr.left.accept(this);
		this.enterScope(1);
		expr.right.accept(this);
		this.leaveScope(1);
		this.leave(expr);
		return null;
	}
	public void enter(Sequential expr) {};	
	public void leave(Sequential expr) {};

	public Void visit(Otherwise expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Otherwise expr) {};
	public void leave(Otherwise expr) {};
	
	public Void visit(WithLocation expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(WithLocation expr) {};
	public void leave(WithLocation expr) {};

	public Void visit(Constant arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Constant arg) {};
	public void leave(Constant arg) {};

	public Void visit(Field arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Field arg) {};
	public void leave(Field arg) {};

	public Void visit(Site arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Site arg) {};
	public void leave(Site arg) {};
	
	public Void visit(Variable arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Variable arg) {};
	public void leave(Variable arg) {};
	
	
	public Void visit(HasType expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(HasType expr) {};	
	public void leave(HasType expr) {};

	public Void visit(DeclareType expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(DeclareType expr) {};	
	public void leave(DeclareType expr) {};
	
	//TODO:
	public Void visit(Catch catchExpr){
		return null;
	}
	
	public Void visit(Throw throwExpr){
		return null;
	}

}
