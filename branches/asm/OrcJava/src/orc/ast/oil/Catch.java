package orc.ast.oil;

import java.util.Set;

import orc.ast.oil.xml.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

public class Catch extends Expr {
	
	public Def handler;
	public Expr tryBlock;
	
	public Catch(Def handler, Expr tryBlock){
		this.handler = handler;
		this.tryBlock = tryBlock;
	}
	
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		return tryBlock.typesynth(ctx, typectx);
	}
	
	public void addIndices(Set<Integer> indices, int depth) {
		handler.addIndices(indices, depth);
		tryBlock.addIndices(indices, depth);
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Catch(handler.marshal(), tryBlock.marshal());
	}

}
