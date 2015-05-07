package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * 
 * Bind a type in the given scope.
 * 
 * @author dkitchin
 *
 */
public class DeclareType extends Expression {

	public orc.ast.oil.type.Type type;
	public Expression body;
	
	public DeclareType(orc.ast.oil.type.Type type, Expression body) {
		this.type = type;
		this.body = body;
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public orc.type.Type typesynth(TypingContext ctx) throws TypeException {
		orc.type.Type actualType = ctx.promote(type);
		TypingContext newctx = ctx.bindType(actualType);
		return body.typesynth(newctx);
	}

	public void typecheck(TypingContext ctx, orc.type.Type T) throws TypeException {
		orc.type.Type actualType = ctx.promote(type);
		TypingContext newctx = ctx.bindType(actualType);
		body.typecheck(newctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.DeclareType(type.marshal(), body.marshal());
	}
}
