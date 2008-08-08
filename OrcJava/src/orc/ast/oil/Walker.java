package orc.ast.oil;

import orc.ast.oil.arg.Constant;
import orc.ast.oil.arg.Field;
import orc.ast.oil.arg.Site;
import orc.ast.oil.arg.Var;
import orc.ast.oil.arg.Arg;

/**
 * Abstract base class tree walker for Oil expressions.
 * 
 * @author quark
 */
public abstract class Walker implements Visitor<Void> {
	
	public Void visit(Bar expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Bar expr) {};	
	public void leave(Bar expr) {};

	public Void visit(Call expr) {
		this.enter(expr);
		expr.callee.accept(this);
		for (Arg a : expr.args) a.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Call expr) {};	
	public void leave(Call expr) {};

	public Void visit(Defs expr) {
		this.enter(expr);
		expr.body.accept(this);
		for (Def def : expr.defs) {
			this.enter(def);
			def.body.accept(this);
			this.leave(def);
		}
		this.leave(expr);
		return null;
	}
	public void enter(Def def) {};	
	public void leave(Def def) {};
	public void enter(Defs expr) {};	
	public void leave(Defs expr) {};

	public Void visit(Silent arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Silent arg) {};
	public void leave(Silent arg) {};

	public Void visit(Pull expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Pull expr) {};
	public void leave(Pull expr) {};

	public Void visit(Push expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Push expr) {};	
	public void leave(Push expr) {};

	public Void visit(Semi expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}
	public void enter(Semi expr) {};
	public void leave(Semi expr) {};

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
	
	public Void visit(Var arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}
	public void enter(Var arg) {};
	public void leave(Var arg) {};
}
