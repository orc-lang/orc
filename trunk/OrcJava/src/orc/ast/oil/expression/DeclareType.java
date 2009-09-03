package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

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
	public orc.type.Type typesynth(Env<orc.type.Type> ctx, Env<orc.type.Type> typectx) throws TypeException {
		orc.type.Type actualType = type.transform().subst(typectx);
		return body.typesynth(ctx, typectx.extend(actualType));
	}

	public void typecheck(orc.type.Type T, Env<orc.type.Type> ctx, Env<orc.type.Type> typectx) throws TypeException {
		orc.type.Type actualType = type.transform().subst(typectx);
		body.typecheck(T, ctx, typectx.extend(actualType));
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.DeclareType(type.marshal(), body.marshal());
	}
}
