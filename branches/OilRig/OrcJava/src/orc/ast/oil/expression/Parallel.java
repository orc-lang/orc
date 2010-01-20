package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.TokenContinuation;
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;
import orc.type.Type;
import orc.type.TypingContext;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;
	
	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(" + left.toString() + " | " + right.toString() + ")";
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
		
		Type L = left.typesynth(ctx);
		Type R = right.typesynth(ctx);
		return L.join(R);
	}

	@Override
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		
		left.typecheck(ctx, T);
		right.typecheck(ctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Parallel(left.marshal(), right.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		left.setPublishContinuation(getPublishContinuation());
		right.setPublishContinuation(getPublishContinuation());
		left.populateContinuations();
		right.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(Token t) {
		Token forked;
		try {
			forked = t.fork();
		} catch (TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		left.enter(t.move(left));
		right.enter(forked.move(right));
	}
}
