package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * The "isolated" keyword.
 * @see orc.runtime.nodes.Isolate
 * @author quark
 */
public class Isolated extends Expression {

	public Expression body;
	
	public Isolated(Expression body) {
		this.body = body;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(isolated " + body + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}
	
	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		return body.typesynth(ctx);
	}

	@Override
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		body.typecheck(ctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Isolated(body.marshal());
	}
}
