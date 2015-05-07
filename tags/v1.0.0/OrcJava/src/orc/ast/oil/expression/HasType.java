package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.oil.type.Type;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.TypingContext;

/**
 * 
 * An expression with an ascribed type.
 * 
 * @author dkitchin
 *
 */
public class HasType extends Expression {

	public Expression body;
	public orc.ast.oil.type.Type type;
	public boolean checkable; // set to false if this is a type assertion, not a type ascription
	
	public HasType(Expression body, orc.ast.oil.type.Type type, boolean checkable) {
		this.body = body;
		this.type = type;
		this.checkable = checkable;
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
		
		/* If this ascription can be checked, check it */ 
		if (checkable) {
			body.typecheck(ctx, actualType);
		}
		/* If not, it is a type assertion, so we do not check it. */
		else {}
	
		return actualType;
	}

	
	
	
	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.HasType(body.marshal(),
				type.marshal(), checkable);
	}
}