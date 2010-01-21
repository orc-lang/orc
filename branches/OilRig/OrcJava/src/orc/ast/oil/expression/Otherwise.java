package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.TokenContinuation;
import orc.ast.oil.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;
import orc.type.Type;
import orc.type.TypingContext;

public class Otherwise extends Expression {

	public Expression left;
	public Expression right;
	
	public Otherwise(Expression left, Expression right)
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
		return "(" + left.toString() + " ; " + right.toString() + ")";
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
		return new orc.ast.xml.expression.Otherwise(left.marshal(), right.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		TokenContinuation leftK = new TokenContinuation() {
			public void execute(Token t) {
				// This cast cannot fail; a Leave node always matches a Semi node earlier in the dag.
				SemiRegion region = (SemiRegion)t.getRegion();
				
				// If a publication successfully leaves a SemiRegion, the right hand side of the semicolon shouldn't execute.
				// This step cancels the RHS.
				// It is an idempotent operation.
				region.cancel();
				
				leave(t.setRegion(region.getParent()));
			}
		};
		left.setPublishContinuation(leftK);
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
		forked.setQuiescent();
		SemiRegion region = new SemiRegion(t.getRegion(), forked.move(right));
		left.enter(t.move(left).setRegion(region));
	}
}
