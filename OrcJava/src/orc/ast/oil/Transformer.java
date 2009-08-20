package orc.ast.oil;

import java.util.LinkedList;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.Defs;
import orc.ast.oil.expression.Expr;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Isolated;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.TypeDecl;
import orc.ast.oil.expression.argument.Arg;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Var;
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

	public Expr visit(Parallel expr) {
		return new Parallel(expr.left.accept(this), expr.right.accept(this));
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

	public Expr visit(Pruning expr) {
		return new Pruning(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expr visit(Sequential expr) {
		return new Sequential(expr.left.accept(this), expr.right.accept(this), expr.name);
	}

	public Expr visit(Otherwise expr) {
		return new Otherwise(expr.left.accept(this), expr.right.accept(this));
	}

	public Expr visit(Stop expr) {
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
	
	public Expr visit(Throw throwExpr){
		return new Throw(throwExpr.exception.accept(this));
	}
	
	public Expr visit(Catch catchExpr){
		return new Catch(this.visit(catchExpr.handler), catchExpr.tryBlock.accept(this));
	}
}
