package orc.ast.oil.expression;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Stop extends Expression {

	public String toString() {
		return "stop";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) {
		return Type.BOT;
	}
	
	@Override
	public void typecheck(Type t, Env<Type> ctx, Env<Type> typectx) {
		// Do nothing. Silent checks against all types.
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Stop();
	}
}
