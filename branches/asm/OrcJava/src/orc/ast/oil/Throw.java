package orc.ast.oil;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Var;
import orc.ast.oil.xml.Expression;
import orc.ast.simple.arg.Argument;

public class Throw extends Expr {
	
	public Expr exception;
	
	public Throw(Expr e){
		exception = e;
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		exception.addIndices(indices, depth);
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		return exception.typesynth(ctx, typectx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(exception.typesynth(ctx, typectx));
		exception.typecheck(T, rctx, typectx);
	}	
	
	public Expression marshal() throws CompilationException {
		return null; //new orc.ast.oil.xml.Throw(exception.marshall());
	}

}
