package orc.ast.oil.expression;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Arg;
import orc.ast.oil.expression.argument.Var;
import orc.ast.oil.xml.Expression;
import orc.ast.simple.argument.Argument;

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
		
		/* TODO: thrown type = join of thrown type and this synthesized type */ 
		exception.typesynth(ctx, typectx);
		
		// throw e : Bot, so long as e is typable.
		return Type.BOT;
	}
	
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Throw(exception.marshal());
	}

}
