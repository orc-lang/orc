package orc.ast.oil;

import java.util.LinkedList;

import orc.ast.oil.arg.Constant;
import orc.ast.oil.arg.Field;
import orc.ast.oil.arg.Site;
import orc.ast.oil.arg.Var;
import orc.ast.oil.arg.Arg;
import orc.type.Type;

/**
 * Abstract base class tree transformer for Oil expressions.
 * 
 * @author quark
 */
public abstract class Transformer implements Visitor<Expr> {
	public Expr visit(Atomic expr) {
		return new Atomic(expr.body.accept(this));
	}

	public Expr visit(Bar expr) {
		return new Bar(expr.left.accept(this), expr.right.accept(this));
	}

	public Expr visit(Call expr) {
		LinkedList<Arg> newArgs = new LinkedList<Arg>();
		for (Arg a : expr.args) {
			newArgs.add((Arg)a.accept(this));
		}
		LinkedList<Type> newTypeArgs = null;
		if (expr.typeArgs != null) {
			newTypeArgs = new LinkedList<Type>();
			for (Type t : expr.typeArgs) {
				newTypeArgs.add(visit(t));
			}
		}
		return new Call((Arg)expr.callee.accept(this), newArgs, newTypeArgs);
	}
	
	public Type visit(Type type) {
		return type;
	}

	public Expr visit(Constant arg) {
		return arg;
	}

	public Expr visit(Defs expr) {
		LinkedList<Def> newDefs = new LinkedList<Def>();
		for (Def d : expr.defs) {
			newDefs.add(visit(d));
		}
		return new Defs(newDefs, expr.body.accept(this));
	}
	
	public Def visit(Def d) {
		LinkedList<Type> newArgTypes = null;
		if (d.argTypes != null) {
			newArgTypes = new LinkedList<Type>();
			for (Type t : d.argTypes) {
				newArgTypes.add(visit(t));
			}
		}
		Type newResultType = null;
		if (d.resultType != null) {
			newResultType = visit(d.resultType);
		}
		return new Def(d.arity, d.body.accept(this), d.typeArity, newArgTypes, newResultType, d.location, d.name);
	}

	public Expr visit(Field arg) {
		return arg;
	}

	public Expr visit(HasType hasType) {
		return new HasType(hasType.body.accept(this), visit(hasType.type), hasType.checkable);
	}

	public Expr visit(Isolated expr) {
		return new Isolated(expr.body.accept(this));
	}

	public Expr visit(Pull expr) {
		return new Pull(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expr visit(Push expr) {
		return new Push(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expr visit(Semi expr) {
		return new Semi(expr.left.accept(this), expr.right.accept(this));
	}

	public Expr visit(Silent expr) {
		return expr;
	}

	public Expr visit(Site arg) {
		return arg;
	}

	public Expr visit(TypeDecl typeDecl) {
		return new TypeDecl(visit(typeDecl.type), typeDecl.body.accept(this));
	}

	public Expr visit(Var arg) {
		return arg;
	}

	public Expr visit(WithLocation expr) {
		return new WithLocation(expr.body.accept(this), expr.location);
	}
}
